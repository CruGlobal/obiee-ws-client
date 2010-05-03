package org.ccci.obiee.client.rowmap;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.Calendar;
import java.util.Date;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ccci.obiee.rowmap.annotation.ReportParamVariable;
import org.ccci.obiee.rowmap.annotation.ReportPath;
import org.hamcrest.core.SubstringMatcher;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.siebel.analytics.web.soap.v5.SAWSessionServiceSoap;
import com.siebel.analytics.web.soap.v5.XmlViewServiceSoap;
import com.siebel.analytics.web.soap.v5.model.QueryResults;
import com.siebel.analytics.web.soap.v5.model.ReportParams;
import com.siebel.analytics.web.soap.v5.model.ReportRef;
import com.siebel.analytics.web.soap.v5.model.Variable;
import com.siebel.analytics.web.soap.v5.model.XMLQueryExecutionOptions;
import com.siebel.analytics.web.soap.v5.model.XMLQueryOutputFormat;


public class AnalyticsManagerImpl implements AnalyticsManager
{

    private final String sessionId;
    private final SAWSessionServiceSoap sawSessionService;
    private final XmlViewServiceSoap xmlViewService;
    private final XPathFactory xpathFactory;
    private XPathExpression xsdElementExpression;
    private XPathExpression rowExpression;
    private final DocumentBuilder builder;
    private final ConverterStore converterStore;
    
    private boolean closed = false;

    /**
     * Assumes that the caller has logged us in to OBIEE already.  
     * 
     * @param sessionId used for logout
     * @param sawSessionService used for logout
     * @param xmlViewService used for retrieving report queries
     * @param converterStore
     */
    public AnalyticsManagerImpl(String sessionId, 
                                SAWSessionServiceSoap sawSessionService,
                                XmlViewServiceSoap xmlViewService,
                                ConverterStore converterStore)
    {
        this.sessionId = sessionId;
        this.sawSessionService = sawSessionService;
        this.xmlViewService = xmlViewService;
        this.converterStore = converterStore;

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
        sawSessionService.logoff(sessionId);
    }

    private void checkOpen()
    {
        if (closed) throw new IllegalStateException("already closed");
    }
    
    public <T> List<T> query(Class<T> rowType) throws Exception
    {
    	return query(rowType, null);
    }
    
    public <T> List<T> query(Class<T> rowType, Object reportParams) throws Exception
    {
    	checkOpen();
        if (rowType == null)
            throw new NullPointerException("rowType is null");
        
        ReportPath reportPathConfiguration = rowType.getAnnotation(ReportPath.class);
        if (reportPathConfiguration == null)
        {
            throw new IllegalArgumentException(rowType.getName() + " is not a valid OBIEE report row");
        }
        
        String rowset = queryForRowsetXml(reportPathConfiguration, reportParams);
        Document doc = buildDocument(rowset);
        RowBuilder<T> rowBuilder = buildRowBuilder(rowType, doc);
        NodeList rows = getRows(doc);
        
        List<T> results = new ArrayList<T>();
        for (Node row : each(rows))
        {
            T rowInstance = rowBuilder.buildRowInstance(row);
            results.add(rowInstance);
        }
        
        return results;
	}
    
    private String queryForRowsetXml(ReportPath reportPathConfiguration, Object reportParams ) throws Exception
    {
        ReportRef report = new ReportRef();
        report.setReportPath(reportPathConfiguration.value());
        
        XMLQueryOutputFormat outputFormat = XMLQueryOutputFormat.SAW_ROWSET_SCHEMA_AND_DATA;
        XMLQueryExecutionOptions executionOptions = new XMLQueryExecutionOptions();
        executionOptions.setMaxRowsPerPage(-1);
        executionOptions.setPresentationInfo(true);
        
        ReportParams params = new ReportParams();
        
        if(reportParams != null)
        {
        	buildReportParams(reportParams, params);
        }
        
        QueryResults queryResults;
        try
        {
            queryResults = xmlViewService.executeXMLQuery(
                report, 
                outputFormat, 
                executionOptions, 
                params, 
                sessionId);
        }
        catch (RuntimeException e)
        {
            throw new DataRetrievalException("unable to query OBIEE server", e);
        }
        
        return queryResults.getRowset();
    }

