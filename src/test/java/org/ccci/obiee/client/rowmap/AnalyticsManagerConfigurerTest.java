package org.ccci.obiee.client.rowmap;

import io.opentracing.noop.NoopTracerFactory;
import org.testng.annotations.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.notNullValue;

public class AnalyticsManagerConfigurerTest
{

    @Test
    public void testSimpleConstruction()
    {
        AnalyticsManagerFactory factory = new AnalyticsManagerConfigurer(NoopTracerFactory.create()).getAMFactory();
        assertThat(factory, notNullValue());
    }
}
