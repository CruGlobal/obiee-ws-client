package org.ccci.obiee.client.rowmap.impl;

import java.io.IOException;
import java.io.StringReader;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Date;
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

import org.ccci.obiee.client.rowmap.AnalyticsManager;
import org.ccci.obiee.client.rowmap.DataRetrievalException;
import org.ccci.obiee.client.rowmap.Query;
import org.ccci.obiee.client.rowmap.RowmapConfigurationException;
import org.ccci.obiee.client.rowmap.SortDirection;
import org.ccci.obiee.rowmap.annotation.ReportParamVariable;
import org.ccci.obiee.rowmap.annotation.ReportPath;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

import com.siebel.analytics.web.soap.v5.ReportEditingServiceSoap;
import com.siebel.analytics.web.soap.v5.SAWSessionServiceSoap;
import com.siebel.analytics.web.soap.v5.XmlViewServiceSoap;
import com.siebel.analytics.web.soap.v5.model.QueryResults;
import com.siebel.analytics.web.soap.v5.model.ReportParams;
import com.siebel.analytics.web.soap.v5.model.ReportRef;
import com.siebel.analytics.web.soap.v5.model.Variable;
import com.siebel.analytics.web.soap.v5.model.XMLQueryExecutionOptions;
import com.siebel.analytics.web.soap.v5.model.XMLQueryOutputFormat;

