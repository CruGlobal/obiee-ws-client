package org.ccci.obiee.client.rowmap.impl;

import org.ccci.obiee.client.rowmap.AnalyticsManager;
import org.ccci.obiee.client.rowmap.AnalyticsManagerFactory;

import com.siebel.analytics.web.soap.v5.ReportEditingService;
import com.siebel.analytics.web.soap.v5.ReportEditingServiceSoap;
import com.siebel.analytics.web.soap.v5.SAWSessionService;
import com.siebel.analytics.web.soap.v5.SAWSessionServiceSoap;
import com.siebel.analytics.web.soap.v5.XmlViewService;
import com.siebel.analytics.web.soap.v5.XmlViewServiceSoap;

public class AnalyticsManagerFactoryImpl implements AnalyticsManagerFactory
{

    private final SAWSessionService sawSessionService;
    private final XmlViewService xmlViewService;
    private final ReportEditingService reportEditingService;
    private final String username;
    private final String password;

    public AnalyticsManagerFactoryImpl(SAWSessionService sawSessionService, XmlViewService xmlViewService, ReportEditingService reportEditingService, String username, String password)
    {
        this.sawSessionService = sawSessionService;
        this.xmlViewService = xmlViewService;
        this.reportEditingService = reportEditingService;
        this.username = username;
        this.password = password;
    }
    

    public AnalyticsManager createAnalyticsManager()
    {
        
        SAWSessionServiceSoap sawSessionServiceSoap = sawSessionService.getSAWSessionServiceSoap();
        XmlViewServiceSoap xmlViewServiceSoap = xmlViewService.getXmlViewServiceSoap();
        ReportEditingServiceSoap reportEditingServiceSoap = reportEditingService.getReportEditingServiceSoap();
        
        String sessionId = sawSessionServiceSoap.logon(username, password);
        
        ConverterStore converterStore = ConverterStore.buildDefault();
        return new AnalyticsManagerImpl(sessionId, sawSessionServiceSoap, xmlViewServiceSoap, reportEditingServiceSoap, converterStore);
    }
    
}
