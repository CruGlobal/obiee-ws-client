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

import com.siebel.analytics.web.soap.v5.ReportEditingService;
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
        factory = new AnalyticsManagerFactoryImpl(new SAWSessionService(), new XmlViewService(), new ReportEditingService(), USERNAME, PASSWORD);
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
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
        List<SaiDonationRow> rows = query.getResultList();
        
        assertThat(rows, Matchers.hasSize(295));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Shires, Mark R & Carol"));
        assertThat(first.getAccountNumber(), is("000376764"));
        assertThat(first.getAmount(), is(new BigDecimal("30.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("EFT"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2007, 1, 15)));
        assertThat(first.getTransactionNumber(), is("1F7Z500-000376764"));
    }
    
    @Test(enabled = true)
    public void testRetrieveWithDesignationParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";
        
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();
        
        assertThat(rows, Matchers.hasSize(304));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Johnson, Dirke D & Lorna"));
        assertThat(first.getAccountNumber(), is("000105923"));
        assertThat(first.getAmount(), is(new BigDecimal("150.00")));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Check"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2007, 02, 07)));
        assertThat(first.getTransactionNumber(), is("R092530-000105923"));
    }
    
    @Test(enabled = true)
    public void testRetrieveWithAccountNumberParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.accountNumber = "000376764";
        
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();
        
        assertThat(rows, Matchers.hasSize(37));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Shires, Mark R & Carol"));
        assertThat(first.getAccountNumber(), is("000376764"));
        assertThat(first.getAmount(), is(new BigDecimal("30.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("EFT"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2007, 1, 15)));
        assertThat(first.getTransactionNumber(), is("1F7Z500-000376764"));
    }
    
    @Test(enabled = true)
    public void testRetrieveWithAccountNumberAndDesignationParameters() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.accountNumber = "000442787";
    	params.designationNumber = "0378570";
        
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();
        
        assertThat(rows, Matchers.hasSize(27));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Beckman, Michelle L"));
        assertThat(first.getAmount(), is(new BigDecimal("22.00")));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Check"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2007, 01, 05)));
        assertThat(first.getTransactionNumber(), is("R090265-000442787"));
    }
    
    @Test(enabled = true)
    public void testRetrieveWithDateParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.donationBegin = new LocalDate(2009, 12, 1);
    	params.donationEnd = new LocalDate(2009, 12, 31);
        
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();
        
        assertThat(rows, Matchers.hasSize(4));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Shires, Mark R & Carol"));
        assertThat(first.getAccountNumber(), is("000376764"));
        assertThat(first.getAmount(), is(new BigDecimal("30.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Check"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2009, 12, 29)));
        assertThat(first.getTransactionNumber(), is("1-2168786"));
    }
    
    @Test(enabled = true)
    public void testSortByAmount() throws Exception
    {
    	SortDirection direction = SortDirection.ASCENDING;
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
    	query.orderBy("Transaction Item", "Amount", direction);
    	List<SaiDonationRow> rows = query.getResultList();
    	
    	assertThat(rows, Matchers.hasSize(295));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Cowsky, Albert F III and Mary"));
        assertThat(first.getAccountNumber(), is("440998856"));
        assertThat(first.getAmount(), is(new BigDecimal("2.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Wire"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2008, 01, 18)));
        assertThat(first.getTransactionNumber(), is("1I8A19C-440998856"));
    }
    
    @Test(enabled = false)
    public void testSortByAmountDesc() throws Exception
    {
    	SortDirection direction = SortDirection.DESCENDING;
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
    	query.orderBy("Transaction Item", "Amount", direction);
    	List<SaiDonationRow> rows = query.getResultList();
    	
    	assertThat(rows, Matchers.hasSize(295));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Totally Anonymous"));
        assertThat(first.getAccountNumber(), is("900000009"));
        assertThat(first.getAmount(), is(new BigDecimal("2500.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Check"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2008, 04, 17)));
        assertThat(first.getTransactionNumber(), is("4H8K207-437153485"));
    }
    
    @Test(enabled = true)
    public void testSortByDate() throws Exception
    {
    	SortDirection direction = SortDirection.ASCENDING;
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
    	query.orderBy("- Transaction Date", "Transaction Date", direction);
    	List<SaiDonationRow> rows = query.getResultList();
    	
    	assertThat(rows, Matchers.hasSize(295));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Beckman, Michelle L"));
        assertThat(first.getAccountNumber(), is("000442787"));
        assertThat(first.getAmount(), is(new BigDecimal("22.00")));
        assertThat(first.getDesignationNumber(), is("0378570"));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("Check"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2007, 01, 05)));
        assertThat(first.getTransactionNumber(), is("R090265-000442787"));
    }
    
    @Test(enabled = true)
    public void testSortByAmountAndDesigParam() throws Exception
    {
    	SortDirection direction = SortDirection.ASCENDING;
    	SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";
        
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.class);
        query.withSelection(params);
        query.orderBy("Transaction Item", "Amount", direction);
    	List<SaiDonationRow> rows = query.getResultList();
    	
    	assertThat(rows, Matchers.hasSize(304));
        SaiDonationRow first = rows.get(0);
        
        assertThat(first.getAccountName(), is("Forester, Thomas J"));
        assertThat(first.getAccountNumber(), is("000549981"));
        assertThat(first.getAmount(), is(new BigDecimal("10.00")));
        assertThat(first.getNumberOfTransactionItems(), is(1));
        assertThat(first.getSubType(), is("EFT"));
        assertThat(first.getTransactionDate(), is(new LocalDate(2007, 01, 05)));
        assertThat(first.getTransactionNumber(), is("157Z502-000549981"));
    }
}
