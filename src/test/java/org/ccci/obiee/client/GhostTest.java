package org.ccci.obiee.client;


import org.testng.annotations.Test;

import com.siebel.analytics.web.soap.v5.SAWSessionService;
import com.siebel.analytics.web.soap.v5.SAWSessionServiceSoap;
import com.siebel.analytics.web.soap.v5.XmlViewService;
import com.siebel.analytics.web.soap.v5.XmlViewServiceSoap;
import com.siebel.analytics.web.soap.v5.model.QueryResults;
import com.siebel.analytics.web.soap.v5.model.ReportParams;
import com.siebel.analytics.web.soap.v5.model.ReportRef;
import com.siebel.analytics.web.soap.v5.model.XMLQueryExecutionOptions;
import com.siebel.analytics.web.soap.v5.model.XMLQueryOutputFormat;

public class GhostTest
{

    static
    {
        //bypass the LH proxy server, which sometimes can't connect to the server under test
        System.setProperty("http.nonProxyHosts", "hart-*|harta|*.ccci.org");
    }
    
    final static String TEST_USER = "***";
    final static String TEST_PASSWORD= "***";
    
//    @Test
    public void testLogonLogoff()
    {
        SAWSessionService sessionServiceEndpoint = new SAWSessionService();
        SAWSessionServiceSoap sessionService = sessionServiceEndpoint.getSAWSessionServiceSoap();
        
        String sessionID = sessionService.logon(TEST_USER, TEST_PASSWORD);
        sessionService.logoff(sessionID);
    }
    
    
    
    
    @Test
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
