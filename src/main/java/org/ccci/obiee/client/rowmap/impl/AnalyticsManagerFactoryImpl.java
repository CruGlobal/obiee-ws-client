package org.ccci.obiee.client.rowmap.impl;

import io.opentracing.Scope;
import io.opentracing.Span;
import io.opentracing.Tracer;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.ws.BindingProvider;
import javax.xml.ws.soap.SOAPFaultException;

import org.ccci.obiee.client.init.AnswersServiceFactory;
import org.ccci.obiee.client.rowmap.AnalyticsManager;
import org.ccci.obiee.client.rowmap.AnalyticsManagerFactory;
import org.ccci.obiee.client.rowmap.AnswersConnectionException;

import oracle.bi.web.soap.ReportEditingService;
import oracle.bi.web.soap.ReportEditingServiceSoap;
import oracle.bi.web.soap.SAWSessionService;
import oracle.bi.web.soap.SAWSessionServiceSoap;
import oracle.bi.web.soap.XmlViewService;
import oracle.bi.web.soap.XmlViewServiceSoap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.ccci.obiee.client.rowmap.impl.Tracing.buildTopLevelSpan;

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
    private Logger log = LoggerFactory.getLogger(getClass());
    private Tracer tracer;

    public AnalyticsManagerFactoryImpl(
        SAWSessionService sawSessionService,
        XmlViewService xmlViewService,
        ReportEditingService reportEditingService,
        String username,
        String password,
        Tracer tracer)
    {
        this.sawSessionService = sawSessionService;
        this.xmlViewService = xmlViewService;
        this.reportEditingService = reportEditingService;
        this.username = username;
        this.password = password;
        this.tracer = tracer;
        this.readTimeout = DEFAULT_CONNECT_TIMEOUT;
        this.connectTimeout = DEFAULT_CONNECT_TIMEOUT;
    }
    
    public AnalyticsManagerFactoryImpl(
        AnswersServiceFactory serviceFactory,
        RowmapConfiguration config,
        Tracer tracer)
    {
        this.sawSessionService = serviceFactory.buildService(SAWSessionService.class);
        this.xmlViewService = serviceFactory.buildService(XmlViewService.class);
        this.reportEditingService = serviceFactory.buildService(ReportEditingService.class);
        this.username = config.getUsername();
        this.password = config.getPassword();
        this.endpointBaseUrl = config.getEndpointBaseUrl();
        this.readTimeout = config.getReadTimeout() == null ? DEFAULT_READ_TIMEOUT : config.getReadTimeout();
        this.connectTimeout = config.getConnectTimeout() == null ? DEFAULT_CONNECT_TIMEOUT : config.getConnectTimeout();
        this.tracer = tracer;
    }

    public AnalyticsManager createAnalyticsManager()
    {
        final Span span = buildTopLevelSpan(tracer, "create-analytics-manager");
        try (Scope ignored = tracer.scopeManager().activate(span, false)) {
            SAWSessionServiceSoap sawSessionServiceSoap = sawSessionService.getSAWSessionServiceSoap();
            configurePort(sawSessionServiceSoap);

            XmlViewServiceSoap xmlViewServiceSoap = xmlViewService.getXmlViewServiceSoap();
            configurePort(xmlViewServiceSoap);

            ReportEditingServiceSoap reportEditingServiceSoap = reportEditingService.getReportEditingServiceSoap();
            configurePort(reportEditingServiceSoap);

            String sessionId = logon(sawSessionServiceSoap);

            ConverterStore converterStore = ConverterStore.buildDefault();
            return new AnalyticsManagerImpl(
                sessionId,
                sawSessionServiceSoap,
                xmlViewServiceSoap,
                reportEditingServiceSoap,
                converterStore,
                tracer
            );
        } finally {
            span.finish();
        }
    }

    private String logon(SAWSessionServiceSoap sawSessionServiceSoap) {
        final Span span = tracer.buildSpan("logon").start();
        try (Scope ignored = tracer.scopeManager().activate(span, false)) {
            String sessionId = sawSessionServiceSoap.logon(username, password);
            log.debug("created Answers session " + sessionId);
            return sessionId;
        } catch (SOAPFaultException e) {
            throw new AnswersConnectionException(username, e);
        } finally {
            span.finish();
        }
    }

    private void configurePort(Object port)
    {
        BindingProvider bindingProvider = (BindingProvider) port;
        bindingProvider.getRequestContext().put(BindingProvider.SESSION_MAINTAIN_PROPERTY, true);
        setEndpointAddressIfNecessary(bindingProvider);

        PortConfigurer portConfigurer = new PortConfigurer(bindingProvider);
        portConfigurer.setDefaults();
        portConfigurer.setTimeouts(connectTimeout, readTimeout);
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
