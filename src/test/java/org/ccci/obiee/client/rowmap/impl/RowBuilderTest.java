package org.ccci.obiee.client.rowmap.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.util.HashMap;
import java.util.Map;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.ccci.obiee.client.rowmap.annotation.Column;
import org.ccci.obiee.client.rowmap.annotation.ReportPath;
import org.ccci.obiee.client.rowmap.impl.AnalyticsManagerImpl.RowsetNamespaceContext;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

public class RowBuilderTest
{

    
    private static final String MICROSOFT_XML_ANALYSIS_ROWSET_NS_URI = "urn:schemas-microsoft-com:xml-analysis:rowset";
    RowBuilder<Fruit> fruitBuilder;
    
    @Test
    public void testRowBuilderWithSingleResult() throws ParserConfigurationException, XPathExpressionException
    {
        Map<ReportColumnId, XPathExpression> columnToValueExpressionMapping = new HashMap<ReportColumnId, XPathExpression>();
        
        columnToValueExpressionMapping.put(
            new ReportColumnId("Fruit", "Name"), 
            compileXpath("rowset:Column0/text()"));
        
        fruitBuilder = new RowBuilder<Fruit>(columnToValueExpressionMapping, Fruit.class, ConverterStore.buildDefault());
        
        Node row = buildBananaRow();

        Fruit builtFruit = fruitBuilder.buildRowInstance(row);
        
        assertThat(builtFruit, is(notNullValue()));
        assertThat(builtFruit.name, is("Banana"));
    }

    private Node buildBananaRow() throws ParserConfigurationException
    {
        DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
        Document document = documentBuilder.newDocument();
        Node row = document.createElementNS(MICROSOFT_XML_ANALYSIS_ROWSET_NS_URI, "Row");
        Element column0 = document.createElementNS(MICROSOFT_XML_ANALYSIS_ROWSET_NS_URI, "Column0");
        row.appendChild(column0);
        column0.setTextContent("Banana");
        return row;
    }

    private XPathExpression compileXpath(String xpathExpression) throws XPathExpressionException
    {
        XPath xpath = XPathFactory.newInstance().newXPath();
        xpath.setNamespaceContext(new RowsetNamespaceContext());
        XPathExpression compiledExpression = xpath.compile(xpathExpression);
        return compiledExpression;
    }


    @ReportPath("/does/not/exist")
    public static class Fruit
    {

        @Column(tableHeading = "Fruit")
        String name;
    }
}
