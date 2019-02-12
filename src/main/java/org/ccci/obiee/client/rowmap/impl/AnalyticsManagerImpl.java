package org.ccci.obiee.client.rowmap.impl;

import com.google.common.collect.Lists;
import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import oracle.bi.web.soap.QueryResults;
import oracle.bi.web.soap.ReportEditingServiceSoap;
import oracle.bi.web.soap.ReportParams;
import oracle.bi.web.soap.ReportRef;
import oracle.bi.web.soap.SAWSessionServiceSoap;
import oracle.bi.web.soap.Variable;
import oracle.bi.web.soap.XMLQueryExecutionOptions;
import oracle.bi.web.soap.XMLQueryOutputFormat;
import oracle.bi.web.soap.XmlViewServiceSoap;
import org.ccci.obiee.client.rowmap.AnalyticsManager;
import org.ccci.obiee.client.rowmap.DataRetrievalException;
import org.ccci.obiee.client.rowmap.Query;
import org.ccci.obiee.client.rowmap.ReportColumn;
import org.ccci.obiee.client.rowmap.ReportDefinition;
import org.ccci.obiee.client.rowmap.RowmapConfigurationException;
import org.ccci.obiee.client.rowmap.SortDirection;
import org.ccci.obiee.client.rowmap.annotation.ReportParamVariable;
import org.ccci.obiee.client.rowmap.annotation.ReportPath;
import org.ccci.obiee.client.rowmap.util.Doms;
import org.ccci.obiee.client.rowmap.util.SoapFaults;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static org.ccci.obiee.client.rowmap.impl.JodaTimeAvailability.isJodaAvailable;
import static org.ccci.obiee.client.rowmap.impl.Tracing.buildTopLevelSpan;

/**
 * 
 * @author Matt Drees
 * @author William Randall
 *
 */
public class AnalyticsManagerImpl implements AnalyticsManager
{

    private static final String VALIDATION_REPORT_PATH = "/shared/CCCi/SSW/Rowmap Session Validation Query";
    private static final String SAW_URI = "com.siebel.analytics.web/report/v1.1";
    /**
     * The Answers web service doesn't require you to send the sessionId on every request as long
     * as your web service client maintains cookies.  The jax-ws client can be configured to do this
     * (see http://weblogs.java.net/blog/ramapulavarthi/archive/2006/06/maintaining_ses.html),
     * however, this does not work for us.
     * I believe in this case the cookies are not shared between different service implementations. 
     * That is, cookies received by the sawSessionService aren't propagated to the xmlViewService,
     * for example.
     * So, we have to maintain the sessionId instead of using cookies.
     */
    private final String sessionId;
    private final SAWSessionServiceSoap sawSessionService;
    private final XmlViewServiceSoap xmlViewService;
    private final XPathFactory xpathFactory;

    private XPathExpression xsdElementExpression;
    private XPathExpression rowExpression;
    private XPathExpression columnOrderExpression;
    private XPathExpression criteriaExpression;

    private final DocumentBuilder builder;
    private final ConverterStore converterStore;
    private final Tracer tracer;

    private final ReportEditingServiceSoap reportEditingService;
    private boolean closed = false;

    private Exception recentException = null;
    private Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Assumes that the caller has logged us in to OBIEE already.  
     * @param sessionId used to maintain session for various calls
     * @param sawSessionService used for logout
     * @param xmlViewService used for retrieving report queries
     * @param converterStore used to convert field values
     * @param tracer used for APM
     */
    public AnalyticsManagerImpl(
        String sessionId,
        SAWSessionServiceSoap sawSessionService,
        XmlViewServiceSoap xmlViewService,
        ReportEditingServiceSoap reportEditingService,
        ConverterStore converterStore,
        Tracer tracer)
    {

        this.sessionId = sessionId;
        this.sawSessionService = sawSessionService;
        this.xmlViewService = xmlViewService;
        this.reportEditingService = reportEditingService;
        this.converterStore = converterStore;
        this.tracer = tracer;

        xpathFactory = XPathFactory.newInstance();
        buildXpathExpressions();

        DocumentBuilderFactory factory;
        factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        try
        {
            builder = factory.newDocumentBuilder();
        }
        catch (ParserConfigurationException e)
        {
            throw new RowmapConfigurationException("unable to build document builder", e);
        }
    }

