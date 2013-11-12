package org.ccci.obiee.client.init;

import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import oracle.bi.web.soap.HtmlViewService;
import oracle.bi.web.soap.IBotService;
import oracle.bi.web.soap.JobManagementService;
import oracle.bi.web.soap.MetadataService;
import oracle.bi.web.soap.ReplicationService;
import oracle.bi.web.soap.ReportEditingService;
import oracle.bi.web.soap.SAWSessionService;
import oracle.bi.web.soap.SecurityService;
import oracle.bi.web.soap.WebCatalogService;
import oracle.bi.web.soap.XmlViewService;

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
