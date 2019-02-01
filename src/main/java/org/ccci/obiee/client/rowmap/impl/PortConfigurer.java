package org.ccci.obiee.client.rowmap.impl;

import javax.xml.ws.BindingProvider;

import org.apache.cxf.endpoint.Client;
import org.apache.cxf.frontend.ClientProxy;
import org.apache.cxf.transport.http.HTTPConduit;
import org.apache.cxf.transports.http.configuration.HTTPClientPolicy;

import com.sun.xml.ws.client.BindingProviderProperties;

public class PortConfigurer
{

    BindingProvider port;
    
    public PortConfigurer(BindingProvider port)
    {
        this.port = port;
    }

    public void setDefaults()
    {
        setCxfNoChunkedEncoding();
    }
    
    public void setTimeouts(int connectTimeout, int readTimeout)
    {
        setSunJaxWsTimeouts(connectTimeout, readTimeout);
        setSunJdkTimeouts(connectTimeout, readTimeout);
        setCxfTimeouts(connectTimeout, readTimeout);
    }
    
    public void setReadTimeout(int readTimeout)
    {
        setSunJaxWsReadTimeout(readTimeout);
        setCxfReadTimeout(readTimeout);
    }

    private void setCxfTimeouts(int connectTimeout, int readTimeout)
    {
        setCxfConnectTimeout(connectTimeout);
        setCxfReadTimeout(readTimeout);
    }

    private void setSunJaxWsTimeouts(int connectTimeout, int readTimeout)
    {
        setSunJaxWsConnectTimeout(connectTimeout);
        setSunJaxWsReadTimeout(readTimeout);
    }

    private void setSunJaxWsReadTimeout(int readTimeout)
    {
        port.getRequestContext().put(BindingProviderProperties.REQUEST_TIMEOUT, readTimeout);
    }

    private void setSunJaxWsConnectTimeout(int connectTimeout)
    {
        port.getRequestContext().put(BindingProviderProperties.CONNECT_TIMEOUT, connectTimeout);
    }

    private void setSunJdkTimeouts(int connectTimeout, int readTimeout)
    {
        setSunJdkConnectTimeout(connectTimeout);
        setSunJdkReadTimeout(readTimeout);
    }

    private void setSunJdkReadTimeout(int readTimeout)
    {
        port.getRequestContext().put("com.sun.xml.internal.ws.request.timeout", readTimeout);
    }

    private void setSunJdkConnectTimeout(int connectTimeout)
    {
        port.getRequestContext().put("com.sun.xml.internal.ws.connect.timeout", connectTimeout);
    }

    private void setCxfReadTimeout(int readTimeout)
    {
        if (CxfConfiguration.cxfIsPresent && CxfConfiguration.configurator.isCxfPort(port))
        {
            CxfConfiguration.configurator.setReadTimeout(port, readTimeout);
        }
    }

    private void setCxfConnectTimeout(int connectTimeout)
    {
        if (CxfConfiguration.cxfIsPresent && CxfConfiguration.configurator.isCxfPort(port))
        {
            CxfConfiguration.configurator.setConnectTimeout(port, connectTimeout);
        }
    }

    private void setCxfNoChunkedEncoding()
    {
        if (CxfConfiguration.cxfIsPresent && CxfConfiguration.configurator.isCxfPort(port))
        {
            CxfConfiguration.configurator.setNoChunkedEncoding(port);
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
    


}
