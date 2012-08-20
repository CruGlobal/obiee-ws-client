    package org.ccci.obiee.client.rowmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThan;

import java.util.List;

import org.apache.log4j.Logger;
import org.ccci.obiee.client.rowmap.SaiDonationRow.SaiDonationParameters;
import org.ccci.obiee.client.rowmap.impl.AnalyticsManagerImpl;
import org.ccci.obiee.client.rowmap.impl.StopwatchOperationTimer;
import org.joda.time.LocalDate;
import org.testng.annotations.AfterMethod;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

public class RowmapIntegrationTest
{

    AnalyticsManagerFactory factory;
    AnalyticsManager manager;
    
    @BeforeClass
    public void setupFactory()
    {
        factory = new AnalyticsManagerConfigurer().getAMFactory();
    }
    
    @BeforeMethod
    public void setupManager()
    {
        manager = factory.createAnalyticsManager();
        ((AnalyticsManagerImpl) manager).setOperationTimer(new StopwatchOperationTimer());
    }
    
    @AfterMethod
    public void closeManager()
    {
        manager.close();
    }
    

    @Test
    public void testRetrieveWithNoParameters() throws Exception
    {
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }
    
    @Test
    public void testRetrieveWithDesignationParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";
        
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }
    
    @Test
    public void testRetrieveWithAccountNumberParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.accountNumber = "000376764";
        
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }
    
    @Test
    public void testRetrieveWithAccountNumberAndDesignationParameters() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.accountNumber = "000442787";
    	params.designationNumber = "0378570";
        
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();
        
        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }
    
    @Test(enabled = false) //not getting any results; i need to look into this further
    public void testRetrieveWithDateParameter() throws Exception
    {
    	SaiDonationParameters params = new SaiDonationParameters();
    	params.donationRangeBegin = new LocalDate(2009, 12, 1);
    	params.donationRangeEnd = new LocalDate(2009, 12, 31);
        
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }
    
    @Test(enabled = false)
    public void testSortByAmount() throws Exception
    {
    	SortDirection direction = SortDirection.ASCENDING;
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
    	query.orderBy(SaiDonationRow.report.getColumn("amount"), direction);
    	List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }
    
    @Test(enabled = false)
    public void testSortByAmountDesc() throws Exception
    {
    	SortDirection direction = SortDirection.DESCENDING;
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
    	query.orderBy(SaiDonationRow.report.getColumn("amount"), direction);
    	List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }
    
    @Test
    public void testSortByDate() throws Exception
    {
    	SortDirection direction = SortDirection.ASCENDING;
    	Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
    	query.orderBy(SaiDonationRow.report.getColumn("transactionDate"), direction);
    	List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }
    
    @Test(enabled = false)
    public void testSortByAmountAndDesigParam() throws Exception
    {
    	SortDirection direction = SortDirection.ASCENDING;
    	SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";
        
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        query.orderBy(SaiDonationRow.report.getColumn("amount"), direction);
    	List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        printRowsize(rows);
    }

    Logger log = Logger.getLogger(RowmapIntegrationTest.class);
    
    private void printRowsize(List<SaiDonationRow> rows)
    {
        log.debug("returned " + rows.size() + " rows");
    }
}
