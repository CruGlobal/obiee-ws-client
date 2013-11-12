package org.ccci.obiee.client;


import org.testng.annotations.Test;

import oracle.bi.web.soap.SAWSessionService;
import oracle.bi.web.soap.SAWSessionServiceSoap;
import oracle.bi.web.soap.XmlViewService;
import oracle.bi.web.soap.XmlViewServiceSoap;
import oracle.bi.web.soap.QueryResults;
import oracle.bi.web.soap.ReportParams;
import oracle.bi.web.soap.ReportRef;
import oracle.bi.web.soap.XMLQueryExecutionOptions;
import oracle.bi.web.soap.XMLQueryOutputFormat;

public class GhostTest
{

    final static String TEST_USER = "ssw.test.obiee.user@ccci.org";
    final static String TEST_PASSWORD = "CcciSswTest12345";
    
    @Test(enabled = true)
    public void testLogonLogoff()
    {
        SAWSessionService sessionServiceEndpoint = new SAWSessionService();
        SAWSessionServiceSoap sessionService = sessionServiceEndpoint.getSAWSessionServiceSoap();
        
        String sessionID = sessionService.logon(TEST_USER, TEST_PASSWORD);
        sessionService.logoff(sessionID);
    }
    
    
    @Test(enabled = true)
    public void testRunQuery()
    {
        XmlViewService xmlViewServiceEndpoint = new XmlViewService();
        XmlViewServiceSoap xmlViewService = xmlViewServiceEndpoint.getXmlViewServiceSoap();

        SAWSessionService sessionServiceEndpoint = new SAWSessionService();
        SAWSessionServiceSoap sessionService = sessionServiceEndpoint.getSAWSessionServiceSoap();
        

        String sessionID = sessionService.logon(TEST_USER, TEST_PASSWORD);
        
        ReportRef report = new ReportRef();
        report.setReportPath("/shared/CCCi/SSW/SAI Donations");
        
        XMLQueryOutputFormat outputFormat = XMLQueryOutputFormat.SAW_ROWSET_SCHEMA_AND_DATA;
        XMLQueryExecutionOptions executionOptions = new XMLQueryExecutionOptions();
        executionOptions.setMaxRowsPerPage(-1);
        executionOptions.setPresentationInfo(true);
        
        ReportParams reportParams = new ReportParams();
        
        QueryResults queryResults = xmlViewService.executeXMLQuery(
            report, 
            outputFormat, 
            executionOptions, 
            reportParams, 
            sessionID);
        
        String rowset = queryResults.getRowset();

        System.out.println(rowset);
        sessionService.logoff(sessionID);
    }
    
    
    
}