	private void buildReportParams(Object reportParams, ReportParams params) throws IllegalAccessException 
	{
		Variable var;
		Class clazz = reportParams.getClass();
		
		for(Field field: clazz.getDeclaredFields())
		{
			ReportParamVariable reportParamVar = field.getAnnotation(ReportParamVariable.class);
			if(reportParamVar != null && field.get(reportParams) != null)
			{
				String fieldType = field.getType().getName();
				var = new Variable();
				if(reportParamVar.name().equals(""))
				{
					var.setName(field.getName());
				}
				else
				{
					var.setName(reportParamVar.name());
				}
				if(fieldType.equals("java.lang.String"))
				{
					var.setValue(field.get(reportParams).toString());
				}
				else if(fieldType.equals("org.joda.time.LocalDate"))
				{
					LocalDate ld = (LocalDate)field.get(reportParams);
					DateTime dTime = ld.toDateTimeAtCurrentTime();
					Date dt = dTime.toDate();
					var.setValue(dt);
				}
				params.getVariables().add(var);
			}
		}
	}
    
    <T> RowBuilder<T> buildRowBuilder(Class<T> rowType, Document doc)
    {
        NodeList columnDefinitionXsdElements = getColumnSchemaNodesFromPreamble(doc);
        
        Map<ReportColumnId, XPathExpression> columnValueExpressionms = new HashMap<ReportColumnId, XPathExpression>();
        
        for (Node node : each(columnDefinitionXsdElements) )
        {
            String elementName = node.getAttributes().getNamedItem("name").getNodeValue();
            String tableHeading = node.getAttributes().getNamedItem("saw-sql:tableHeading").getNodeValue();
            String columnHeading = node.getAttributes().getNamedItem("saw-sql:columnHeading").getNodeValue();
            XPath xpath = xpathFactory.newXPath();
            xpath.setNamespaceContext(new RowsetNamespaceContext());
            
            XPathExpression columnValueExpression;
            try
            {
                columnValueExpression = xpath.compile("rowset:" + elementName + "/text()");
            }
            catch (XPathExpressionException e)
            {
                throw new RuntimeException(e);
            }
            
            columnValueExpressionms.put(new ReportColumnId(tableHeading, columnHeading), columnValueExpression);
        }
        return new RowBuilder<T>(columnValueExpressionms, rowType, converterStore);
    }
    

    Document buildDocument(String rowset)
    {
        InputSource inputsource = new InputSource(new StringReader(rowset));
        try
        {
            return builder.parse(inputsource );
        }
        catch (SAXParseException e)
        {
            throw new DataRetrievalException(
                String.format(
                    "cannot parse rowset from OBIEE; error on line %s and column %s", 
                    e.getLineNumber(),
                    e.getColumnNumber()), 
                e);
        }
        catch (SAXException e)
        {
            throw new DataRetrievalException("cannot parse rowset from OBIEE", e);
        }
        catch (IOException e)
        {
            throw new DataRetrievalException("cannot parse rowset from OBIEE", e);
        }
    }

    NodeList getColumnSchemaNodesFromPreamble(Document doc)
    {
        try
        {
            return (NodeList) xsdElementExpression.evaluate(doc, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e)
        {
            throw new RuntimeException("unable to evaluate xpath expression on document", e);
        }
        
    }
    
    NodeList getRows(Document doc)
    {
        try
        {
            return (NodeList) rowExpression.evaluate(doc, XPathConstants.NODESET);
        }
        catch (XPathExpressionException e)
        {
            throw new RuntimeException("unable to evaluate xpath expression on document", e);
        }
        
    }

    static class RowsetNamespaceContext implements NamespaceContext {

        public String getNamespaceURI(String prefix) {
            if (prefix == null) throw new NullPointerException("Null prefix");
            else if ("rowset".equals(prefix)) return "urn:schemas-microsoft-com:xml-analysis:rowset";
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

    private Iterable<Node> each(final NodeList nodeList)
    {
        return new Iterable<Node>()
        {
            
            public Iterator<Node> iterator()
            {
                return new Iterator<Node>()
                {
                    int index = 0;

                    public boolean hasNext()
                    {
                        return nodeList.getLength() > index;
                    }

                    public Node next()
                    {
                        if (index == nodeList.getLength())
                            throw new NoSuchElementException();
                        Node next = nodeList.item(index);
                        index++;
                        return next;
                    }

                    public void remove()
                    {
                        throw new UnsupportedOperationException();
                    }
                };
            }
        };
    }
}
