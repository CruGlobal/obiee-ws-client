package org.ccci.obiee.client.rowmap.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.ccci.obiee.client.init.AnswersServiceFactory;
import org.testng.annotations.Test;

public class AnalyticsManagerFactoryTest
{

    @Test
    public void testEndpointUrlConstruction()
    {
        AnswersServiceFactory serviceFactory = new AnswersServiceFactory();
        RowmapConfiguration config = new RowmapConfiguration();
        config.setEndpointBaseUrl("https://therealendpoint.example.com:80");
        AnalyticsManagerFactoryImpl factory = new AnalyticsManagerFactoryImpl(serviceFactory, config);
        String newEndpointAddress = factory.buildNewEndpointAddress("https://theurl.in.the.wsdl.org:8080/services/TestEndpoint?TestIt");
        
        assertThat(newEndpointAddress, is("https://therealendpoint.example.com:80/services/TestEndpoint?TestIt"));
    }
}