/**
 * 
 * @author Matt Drees
 * @author William Randall
 *
 */
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
    private final ReportEditingServiceSoap reportEditingService;
    
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
                                ReportEditingServiceSoap reportEditingService,
                                ConverterStore converterStore)
    {
        this.sessionId = sessionId;
        this.sawSessionService = sawSessionService;
        this.xmlViewService = xmlViewService;
        this.reportEditingService = reportEditingService;
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
    
    public <T> Query<T> createQuery(Class<T> rowType)
    {
        checkOpen();
        if (rowType == null)
            throw new NullPointerException("rowType is null");
        if (!rowType.isAnnotationPresent(ReportPath.class))
        {
            throw new IllegalArgumentException(
                rowType.getName() + " is not a valid OBIEE report row; it is not annotated @" + ReportPath.class.getSimpleName());
        }
        return new QueryImpl<T>(rowType);
    }
    
    class QueryImpl<T> implements Query<T>
    {

        private final Class<T> rowType;
        private Object selection;
        private String tableHead;
        private String colName;
        private SortDirection direction;

        public QueryImpl(Class<T> rowType)
        {
            this.rowType = rowType;
        }

        public Query<T> withSelection(Object selection)
        {
            if (selection == null) 
                throw new NullPointerException("selection is null");
            this.selection = selection;
            return this;
        }
        
        public Query<T> orderBy(String tableHead, String colName, SortDirection direction) 
        {
			if(tableHead == null)
				throw new NullPointerException("Table heading cannot be null.");
			if(colName == null)
				throw new NullPointerException("Column name cannot be null.");
				
			this.tableHead = tableHead;
			this.colName = colName;
			this.direction = direction;
			
			return this;
		}

        public List<T> getResultList()
        {
        	return query(rowType, selection, tableHead, colName, direction);
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
    }
    
    private <T> List<T> query(Class<T> rowType, Object reportParams, String tableHead, String colName, SortDirection direction)
    {
    	checkOpen();
    	
    	ReportPath reportPathConfiguration = rowType.getAnnotation(ReportPath.class);
    	
    	NodeList rows = null;
        RowBuilder<T> rowBuilder = null;
        Document doc = null;
    	
        if(tableHead != null && colName != null)
        {
        	if(direction == null)
        	{
        		direction = SortDirection.ASCENDING;
        	}
        	String sqlUsed = setupSqlForQuery(reportPathConfiguration, reportParams, tableHead, colName, direction);
        	String metadata = sqlQueryForMetadata(sqlUsed, sessionId);
	        doc = buildDocument(metadata);
	        rowBuilder = buildRowBuilder(rowType, doc);
	        
	        String data = sqlQueryForData(sqlUsed, sessionId);
	        Document docData = buildDocument(data);
	        rows = getRows(docData);
        }
        else
        {
        	String rowset = queryForRowsetXml(reportPathConfiguration, reportParams);
        	doc = buildDocument(rowset);
        	rowBuilder = buildRowBuilder(rowType, doc);
        	rows = getRows(doc);
        }
        
        List<T> results = new ArrayList<T>();
        for (Node row : each(rows))
        {
            T rowInstance = rowBuilder.buildRowInstance(row);
            results.add(rowInstance);
        }
        
        return results;
	}
    
    private String setupSqlForQuery(ReportPath reportPathConfiguration, Object reportParams, String tableHead, String colName, SortDirection direction)
    {
    	ReportRef report = new ReportRef();
        report.setReportPath(reportPathConfiguration.value());
        
        ReportParams params = new ReportParams();
        
        if(reportParams != null)
        {
        	buildReportParams(reportParams, params);
        }
        
        String sqlUsed = reportEditingService.generateReportSQL(report, params, sessionId);
        
        return prepareSql(sqlUsed, tableHead, colName, direction);
    }
    
    private String sqlQueryForMetadata(String sqlUsed, String sessionId)
    {
    	XMLQueryOutputFormat outputFormat = XMLQueryOutputFormat.SAW_ROWSET_SCHEMA;
        XMLQueryExecutionOptions executionOptions = new XMLQueryExecutionOptions();
        executionOptions.setMaxRowsPerPage(-1);
        executionOptions.setPresentationInfo(true);
        
        QueryResults results;
        try
        {
        	results = xmlViewService.executeSQLQuery(sqlUsed, outputFormat, executionOptions, sessionId);
        }
        catch(RuntimeException e)
        {
        	throw new DataRetrievalException(
        			String.format("unable to query with sql: ", sqlUsed), e);
        }
    	return results.getRowset();
    }
    
    private String sqlQueryForData(String sqlUsed, String sessionId)
    {
    	XMLQueryOutputFormat outputFormat = XMLQueryOutputFormat.SAW_ROWSET_DATA;
      
    	QueryResults results;
    	try
    	{
    		results = xmlViewService.executeSQLQuery(sqlUsed, outputFormat, new XMLQueryExecutionOptions(), sessionId);
    	}
    	catch (RuntimeException e)
        {
        	throw new DataRetrievalException(
        			String.format("unable to query with sql: ", sqlUsed), e);
        }
        return results.getRowset();
    }
    
    private String queryForRowsetXml(ReportPath reportPathConfiguration, Object reportParams)
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
        	throw new DataRetrievalException(
                    String.format(
                        "unable to query report %s with %s", 
                        reportPathConfiguration.value(),
                        formatParamsAsString(params)), 
                    e);
        }
        
        return queryResults.getRowset();
    }

	private void buildReportParams(Object reportParams, ReportParams params) 
	{
		Variable var;
		Class<?> clazz = reportParams.getClass();
		
		try
		{
			for(Field field: clazz.getDeclaredFields())
			{
				field.setAccessible(true);
				ReportParamVariable reportParamVar = field.getAnnotation(ReportParamVariable.class);
				if(reportParamVar != null && field.get(reportParams) != null)
				{
					Class<?> fieldType = field.getType();
					var = new Variable();
					if(reportParamVar.name().equals(""))
					{
						var.setName(field.getName());
					}
					else
					{
						var.setName(reportParamVar.name());
					}
					if(fieldType.equals(String.class))
					{
						var.setValue(field.get(reportParams).toString());
					}
					else if(fieldType.equals(LocalDate.class))
					{
						LocalDate ld = (LocalDate)field.get(reportParams);
						DateTime dTime = ld.toDateTimeAtCurrentTime();
						Date dt = dTime.toDate();
						var.setValue(dt);
					}
					else if(fieldType.equals(DateTime.class))
					{
						DateTime dTime = (DateTime)field.get(reportParams);
						Date dt = dTime.toDate();
						var.setValue(dt);
					}
					else
					{
						throw new RowmapConfigurationException("Unexpected data type passed in - type: " + field.getType());
					}
					params.getVariables().add(var);
				}
			}
		}
		catch(IllegalAccessException e)
		{
			AssertionError assertionError = new AssertionError("We called field.setAccessible(true)");
			assertionError.initCause(e);
			throw assertionError;
		}
		
	}
	
	/**
	 * Formats the SQL statement to be used for sorting.
	 * @param sqlUsed
	 * @param tableHead
	 * @param colName
	 * @return
	 */
	private String prepareSql(String sqlUsed, String tableHead, String colName, SortDirection direction)
	{
		tableHead = formatTableInfo(tableHead);
		colName = formatTableInfo(colName);
		
		if(sqlUsed != null)
		{
			sqlUsed = removeOrderBy(sqlUsed);
			sqlUsed = sqlUsed.concat(" ORDER BY " + tableHead + "." + colName + " " + direction.toCode());
		}
		return sqlUsed;
	}
	
	/**
	 * Formats the string passed to it in case of two word strings.
	 * @param tableInfo (column name or table heading)
	 * @return
	 */
	private String formatTableInfo(String tableInfo)
	{
		if(isOneWord(tableInfo))
		{
			return tableInfo;
		}
		
		return "\"" + tableInfo + "\"";
	}
	
	private boolean isOneWord(String tableInfo)
	{
		return tableInfo.contains(" ")?false:true;
	}
	
	private String removeOrderBy(String sqlUsed)
	{
		if(!sqlUsed.contains("ORDER BY"))
		{
			return sqlUsed;
		}
		
		int index = sqlUsed.indexOf(" ORDER BY");
		sqlUsed = sqlUsed.substring(0,index);
		return sqlUsed;
	}
	
	private String formatParamsAsString(ReportParams params)
    {
        return String.format("[variables=%s]", asMap(params.getVariables()));
    }

    private Map<String, Object> asMap(List<Variable> variables)
    {
        Map<String, Object> variableMap = new HashMap<String, Object>();
        for (Variable variable : variables)
        {
            variableMap.put(variable.getName(), variable.getValue());
        }
        return variableMap;
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
