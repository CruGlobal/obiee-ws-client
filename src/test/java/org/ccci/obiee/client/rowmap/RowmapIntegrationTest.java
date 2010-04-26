package org.ccci.obiee.client.rowmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.List;

import org.hamcrest.Matchers;
import org.joda.time.LocalDate;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.siebel.analytics.web.soap.v5.SAWSessionService;
import com.siebel.analytics.web.soap.v5.XmlViewService;

public class RowmapIntegrationTest
{
    private static final String USERNAME = "***";
    private static final String PASSWORD = "***";

    AnalyticsManagerFactory factory;
    AnalyticsManager manager;

    @BeforeClass
    public void setupFactory()
    {
        factory = new AnalyticsManagerFactoryImpl(new SAWSessionService(), new XmlViewService(), USERNAME, PASSWORD);
    }
    
    @BeforeMethod
    public void setupManager()
    {
        manager = factory.createAnalyticsManager();
    }
    
    @AfterMethod
    public void closeManager()
    {
        manager.close();
    }
    

    @Test(enabled = false)
    public void testRetrieveWithNoParameters()
    {
        List<SaiDonationRow> rows = manager.query(SaiDonationRow.class);
        assertThat(rows, Matchers.hasSize(70));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Shires, Mark R & Carol"));
        assertThat(first.getAccountNumber(), is("000376764"));
        assertThat(first.getAmount(), is(new BigDecimal("30.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("EFT"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2009, 1, 15)));
        assertThat(first.getTransactionItemRowWid(), is("11768063.0"));
        assertThat(first.getTransactionNumber(), is("1F9Z500-000376764"));
    }
}
