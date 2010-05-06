package org.ccci.obiee.client.rowmap.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.ccci.obiee.client.rowmap.impl.AnalyticsManagerImpl;
import org.ccci.obiee.client.rowmap.impl.ConverterStore;
import org.ccci.obiee.rowmap.annotation.Column;
import org.ccci.obiee.rowmap.annotation.ReportPath;
import org.ccci.obiee.rowmap.annotation.Scale;
import org.joda.time.LocalDate;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class AnalyticsManagerTest
{

    AnalyticsManagerImpl manager;
    
    @BeforeMethod
    public void createManager()
    {
        manager = new AnalyticsManagerImpl(null, null, null, null, ConverterStore.buildDefault());
    }
    
    
    @Test
    public void testBuildRowBuilder() throws Exception
    {
        Class<TestRow> rowType = TestRow.class;
        Document doc = readSimpleRowset();
        
        RowBuilder<TestRow> rowBuilder = manager.buildRowBuilder(rowType, doc);
        assertThat(rowBuilder, is(notNullValue()));
    }
    
    @Test
    public void testReadRows() throws Exception
    {
        NodeList rows = manager.getRows(readSimpleRowset());
        assertThat(rows.getLength(), is(5));
    }
    
    @Test
    public void testReadSchema() throws Exception
    {
        NodeList schemaElements = manager.getColumnSchemaNodesFromPreamble(readSimpleRowset());
        assertThat(schemaElements.getLength(), is(4));
        assertThat(schemaElements.item(0).getAttributes().getNamedItem("name").getNodeValue(), is("Column0"));
    }
    
    private Document readSimpleRowset() throws ParserConfigurationException, SAXException, IOException
    {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        DocumentBuilder documentBuilder = factory.newDocumentBuilder();
        
        InputStream xmlFileStream = this.getClass().getResourceAsStream("simple-rowset.xml");
        return documentBuilder.parse(xmlFileStream);
    }

    @ReportPath("/not/real")
    public static class TestRow
    {

        @Column(tableHeading = "Designation")
        private String designationNumber;

        @Column(tableHeading = "Transaction Date")
        private LocalDate transactionDate;

        @Column(tableHeading = "Transaction Item")
        @Scale(2)
        private BigDecimal amount;

        @Column(tableHeading = "Fact - Transaction Items", columnHeading = "# Transaction Items")
        private String numberOfTransactionItems;

        public String getDesignationNumber()
        {
            return designationNumber;
        }

        public LocalDate getTransactionDate()
        {
            return transactionDate;
        }

        public BigDecimal getAmount()
        {
            return amount;
        }

        public String getNumberOfTransactionItems()
        {
            return numberOfTransactionItems;
        }
        
    }
}
