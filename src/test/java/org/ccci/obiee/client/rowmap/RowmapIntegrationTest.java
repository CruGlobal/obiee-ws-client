package org.ccci.obiee.client.rowmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import java.math.BigDecimal;
import java.util.List;

import org.ccci.obiee.client.rowmap.SaiDonationRow.SaiDonationParameters;
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
    

    @Test(enabled = true)
    public void testRetrieveWithNoParameters() throws Exception
    {
        List<SaiDonationRow> rows = manager.createQuery(SaiDonationRow.class).getResultList();
        
        assertThat(rows, Matchers.hasSize(70));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Shires, Mark R & Carol"));
        assertThat(first.getAccountNumber(), is("000376764"));
        assertThat(first.getAmount(), is(new BigDecimal("30.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("EFT"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2009, 1, 15)));
        assertThat(first.getTransactionItemRowWid(), is("12846673.0"));
        assertThat(first.getTransactionNumber(), is("1F9Z500-000376764"));
    }
    
    @Test(enabled = true)
    public void testRetrieveWithDesignationParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";
        
        List<SaiDonationRow> rows = manager.createQuery(SaiDonationRow.class)
            .withSelection(params)
            .getResultList();
        
        assertThat(rows, Matchers.hasSize(86));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Johnson, Dirke D & Lorna"));
        assertThat(first.getAccountNumber(), is("000105923"));
        assertThat(first.getAmount(), is(new BigDecimal("50.00")));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Check"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2009, 1, 05)));
        assertThat(first.getTransactionItemRowWid(), is("13415761.0"));
        assertThat(first.getTransactionNumber(), is("R139054-000105923"));
    }
    
    @Test(enabled = true)
    public void testRetrieveWithAccountNumberParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.accountNumber = "000376764";
        
        List<SaiDonationRow> rows = manager.createQuery(SaiDonationRow.class)
            .withSelection(params)
            .getResultList();
        
        assertThat(rows, Matchers.hasSize(10));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Shires, Mark R & Carol"));
        assertThat(first.getAccountNumber(), is("000376764"));
        assertThat(first.getAmount(), is(new BigDecimal("30.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("EFT"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2009, 1, 15)));
        assertThat(first.getTransactionItemRowWid(), is("12846673.0"));
        assertThat(first.getTransactionNumber(), is("1F9Z500-000376764"));
    }
    
    @Test(enabled = true)
    public void testRetrieveWithAccountNumberAndDesignationParameters() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.accountNumber = "000442787";
    	params.designationNumber = "0378570";
        
        List<SaiDonationRow> rows = manager.createQuery(SaiDonationRow.class)
            .withSelection(params)
            .getResultList();
        
        assertThat(rows, Matchers.hasSize(9));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Beckman, Michelle L"));
        assertThat(first.getAmount(), is(new BigDecimal("44.00")));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Check"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2009, 02, 12)));
        assertThat(first.getTransactionItemRowWid(), is("13118993.0"));
        assertThat(first.getTransactionNumber(), is("R141482-000442787"));
    }
    
    @Test(enabled = true)
    public void testRetrieveWithDateParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.donationBegin = new LocalDate(2009, 12, 1);
    	params.donationEnd = new LocalDate(2009, 12, 31);
        
        List<SaiDonationRow> rows = manager.createQuery(SaiDonationRow.class)
            .withSelection(params)
            .getResultList();
        
        assertThat(rows, Matchers.hasSize(4));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Shires, Mark R & Carol"));
        assertThat(first.getAccountNumber(), is("000376764"));
        assertThat(first.getAmount(), is(new BigDecimal("30.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Check"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2009, 12, 29)));
        assertThat(first.getTransactionItemRowWid(), is("13608261.0"));
        assertThat(first.getTransactionNumber(), is("1-2168786"));
    }
}
