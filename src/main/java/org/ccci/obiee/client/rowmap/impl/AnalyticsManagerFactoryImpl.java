package org.ccci.obiee.client.rowmap.impl;

import java.util.concurrent.TimeUnit;

import javax.xml.ws.BindingProvider;

import org.ccci.obiee.client.rowmap.AnalyticsManager;
import org.ccci.obiee.client.rowmap.AnalyticsManagerFactory;

import com.siebel.analytics.web.soap.v5.ReportEditingService;
import com.siebel.analytics.web.soap.v5.ReportEditingServiceSoap;
import com.siebel.analytics.web.soap.v5.SAWSessionService;
import com.siebel.analytics.web.soap.v5.SAWSessionServiceSoap;
import com.siebel.analytics.web.soap.v5.XmlViewService;
import com.siebel.analytics.web.soap.v5.XmlViewServiceSoap;
import com.sun.xml.ws.client.BindingProviderProperties;

public class AnalyticsManagerFactoryImpl implements AnalyticsManagerFactory
{

    private final SAWSessionService sawSessionService;
    private final XmlViewService xmlViewService;
    private final ReportEditingService reportEditingService;
    private final String username;
    private final String password;

    /** read timeout in ms.  Default is 30 seconds. */
    private volatile int readTimeout = (int) TimeUnit.SECONDS.toMillis(30);
    
    /** connect timeout in ms.  Default is 4 seconds. */
    private volatile int connectTimeout = (int) TimeUnit.SECONDS.toMillis(4);
    
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
        setTimeouts(sawSessionServiceSoap);
        
        XmlViewServiceSoap xmlViewServiceSoap = xmlViewService.getXmlViewServiceSoap();
        setTimeouts(xmlViewServiceSoap);
        
        ReportEditingServiceSoap reportEditingServiceSoap = reportEditingService.getReportEditingServiceSoap();
        setTimeouts(reportEditingServiceSoap);
        
        String sessionId = sawSessionServiceSoap.logon(username, password);
        
        ConverterStore converterStore = ConverterStore.buildDefault();
        return new AnalyticsManagerImpl(
            sessionId, 
            sawSessionServiceSoap, 
            xmlViewServiceSoap, 
            reportEditingServiceSoap, 
            converterStore);
    }

    private void setTimeouts(Object port)
    {
        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, connectTimeout);
        bindingProvider.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, readTimeout);
    }


    public int getReadTimeout()
    {
        return readTimeout;
    }

    public void setReadTimeout(int readTimeout)
    {
        this.readTimeout = readTimeout;
    }

    public int getConnectTimeout()
    {
        return connectTimeout;
    }

    public void setConnectTimeout(int connectTimeout)
    {
        this.connectTimeout = connectTimeout;
    }
    
}
