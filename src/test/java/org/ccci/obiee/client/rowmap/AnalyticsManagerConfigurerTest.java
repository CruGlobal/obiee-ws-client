package org.ccci.obiee.client.rowmap;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

import org.testng.annotations.Test;

public class AnalyticsManagerConfigurerTest
{

    @Test
    public void testSimpleConstruction()
    {
        AnalyticsManagerFactory factory = new AnalyticsManagerConfigurer().getAMFactory();
        assertThat(factory, notNullValue());
    }
}
