package org.ccci.obiee.client.rowmap.impl;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;
import org.apache.log4j.Logger;
import org.ccci.obiee.client.init.AnswersServiceFactory;
import org.ccci.obiee.client.rowmap.AnalyticsManager;
import org.ccci.obiee.client.rowmap.AnalyticsManagerFactory;
import org.ccci.obiee.client.rowmap.AnswersConnectionException;

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
    private final int readTimeout;
    private static final int DEFAULT_READ_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(30);
    
    /** connect timeout in ms.  Default is 4 seconds. */
    private final int connectTimeout;
    private static final int DEFAULT_CONNECT_TIMEOUT = (int) TimeUnit.SECONDS.toMillis(4);
    
    
    private String endpointBaseUrl;
    private Logger log = Logger.getLogger(getClass());
    
    public AnalyticsManagerFactoryImpl(SAWSessionService sawSessionService, XmlViewService xmlViewService, ReportEditingService reportEditingService, String username, String password)
    {
        this.sawSessionService = sawSessionService;
        this.xmlViewService = xmlViewService;
        this.reportEditingService = reportEditingService;
        this.username = username;
        this.password = password;
        this.readTimeout = DEFAULT_CONNECT_TIMEOUT;
        this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    }
    
    public AnalyticsManagerFactoryImpl(AnswersServiceFactory serviceFactory, RowmapConfiguration config)
    {
        this.sawSessionService = serviceFactory.buildService(SAWSessionService.class);
        this.xmlViewService = serviceFactory.buildService(XmlViewService.class);
        this.reportEditingService = serviceFactory.buildService(ReportEditingService.class);
        this.username = config.getUsername();
        this.password = config.getPassword();
        this.endpointBaseUrl = config.getEndpointBaseUrl();
        this.readTimeout = config.getReadTimeout() == null ? DEFAULT_READ_TIMEOUT : config.getReadTimeout();
        this.connectTimeout = config.getConnectTimeout() == null ? DEFAULT_CONNECT_TIMEOUT : config.getConnectTimeout();
    }

    public AnalyticsManager createAnalyticsManager()
    {
        SAWSessionServiceSoap sawSessionServiceSoap = sawSessionService.getSAWSessionServiceSoap();
        configurePort(sawSessionServiceSoap);
        
        XmlViewServiceSoap xmlViewServiceSoap = xmlViewService.getXmlViewServiceSoap();
        configurePort(xmlViewServiceSoap);
        
        ReportEditingServiceSoap reportEditingServiceSoap = reportEditingService.getReportEditingServiceSoap();
        configurePort(reportEditingServiceSoap);
        
        String sessionId;
        try
        {
            sessionId = sawSessionServiceSoap.logon(username, password);
        }
        catch (SOAPFaultException e)
        {
            throw new AnswersConnectionException(username, e);
        }
        log.debug("created Answers session " + sessionId);
        
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
        bindingProvider.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        setEndpointAddressIfNecessary(bindingProvider);

        setSunJaxWsProperties(bindingProvider);
        
        setCxfProperties(port);
    }

    private void setSunJaxWsProperties(BindingProvider bindingProvider)
    {
        bindingProvider.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, connectTimeout);
        bindingProvider.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, readTimeout);
    }

    private void setCxfProperties(Object port)
    {
        if (CxfConfiguration.cxfIsPresent && CxfConfiguration.configurator.isCxfPort(port))
        {
            CxfConfiguration.configurator.setNoChunkedEncoding(port);
            CxfConfiguration.configurator.setConnectTimeout(port, connectTimeout);
            CxfConfiguration.configurator.setReadTimeout(port, readTimeout);
        }
    }

    
    static class CxfConfiguration
    {
        static final boolean cxfIsPresent;
        static {
            cxfIsPresent = checkCxfClientPresent();
            if (cxfIsPresent) 
                configurator = new CxfConfigurator();
        }
        
        static CxfConfigurator configurator;

        private static boolean checkCxfClientPresent()
        {
            try
            {
                Class.forName("org.apache.cxf.frontend.ClientProxy", false, Thread.currentThread().getContextClassLoader());
                return true;
            }
            catch (ClassNotFoundException e)
            {
                return false;
            }
        }
    }
    
    static class CxfConfigurator
    {

        public boolean isCxfPort(Object port)
        {
            return getClientPolicy(port) != null;
        }
        
        /*
         * Chunked transfer encoding is not handled, for some reason, by Answer's soap service
         */
        public void setNoChunkedEncoding(Object port)
        {
            HTTPClientPolicy clientPolicy = getClientPolicy(port);
            clientPolicy.setAllowChunking(false);
        }

        public void setReadTimeout(Object port, int readTimeout)
        {
            HTTPClientPolicy clientPolicy = getClientPolicy(port);
            clientPolicy.setReceiveTimeout(readTimeout);
        }

        public void setConnectTimeout(Object port, int connectTimeout)
        {
            HTTPClientPolicy clientPolicy = getClientPolicy(port);
            clientPolicy.setConnectionTimeout(connectTimeout);
        }

        /**
         * returns the {@link HTTPClientPolicy} associate with this port if it's
         * a CXF port; otherwise; returns null.
         */
        private HTTPClientPolicy getClientPolicy(Object port)
        {
            Client client;
            try
            {
                client = ClientProxy.getClient(port);
            }
            catch (ClassCastException e)
            {
                return null;
            }
            
            HTTPConduit conduit = (HTTPConduit) client.getConduit();
            HTTPClientPolicy clientPolicy = conduit.getClient();
            if (clientPolicy == null)
                clientPolicy = new HTTPClientPolicy();
            conduit.setClient(clientPolicy);
            return clientPolicy;
        }
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

    public int getConnectTimeout()
    {
        return connectTimeout;
    }

}