    private void buildXpathExpressions()
    {
        XPath xpath = xpathFactory.newXPath();
        xpath.setNamespaceContext(new RowsetNamespaceContext());
        try
        {
            xsdElementExpression = xpath.compile("/rowset:rowset/xsd:schema/xsd:complexType[@name='Row']/xsd:sequence/xsd:element");
            rowExpression = xpath.compile("/rowset:rowset/rowset:Row");
            columnOrderExpression = xpath.compile("/saw:report/saw:criteria/saw:columnOrder");
            criteriaExpression = xpath.compile("/saw:report/saw:criteria");
        }
        catch (XPathExpressionException e)
        {
            throw new RuntimeException("bad xpath", e);
        }
    }

    public void close()
    {
        checkOpen();
        closed = true;
        final Span span = buildTopLevelSpan(tracer, "logoff");
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            log.debug("logging off session " + sessionId);
            sawSessionService.logoff(sessionId);
            log.debug("logoff successful");
        }
        finally
        {
            span.finish();
        }
    }

    private void checkOpen()
    {
        if (closed) throw new IllegalStateException("already closed");
    }
    
    public <T> QueryImpl<T> createQuery(ReportDefinition<T> reportDefinition)
    {
        checkOpen();
        if (reportDefinition == null)
            throw new NullPointerException("reportDefinition is null");
        return new QueryImpl<>(reportDefinition);
    }
    
    class QueryImpl<T> implements Query<T>
    {

        private final ReportDefinition<T> reportDefinition;
        private Object selection;
        private ReportColumn<T> sortColumn;
        private SortDirection direction;
        private int maxResults = -1;

        public QueryImpl(ReportDefinition<T> reportDefinition)
        {
            this.reportDefinition = reportDefinition;
        }

        public Query<T> withSelection(Object selection)
        {
            if (selection == null) 
                throw new NullPointerException("selection is null");
            if(!annotatedFieldsExist(selection))
                throw new RowmapConfigurationException("You forgot to annotate the filter variables");
            this.selection = selection;
            return this;
        }
        
        public Query<T> orderBy(ReportColumn<T> sortColumn, SortDirection direction) 
        {
            if (sortColumn == null)
                throw new NullPointerException("sortColumn cannot be null.");
            if (!reportDefinition.getColumns().contains(sortColumn))
            {
                throw new IllegalArgumentException(String.format(
                    "Sort column %s does not appear to be a column of report %s",
                    sortColumn,
                    reportDefinition.getName()
                ));
            }
            this.direction = direction;
            this.sortColumn = sortColumn;

            return this;
        }

        @Override
        public Query<T> setMaxResults(int maxResults)
        {
            this.maxResults = maxResults;
            return this;
        }

        public List<T> getResultList()
        {
            final Span span = buildTopLevelSpan(tracer, "get-result-list");
            try (Scope ignored = tracer.scopeManager().activate(span, false))
            {
                checkOpen();
                Class<T> rowType = reportDefinition.getRowType();
                ReportPath reportPathConfiguration = rowType.getAnnotation(ReportPath.class);
                overrideResourceName(span, reportPathConfiguration.value());

                Document dataDocument;
                Document metadataDocument;
                ReportParams params = buildReportParams(selection);
                if (sortColumn != null)
                {
                    if (direction == null)
                    {
                        direction = SortDirection.ASCENDING;
                    }

                    String metadata = queryForMetadata(reportPathConfiguration);
                    metadataDocument = buildRowsetDocument(metadata);

                    String rowset = buildXmlReportAndQuery(
                        reportPathConfiguration,
                        params,
                        metadataDocument,
                        maxResults
                    );
                    dataDocument = buildRowsetDocument(rowset);
                }
                else
                {
                    String rowset = queryForMetadataAndData(reportPathConfiguration, params, maxResults);
                    dataDocument = buildRowsetDocument(rowset);
                    metadataDocument = dataDocument;
                }

                if (isEmptyRowset(dataDocument))
                {
                    return Lists.newArrayList();
                }
                RowBuilder<T> rowBuilder = buildRowBuilder(metadataDocument);
                NodeList rows = getRows(dataDocument);
                return buildResults(rowBuilder, rows);
            }
            finally
            {
                span.finish();
            }
        }

        private String buildXmlReportAndQuery(
            ReportPath reportPathConfiguration,
            ReportParams params,
            Document metadataDoc,
            int maxResults) {

            if (isEmptyRowset(metadataDoc)) {
                throw new RowmapConfigurationException(
                    String.format(
                        "the report '%s', as stored in Answers, appears to return zero results. " +
                        "Please adjust the default parameters in Answers so that it returns at least one.",
                        reportPathConfiguration.value()
                    )
                );
            }

            String sortColumnId = findSortColumnId(sortColumn, metadataDoc);

            String xmlReportWithAppropriateOrdering = createXmlReportWithAppropriateOrdering(
                reportPathConfiguration,
                params,
                sortColumnId,
                direction);

            return queryForData(xmlReportWithAppropriateOrdering, params, maxResults);
        }

        private boolean isEmptyRowset(Document rowsetDocument) {
            Node rowsetNode = rowsetDocument.getDocumentElement();
            return rowsetNode.getChildNodes().getLength() == 0;
        }

        private List<T> buildResults(RowBuilder<T> rowBuilder, NodeList rows)
        {
            final Span span = tracer.buildSpan("build-results").start();
            try (Scope ignored = tracer.scopeManager().activate(span, false))
            {
                List<T> results = new ArrayList<>();
                for (Node row : Doms.each(rows))
                {
                    T rowInstance = rowBuilder.buildRowInstance(row);
                    results.add(rowInstance);
                }
                return results;
            }
            finally
            {
                span.finish();
            }

        }

        RowBuilder<T> buildRowBuilder(Document doc)
        {
            NodeList columnDefinitionXsdElements = getColumnSchemaNodesFromPreamble(doc);
            
            Map<ReportColumnId, String> elementNamesPerColumnId = new HashMap<>();
            
            for (Node node : Doms.each(columnDefinitionXsdElements) )
            {
                String elementName = node.getAttributes().getNamedItem("name").getNodeValue();
                String tableHeading = node.getAttributes().getNamedItem("saw-sql:tableHeading").getNodeValue();
                String columnHeading = node.getAttributes().getNamedItem("saw-sql:columnHeading").getNodeValue();
                
                elementNamesPerColumnId.put(new ReportColumnId(tableHeading, columnHeading), elementName);
            }
            if (elementNamesPerColumnId.isEmpty())
                throw new DataRetrievalException("No schema was returned in rowset");
            ConverterStore converters = reportDefinition.getConverterStore();
            ConverterStore reportConverterStore = converterStore.copyAndAdd(converters);
            
            return new RowBuilder<>(elementNamesPerColumnId, reportDefinition.getRowType(), reportConverterStore);
        }
        
        
        

        public T getSingleResult()
        {
            List<T> resultList = getResultList();
            if (resultList.size() == 0)
                //TODO: this message could be nicer, including the selection criteria
                throw new DataRetrievalException("No rows were returned");
            if (resultList.size() > 1)
                throw new DataRetrievalException("More than one row was returned");
            return resultList.get(0);
        }
        
        private boolean annotatedFieldsExist(Object selection)
        {
            Class<?> clazz = selection.getClass();
            
            for(Field field: clazz.getDeclaredFields())
            {
                if(field.getAnnotation(ReportParamVariable.class) != null)
                {
                    return true;
                }
            }
            return false;
        }
    }
    
    private <T> String findSortColumnId(ReportColumn<T> sortColumn, Document metadataDoc)
    {
        NodeList columnDefinitionXsdElements = getColumnSchemaNodesFromPreamble(metadataDoc);
        
        ReportColumnId sortColumnId = ReportColumnId.buildColumnId(sortColumn.getField());
        
        for (Node node : Doms.each(columnDefinitionXsdElements) )
        {
            ReportColumnId potentialColumnId = new ReportColumnId(
                node.getAttributes().getNamedItem("saw-sql:tableHeading").getNodeValue(), 
                node.getAttributes().getNamedItem("saw-sql:columnHeading").getNodeValue());
            if (sortColumnId.equals(potentialColumnId))
            {
                return node.getAttributes().getNamedItem("saw-sql:columnID").getNodeValue();
            }
        }
        throw new DataRetrievalException("metadata does not indicate such a sort column exists: " + sortColumnId);
    }

    private ReportParams buildReportParams(Object selector)
    {
        ReportParams params = new ReportParams();
        if(selector != null)
        {
            Class<?> clazz = selector.getClass();
            
            for(Field field: clazz.getDeclaredFields())
            {
                Object value = getValue(selector, field);
                ReportParamVariable reportParamVar = field.getAnnotation(ReportParamVariable.class);
                if(reportParamVar != null && value != null)
                {
                    Variable var = createVariable(field, value, reportParamVar);
                    params.getVariables().add(var);
                }
            }
        }
        return params;
    }

    private Variable createVariable(Field field, Object value, ReportParamVariable reportParamVar)
    {
        Class<?> fieldType = field.getType();
        Variable var = new Variable();
        if(reportParamVar.name().equals(""))
        {
            var.setName(field.getName());
        }
        else
        {
            var.setName(reportParamVar.name());
        }
        var.setValue(getVariableValue(field, value, fieldType));
        return var;
    }

    private Object getVariableValue(Field field, Object value, Class<?> fieldType)
    {
        if(fieldType.equals(String.class))
        {
            //noinspection RedundantCast
            return (String) value;
        }
        else if(fieldType.equals(LocalDate.class))
        {
            return convertLocalDateToXmlDate(value);
        }
        else if(fieldType.equals(LocalDateTime.class))
        {
            return convertDateTimeToUTCDate(value);
        }
        else if(fieldType.equals(Set.class) && field.getGenericType() instanceof ParameterizedType)
        {
            return convertSetToInClauseList(field, value);
        }
        else if(isJodaAvailable() && JodaConverters.isJodaField(fieldType))
        {
            return JodaConverters.convertJodaValueToReportVariableValue(value);
        }
        else
        {
            throw new RowmapConfigurationException("Unexpected data type passed in - field: " + field);
        }
    }

    private Object convertSetToInClauseList(Field field, Object value)
    {
        ParameterizedType parameterizedFieldType = (ParameterizedType) field.getGenericType();
        Type setType = parameterizedFieldType.getActualTypeArguments()[0];
        if (setType.equals(String.class))
        {
            @SuppressWarnings("unchecked") //this is actually checked in the if() statement directly above
            Set<String> set = (Set<String>) value;
            Iterable<String> quotedStrings = Iterables.transform(set, input -> "'" + input + "'");
            return Joiner.on(",").join(quotedStrings);
        }
        else
        {
            throw new RowmapConfigurationException("Unexpected data type passed in - field: " + field);
        }
    }

    /**
     * It's important to send dates oriented to UTC, since the dates are stored in the data warehouse as UTC.
     */
    Date convertDateTimeToUTCDate(Object value)
    {
        LocalDateTime dateTime = (LocalDateTime)value;
        return Date.from(dateTime.toInstant(ZoneOffset.UTC));
    }

    /**
     * oddly, we can't just convert this to an xml 'date' object; Answers doesn't translate it correctly
     * to sql syntax, as it does when we pass an xml 'dateTime' object.  So, we'll just pass a string
     * ready for sql.
     */
    String convertLocalDateToXmlDate(Object value)
    {
        LocalDate localDate = (LocalDate)value;
        return "date '" + localDate + "'";
    }

    private Object getValue(Object reportParams, Field field) throws AssertionError
    {
        field.setAccessible(true);
        Object value;
        try
        {
            value = field.get(reportParams);
        }
        catch(IllegalAccessException e)
        {
            throw new AssertionError("We called field.setAccessible(true)", e);
        }
        return value;
    }
    
    private String createXmlReportWithAppropriateOrdering(
        ReportPath reportPathConfiguration,
        ReportParams params,
        String sortColumnId,
        SortDirection direction)
    {
        String xml = buildXmlFromParams(reportPathConfiguration, params);
        return prepareXml(xml, sortColumnId, direction);
    }

    private String buildXmlFromParams(ReportPath reportPathConfiguration, ReportParams params)
    {
        final Span span = tracer.buildSpan("apply-report-params").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            ReportRef report = new ReportRef();
            report.setReportPath(reportPathConfiguration.value());

            try
            {
                return (String) reportEditingService.applyReportParams(report, params, true, sessionId);
            }
            catch (SOAPFaultException e)
            {
                recentException = e;
                throw new DataRetrievalException(
                    String.format(
                        "unable to generate xml for report %s with %s; details follow:\n%s",
                        reportPathConfiguration.value(),
                        formatParamsAsString(params),
                        SoapFaults.getDetailsAsString(e.getFault())
                    ),
                    e
                );
            }
        }
        finally
        {
            span.finish();
        }
    }

    private String queryForMetadata(ReportPath reportPathConfiguration)
    {
        final Span span = tracer.buildSpan("query-for-metadata").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            XMLQueryOutputFormat outputFormat = XMLQueryOutputFormat.SAW_ROWSET_SCHEMA;
            XMLQueryExecutionOptions executionOptions = new XMLQueryExecutionOptions();
            executionOptions.setMaxRowsPerPage(-1);
            executionOptions.setPresentationInfo(true);

            ReportRef report = new ReportRef();
            report.setReportPath(reportPathConfiguration.value());

            QueryResults results = queryXmlViewServiceAndHandleExceptions(
                reportPathConfiguration,
                new ReportParams(),
                report,
                outputFormat,
                executionOptions
            );
            return results.getRowset();
        }
        finally
        {
            span.finish();
        }
    }

    private String queryForData(String xmlReport, ReportParams reportParams, int maxResults)
    {
        final Span span = tracer.buildSpan("query-for-data").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            XMLQueryOutputFormat outputFormat = XMLQueryOutputFormat.SAW_ROWSET_DATA;

            QueryResults results = queryXmlViewServiceWithXmlAndHandleExceptions(
                xmlReport,
                outputFormat,
                reportParams,
                maxResults
            );

            if (!results.isFinished())
            {
                String queryId = results.getQueryID();
                xmlViewService.cancelQuery(queryId, sessionId);
            }
            return results.getRowset();
        }
        finally
        {
            span.finish();
        }
    }

    private QueryResults queryXmlViewServiceWithXmlAndHandleExceptions(
        String xmlReport,
        XMLQueryOutputFormat outputFormat,
        ReportParams reportParams,
        int maxRowsPerPage)
    {
        XMLQueryExecutionOptions executionOptions = new XMLQueryExecutionOptions();
        executionOptions.setMaxRowsPerPage(maxRowsPerPage);

        // note: the docs I've found don't explain this setting, but it seems
        // that maxRowsPerPage is ignored when it is false.
        executionOptions.setAsync(maxRowsPerPage != -1);

        executionOptions.setPresentationInfo(true);

        ReportRef report = new ReportRef();
        report.setReportXml(xmlReport);

        try
        {
            return xmlViewService.executeXMLQuery(
                report,
                outputFormat,
                executionOptions,
                reportParams,
                sessionId);
        }
        catch (SOAPFaultException e)
        {
            recentException = e;
            throw new DataRetrievalException(
                    String.format(
                        "unable to query with xml:\n%s\n\nsoapfault details follow:\n%s",
                        xmlReport,
                        SoapFaults.getDetailsAsString(e.getFault())), 
                    e);
        }
        catch (RuntimeException e)
        {
            recentException = e;
            throw new DataRetrievalException(
                    String.format("unable to query with xml:\n" +
                                  "%s", xmlReport), e);
        }
    }

    private String queryForMetadataAndData(
        ReportPath reportPathConfiguration,
        ReportParams reportParams,
        int maxRowsPerPage)
    {
        final Span span = tracer.buildSpan("query-for-metadata-and-data").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            ReportRef report = new ReportRef();
            report.setReportPath(reportPathConfiguration.value());

            XMLQueryOutputFormat outputFormat = XMLQueryOutputFormat.SAW_ROWSET_SCHEMA_AND_DATA;
            XMLQueryExecutionOptions executionOptions = new XMLQueryExecutionOptions();
            executionOptions.setMaxRowsPerPage(maxRowsPerPage);

            // note: the docs I've found don't explain this setting, but it seems
            // that maxRowsPerPage is ignored when it is false.
            executionOptions.setAsync(maxRowsPerPage != -1);

            executionOptions.setPresentationInfo(true);
            QueryResults queryResults = queryXmlViewServiceAndHandleExceptions(
                reportPathConfiguration,
                reportParams,
                report,
                outputFormat,
                executionOptions
            );

            if (!queryResults.isFinished())
            {
                String queryId = queryResults.getQueryID();
                xmlViewService.cancelQuery(queryId, sessionId);
            }
            return queryResults.getRowset();
        }
        finally
        {
            span.finish();
        }
    }

    private QueryResults queryXmlViewServiceAndHandleExceptions(
            ReportPath reportPathConfiguration,
            ReportParams reportParams, ReportRef report,
            XMLQueryOutputFormat outputFormat,
            XMLQueryExecutionOptions executionOptions)
    {
        QueryResults queryResults;
        try
        {
            queryResults = xmlViewService.executeXMLQuery(
                report, 
                outputFormat, 
                executionOptions, 
                reportParams, 
                sessionId);
        }
        catch (SOAPFaultException e)
        {
            recentException = e;
            throw new DataRetrievalException(
                    String.format(
                        "unable to query report %s with %s; details follow:\n%s", 
                        reportPathConfiguration.value(),
                        formatParamsAsString(reportParams),
                        SoapFaults.getDetailsAsString(e.getFault())), 
                    e);
        }
        catch (RuntimeException e)
        {
            recentException = e;
            throw new DataRetrievalException(
                String.format(
                    "unable to query report %s with %s", 
                    reportPathConfiguration.value(),
                    formatParamsAsString(reportParams)), 
                        e);
        }
        return queryResults;
    }

    /**
     * modifies the xml definition of the query to order the results by the desired column
     */
    String prepareXml(String xml, String sortColumnId, SortDirection sortDirection)
    {
        Document reportDoc = buildXmlReportDocument(xml);
        replaceColumnOrderChildren(sortColumnId, sortDirection, reportDoc);
        return writeDocument(reportDoc);
    }

    void replaceColumnOrderChildren(String sortColumnId, SortDirection sortDirection, Document reportDoc) {
        NodeList list = searchForColumnOrder(reportDoc);

        Node columnOrder;
        if (list.getLength() == 0)
        {
            Node criteriaNode = searchForCriteriaNode(reportDoc);
            columnOrder = reportDoc.createElementNS(SAW_URI, "saw:columnOrder");
            criteriaNode.appendChild(columnOrder);
        }
        else
        {
            columnOrder = list.item(0);
        }

        for (Node child : Doms.each(columnOrder.getChildNodes()))
        {
            columnOrder.removeChild(child);
        }
        Element columnOrderRef = reportDoc.createElementNS(SAW_URI, "saw:columnOrderRef");

        addAttribute("columnID", sortColumnId, columnOrderRef, reportDoc);
        addAttribute("direction", sortDirection.name().toLowerCase(), columnOrderRef, reportDoc);

        columnOrder.appendChild(columnOrderRef);
    }

    private NodeList searchForColumnOrder(Document reportDoc) {
        try
        {
            return (NodeList) columnOrderExpression.evaluate(reportDoc, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e)
        {
            throw new RuntimeException("unable to evaluate xpath expression on document", e);
        }
    }

    private Node searchForCriteriaNode(Document reportDoc) {
        try
        {
            return (Node) criteriaExpression.evaluate(reportDoc, XPathConstants.NODE);
        }
        catch (XPathExpressionException e)
        {
            throw new RuntimeException("unable to evaluate xpath expression on document", e);
        }
    }

    private void addAttribute(String name, String sortColumnId, Element columnOrderRef, Document reportDoc) {
        Attr columnID = reportDoc.createAttribute(name);
        columnID.setValue(sortColumnId);
        columnOrderRef.getAttributes().setNamedItem(columnID);
    }

    String writeDocument(Document reportDoc) {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer;
        try {
            transformer = factory.newTransformer();
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException(e);
        }

        DOMSource source = new DOMSource(reportDoc);
        StringWriter writer = new StringWriter();
        StreamResult result = new StreamResult(writer);
        try {
            transformer.transform(source, result);
        } catch (TransformerException e) {
            throw new RuntimeException(e);
        }
        return writer.toString();
    }

    private String formatParamsAsString(ReportParams params)
    {
        return String.format("[variables=%s]", asMap(params.getVariables()));
    }

    private Map<String, Object> asMap(List<Variable> variables)
    {
        Map<String, Object> variableMap = new HashMap<>();
        for (Variable variable : variables)
        {
            variableMap.put(variable.getName(), variable.getValue());
        }
        return variableMap;
    }


    Document buildRowsetDocument(String rowset)
    {
        final Span span = tracer.buildSpan("parse-rowset-document").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            return parseXmlString(rowset, "rowset from OBIEE");
        }
        finally
        {
            span.finish();
        }
    }

    private Document buildXmlReportDocument(String report)
    {
        final Span span = tracer.buildSpan("parse-report-document").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            return parseXmlString(report, "xml report definition");
        }
        finally
        {
            span.finish();
        }
    }

    private Document parseXmlString(String xml, String description) {
        if (xml == null) {
            throw new DataRetrievalException(description + " is null");
        }

        if (xml.isEmpty()) {
            throw new DataRetrievalException(description + " is empty");
        }

        InputSource inputsource = new InputSource(new StringReader(xml));
        String parseErrorMessage = "cannot parse " + description;
        try
        {
            return builder.parse(inputsource);
        }
        catch (SAXParseException e)
        {
            recentException = e;
            throw new DataRetrievalException(
                String.format(
                    parseErrorMessage + "; error on line %s and column %s",
                    e.getLineNumber(),
                    e.getColumnNumber()),
                e);
        }
        catch (SAXException | IOException e)
        {
            recentException = e;
            throw new DataRetrievalException(parseErrorMessage, e);
        }
    }

    NodeList getColumnSchemaNodesFromPreamble(Document doc)
    {
        final Span span = tracer.buildSpan("xpath-for-schema").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            return evaluateXsdXPathAndHandleExceptions(doc);
        }
        finally
        {
            span.finish();
        }
    }

    private NodeList evaluateXsdXPathAndHandleExceptions(Document doc)
    {
        try
        {
            return (NodeList) xsdElementExpression.evaluate(doc, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e)
        {
            recentException = e;
            throw new RuntimeException("unable to evaluate xpath expression on document", e);
        }
    }

    NodeList getRows(Document doc)
    {
        final Span span = tracer.buildSpan("xpath-for-data").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            return executeRowXPathQueryAndHandleExceptions(doc);
        }
        finally
        {
            span.finish();
        }
    }

    private NodeList executeRowXPathQueryAndHandleExceptions(Document doc)
    {
        try
        {
            return (NodeList) rowExpression.evaluate(doc, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e)
        {
            recentException = e;
            throw new RuntimeException("unable to evaluate xpath expression on document", e);
        }
    }

    static class RowsetNamespaceContext implements NamespaceContext {

        public String getNamespaceURI(String prefix) {
            if (prefix == null) throw new NullPointerException("Null prefix");
            else if ("rowset".equals(prefix)) return "urn:schemas-microsoft-com:xml-analysis:rowset";
            else if ("saw".equals(prefix)) return SAW_URI;
            else if ("sawx".equals(prefix)) return "com.siebel.analytics.web/expression/v1.1";
            else if ("xsd".equals(prefix)) return XMLConstants.W3C_XML_SCHEMA_NS_URI;
            else if ("xml".equals(prefix)) return XMLConstants.XML_NS_URI;
            return XMLConstants.NULL_NS_URI;
        }

        // This method isn't necessary for XPath processing.
        public String getPrefix(String uri) {
            throw new UnsupportedOperationException();
        }

        // This method isn't necessary for XPath processing either.
        public Iterator<?> getPrefixes(String uri) {
            throw new UnsupportedOperationException();
        }
    }


    @Override
    public void validate()
    {
        checkOpen();
        validateNoRecentErrors();
        validateAnswersSession();
        validateBIServerSession();
    }
    
    private void validateNoRecentErrors()
    {
        if (recentException != null)
        {
            throw new IllegalStateException(
                "a recent exception has occurred.  Because it appears that long-running " +
                "Obiee Answers sessions can sometimes experience unsual errors, this session has been " +
                "closed due to the following recent exception:",
                recentException);
        }
    }

    private void validateAnswersSession()
    {
        final Span span = buildTopLevelSpan(tracer, "get-current-user");
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            sawSessionService.getCurUser(sessionId);
        }
        catch (RuntimeException e)
        {
            throw new IllegalStateException("manager is no longer usable", e);
        }
        finally
        {
            span.finish();
        }
    }

    private void validateBIServerSession()
    {
        ReportRef report = new ReportRef();
        report.setReportPath(VALIDATION_REPORT_PATH);
        
        XMLQueryOutputFormat outputFormat = XMLQueryOutputFormat.SAW_ROWSET_SCHEMA_AND_DATA;
        XMLQueryExecutionOptions executionOptions = new XMLQueryExecutionOptions();
        executionOptions.setMaxRowsPerPage(1);
        executionOptions.setPresentationInfo(true);
        ReportParams reportParams = new ReportParams();
        final Span span = buildTopLevelSpan(tracer, "validate-bi-server-session");
        overrideResourceName(span, VALIDATION_REPORT_PATH);
        try (Scope ignored = tracer.scopeManager().activate(span, false))
        {
            xmlViewService.executeXMLQuery(
                report, 
                outputFormat, 
                executionOptions, 
                reportParams, 
                sessionId);
        }
        catch (SOAPFaultException e)
        {
            throw new DataRetrievalException(
                    String.format(
                        "unable to query report %s; details follow:\n%s", 
                        VALIDATION_REPORT_PATH,
                        SoapFaults.getDetailsAsString(e.getFault())), 
                    e);
        }
        finally
        {
            span.finish();
        }
    }

    private void overrideResourceName(Span span, String resourceName)
    {
        span.setTag("resource.name", resourceName);
    }


    public void setQueryTimeout(long time, TimeUnit unit)
    {
        int readTimeout = (int) unit.toMillis(time);
        setReadTimeout(sawSessionService, readTimeout);
        setReadTimeout(xmlViewService, readTimeout);
        setReadTimeout(reportEditingService, readTimeout);
    }

    private void setReadTimeout(Object port, int readTimeout)
    {
        PortConfigurer portConfigurer = new PortConfigurer((BindingProvider) port);
        portConfigurer.setReadTimeout(readTimeout);
    }
    
    
}
