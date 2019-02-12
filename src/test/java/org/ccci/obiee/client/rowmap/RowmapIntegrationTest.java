package org.ccci.obiee.client.rowmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;

import java.time.LocalDate;
import java.util.List;

import io.opentracing.Tracer;
import org.apache.log4j.Logger;
import org.ccci.obiee.client.rowmap.SaiDonationRow.SaiDonationParameters;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.hamcrest.TypeSafeDiagnosingMatcher;
import org.hamcrest.core.CombinableMatcher;
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
        Tracer tracer = new StopwatchTracer();
        factory = new AnalyticsManagerConfigurer(tracer).getAMFactory();
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
    

    @Test
    public void testRetrieveWithNoParameters() throws Exception
    {
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        assertThat(rows, everyItem(
            Matchers.<SaiDonationRow>hasProperty("designationNumber", equalTo("0378570"))));
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

        assertThat(rows.size(), greaterThan(1000));
        assertThat(rows, everyItem(
            Matchers.<SaiDonationRow>hasProperty("designationNumber", equalTo(params.designationNumber))));
        printRowsize(rows);
    }

    @Test
    public void testRetrieveWithMaxResults() throws Exception
    {
        SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";

        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.setMaxResults(1000)
            .getResultList();

        assertThat(rows.size(), equalTo(1000));
        assertThat(rows, everyItem(
            Matchers.<SaiDonationRow>hasProperty("designationNumber", equalTo(params.designationNumber))));
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
    
    @Test(enabled = true)
    public void testRetrieveWithDateParameter() throws Exception
    {
        SaiDonationParameters params = new SaiDonationParameters();
        params.donationRangeBegin = LocalDate.of(2011, 1, 1);
        params.donationRangeEnd = LocalDate.of(2011, 12, 31);
        
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows, hasSize(greaterThan(0)));
        assertThat(rows, everyItem(
            Matchers.<SaiDonationRow>hasProperty("transactionDate", betweenBoundaries(params))));
        printRowsize(rows);
    }

    private CombinableMatcher<LocalDate> betweenBoundaries(SaiDonationParameters params)
    {
        Matcher<LocalDate> lowerBound = isOnOrAfter(params.donationRangeBegin);
        Matcher<LocalDate> upperBound = isOnOrBefore(params.donationRangeEnd);

        return  Matchers.both(lowerBound).and(upperBound);
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
        SaiDonationRow previous = null;
        for (SaiDonationRow row : rows) {
            if (previous != null)
                assertThat(row.getTransactionDate(), isOnOrAfter(previous.getTransactionDate()));
            previous = row;
        }
        printRowsize(rows);
    }

    private Matcher<LocalDate> isOnOrAfter(final LocalDate date)
    {
        return new TypeSafeDiagnosingMatcher<LocalDate>()
        {
            public void describeTo(Description description)
            {
                description.appendText("is on or after").appendValue(date);
            }

            @Override
            protected boolean matchesSafely(LocalDate item, Description mismatchDescription)
            {
                boolean onOrAfter = !item.isBefore(date);
                if (!onOrAfter)
                {
                    mismatchDescription.appendValue(item);
                }
                return onOrAfter;
            }
        };
    }

    private Matcher<LocalDate> isOnOrBefore(final LocalDate date)
    {
        return new TypeSafeDiagnosingMatcher<LocalDate>()
        {
            public void describeTo(Description description)
            {
                description.appendText("is on or before").appendValue(date);
            }

            @Override
            protected boolean matchesSafely(LocalDate item, Description mismatchDescription)
            {
                boolean onOrBefore = !item.isAfter(date);
                if (!onOrBefore)
                {
                    mismatchDescription.appendValue(item);
                }
                return onOrBefore;
            }
        };
    }



    @Test(enabled = true)
    public void testSortByAmountWithDesigParam() throws Exception
    {
        SortDirection direction = SortDirection.ASCENDING;
        SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";
        
        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        query.orderBy(SaiDonationRow.report.getColumn("transactionAmount"), direction);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows.size(), greaterThan(0));
        SaiDonationRow previous = null;
        for (SaiDonationRow row : rows) {
            if (previous != null)
                assertThat(row.getTransactionAmount(), greaterThanOrEqualTo(previous.getTransactionAmount()));
            previous = row;
        }
        assertThat(rows, everyItem(
            Matchers.<SaiDonationRow>hasProperty("designationNumber", equalTo(params.designationNumber))));
        printRowsize(rows);
    }


    @Test(enabled = true)
    public void testRetrieveWithNoResults() throws Exception
    {
        SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";
        params.donationRangeBegin = LocalDate.of(2011, 1, 1);
        params.donationRangeEnd = LocalDate.of(2010, 1, 1);

        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows, hasSize(equalTo(0)));
    }

    @Test(enabled = true)
    public void testRetrieveWithSortingAndNoResults() throws Exception
    {
        SaiDonationParameters params = new SaiDonationParameters();
        params.designationNumber = "0478406";
        params.donationRangeBegin = LocalDate.of(2011, 1, 1);
        params.donationRangeEnd = LocalDate.of(2010, 1, 1);

        Query<SaiDonationRow> query = manager.createQuery(SaiDonationRow.report);
        query.withSelection(params);
        query.orderBy(SaiDonationRow.report.getColumn("transactionAmount"), SortDirection.ASCENDING);
        List<SaiDonationRow> rows = query.getResultList();

        assertThat(rows, hasSize(equalTo(0)));
    }

    
    @Test
    public void testValidate() throws Exception
    {
        manager.validate();
    }
    

    Logger log = Logger.getLogger(RowmapIntegrationTest.class);
    
    private void printRowsize(List<SaiDonationRow> rows)
    {
        log.debug("returned " + rows.size() + " rows");
    }
}
