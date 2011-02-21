package org.ccci.obiee.client.rowmap.impl;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Properties;

import javax.xml.namespace.QName;

import org.ccci.obiee.client.rowmap.AnalyticsManagerFactory;
import com.siebel.analytics.web.soap.v5.ReportEditingService;
import com.siebel.analytics.web.soap.v5.SAWSessionService;
import com.siebel.analytics.web.soap.v5.XmlViewService;

public class AnalyticsManagerConfigurer {
	
	private final AnalyticsManagerFactory factory;
    
    private static final QName sawServiceQName = new QName("com.siebel.analytics.web/soap/v5", "SAWSessionService");
    private static final QName viewServiceQName = new QName("com.siebel.analytics.web/soap/v5", "XmlViewService");
    private static final QName reportEditingServiceQName = new QName("com.siebel.analytics.web/soap/v5", "ReportEditingService");	
	
    public AnalyticsManagerConfigurer()
    {
    	Properties obieeProperties;
    	try
    	{
    		obieeProperties = loadObieeProperties();
    	}
    	catch (IOException e)
    	{
    		throw new RuntimeException("unable to load obiee.properties file", e);
    	}
    	String username = getRequiredProperty(obieeProperties, "obiee.username");
    	String password = getRequiredProperty(obieeProperties, "obiee.password");
    	String wsdlUrlAsString = getRequiredProperty(obieeProperties, "obiee.wsdl.url");

    	URL wsdlUrl;
    	try
    	{
    		wsdlUrl = new URL(wsdlUrlAsString);
    	}
    	catch (MalformedURLException e)
    	{
    		throw new IllegalArgumentException("invalid obiee wsdl url: " + wsdlUrlAsString);
    	}

    	factory = new AnalyticsManagerFactoryImpl(
    			new SAWSessionService(wsdlUrl, sawServiceQName), 
    			new XmlViewService(wsdlUrl, viewServiceQName), 
    			new ReportEditingService(wsdlUrl, reportEditingServiceQName),
    			username, 
    			password);
    }
    
    private String getRequiredProperty(Properties obieeProperties, String property)
    {
        String value = obieeProperties.getProperty(property);
        if (value == null)
            throw new IllegalArgumentException("obiee.properties file does not specifiy required property '" + property + "'");
        return value;
    }
    
    private Properties loadObieeProperties() throws IOException
    {
        Properties properties = new Properties();
        
        ClassLoader classLoader = this.getClass().getClassLoader();
        InputStream propertiesStream = classLoader.getResourceAsStream("obiee.properties");
        if (propertiesStream == null)
        {
            throw new IllegalStateException("Can't find obiee.properties file on " + classLoader + "'s resource path");
        }
        try
        {
            properties.load(propertiesStream);
            return properties;
        }
        finally
        {
            propertiesStream.close();
        }
    }

	public AnalyticsManagerFactory getAMFactory() {
		return factory;
	}
}
