package org.ccci.obiee.client.rowmap.impl;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.BindingProvider;

import org.ccci.obiee.client.init.AnswersServiceFactory;
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
    private String endpointBaseUrl;
    
    public AnalyticsManagerFactoryImpl(SAWSessionService sawSessionService, XmlViewService xmlViewService, ReportEditingService reportEditingService, String username, String password)
    {
        this.sawSessionService = sawSessionService;
        this.xmlViewService = xmlViewService;
        this.reportEditingService = reportEditingService;
        this.username = username;
        this.password = password;
    }
    
    public AnalyticsManagerFactoryImpl(AnswersServiceFactory serviceFactory, RowmapConfiguration config)
    {
        this.sawSessionService = serviceFactory.buildService(SAWSessionService.class);
        this.xmlViewService = serviceFactory.buildService(XmlViewService.class);
        this.reportEditingService = serviceFactory.buildService(ReportEditingService.class);
        this.username = config.getUsername();
        this.password = config.getPassword();
        this.endpointBaseUrl = config.getEndpointBaseUrl();
    }

    public AnalyticsManager createAnalyticsManager()
    {
        SAWSessionServiceSoap sawSessionServiceSoap = sawSessionService.getSAWSessionServiceSoap();
        configurePort(sawSessionServiceSoap);
        
        XmlViewServiceSoap xmlViewServiceSoap = xmlViewService.getXmlViewServiceSoap();
        configurePort(xmlViewServiceSoap);
        
        ReportEditingServiceSoap reportEditingServiceSoap = reportEditingService.getReportEditingServiceSoap();
        configurePort(reportEditingServiceSoap);
        
        String sessionId = sawSessionServiceSoap.logon(username, password);
        
        ConverterStore converterStore = ConverterStore.buildDefault();
        return new AnalyticsManagerImpl(
            sessionId,
            sawSessionServiceSoap, 
            xmlViewServiceSoap, 
            reportEditingServiceSoap, 
            converterStore);
    }

    private void configurePort(Object port)
    {
        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, connectTimeout);
        bindingProvider.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, readTimeout);
        bindingProvider.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        
        setEndpointAddressIfNecessary(bindingProvider);
    }

    private void setEndpointAddressIfNecessary(BindingProvider bindingProvider) throws AssertionError
    {
        String defaultEndpointAddress = (String) bindingProvider.getRequestContext().get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY);
        if (!defaultEndpointAddress.startsWith(endpointBaseUrl))
        {
            String newEndpointAddress = buildNewEndpointAddress(defaultEndpointAddress);
            bindingProvider.getRequestContext().put(BindingProvider.ENDPOINT_ADDRESS_PROPERTY, newEndpointAddress);
        }
    }

    String buildNewEndpointAddress(String defaultEndpointAddress) throws AssertionError
    {
        Matcher matcher = Pattern.compile("^https?://[^/]*/(.*)").matcher(defaultEndpointAddress);
        boolean matches = matcher.matches();
        if (!matches) throw new AssertionError("Can't find endpoint url suffix in " + defaultEndpointAddress);
        String endpointUrlSuffix = matcher.group(1);
        return endpointBaseUrl + "/" + endpointUrlSuffix;
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
