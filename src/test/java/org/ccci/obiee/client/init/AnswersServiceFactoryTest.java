package org.ccci.obiee.client.init;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import com.siebel.analytics.web.soap.v5.HtmlViewService;
import com.siebel.analytics.web.soap.v5.IBotService;
import com.siebel.analytics.web.soap.v5.JobManagementService;
import com.siebel.analytics.web.soap.v5.MetadataService;
import com.siebel.analytics.web.soap.v5.ReplicationService;
import com.siebel.analytics.web.soap.v5.ReportEditingService;
import com.siebel.analytics.web.soap.v5.SAWSessionService;
import com.siebel.analytics.web.soap.v5.SecurityService;
import com.siebel.analytics.web.soap.v5.WebCatalogService;
import com.siebel.analytics.web.soap.v5.XmlViewService;

public class AnswersServiceFactoryTest
{
    
    AnswersServiceFactory factory;
    
    @BeforeMethod
    public void setup()
    {
        factory = new AnswersServiceFactory();
    }
    
    @Test
    public void testHtmlViewServiceCreation()
    {
        factory.buildService(HtmlViewService.class).getHtmlViewService();
    }
    
    @Test
    public void testIBotServiceCreation()
    {
        factory.buildService(IBotService.class).getIBotServiceSoap();
    }
    
    @Test
    public void testJobManagementServiceCreation()
    {
        factory.buildService(JobManagementService.class).getJobManagementServiceSoap();
    }
    
    @Test
    public void testMetadataServiceCreation()
    {
        factory.buildService(MetadataService.class).getMetadataServiceSoap();
    }
    
    @Test
    public void testReplicationServiceCreation()
    {
        factory.buildService(ReplicationService.class).getReplicationServiceSoap();
    }
    
    @Test
    public void testReportEditingServiceCreation()
    {
        factory.buildService(ReportEditingService.class).getReportEditingServiceSoap();
    }
    
    @Test
    public void testSecurityServiceCreation()
    {
        factory.buildService(SecurityService.class).getSecurityServiceSoap();
    }

    @Test
    public void testSawServiceCreation()
    {
        factory.buildService(SAWSessionService.class).getSAWSessionServiceSoap();
    }
    
    @Test
    public void testWebCatalogServiceCreation()
    {
        factory.buildService(WebCatalogService.class).getWebCatalogServiceSoap();
    }
    
    @Test
    public void testXmlViewServiceCreation()
    {
        factory.buildService(XmlViewService.class).getXmlViewServiceSoap();
    }
}
