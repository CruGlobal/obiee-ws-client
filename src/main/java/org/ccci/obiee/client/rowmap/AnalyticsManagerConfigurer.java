package org.ccci.obiee.client.rowmap;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import io.opentracing.Tracer;
import org.ccci.obiee.client.init.AnswersServiceFactory;
import org.ccci.obiee.client.rowmap.impl.AnalyticsManagerFactoryImpl;
import org.ccci.obiee.client.rowmap.impl.RowmapConfiguration;

public class AnalyticsManagerConfigurer {

    private final AnalyticsManagerFactory factory;
    private final Tracer tracer;

    public AnalyticsManagerConfigurer(Tracer tracer)
    {
        this.tracer = tracer;
        Properties obieeProperties;
        try
        {
            obieeProperties = loadObieeProperties();
        }
        catch (IOException e)
        {
            throw new RuntimeException("unable to load obiee.properties file", e);
        }
        RowmapConfiguration config = buildRowmapConfiguration(obieeProperties);

        AnswersServiceFactory serviceFactory = new AnswersServiceFactory();

        factory = new AnalyticsManagerFactoryImpl(
            serviceFactory,
            config,
            this.tracer);
    }

    private RowmapConfiguration buildRowmapConfiguration(Properties obieeProperties)
    {
        String username = getRequiredProperty(obieeProperties, "obiee.username");
        String password = getRequiredProperty(obieeProperties, "obiee.password");
        String endpointBaseUrl= getRequiredProperty(obieeProperties, "obiee.endpoint.baseUrl");

        RowmapConfiguration config = new RowmapConfiguration();
        config.setUsername(username);
        config.setPassword(password);
        config.setEndpointBaseUrl(endpointBaseUrl);
        config.setReadTimeout(asInteger(obieeProperties.getProperty("obiee.readTimeout")));
        return config;
    }
    

    private Integer asInteger(String property)
    {
        if (property == null)
            return null;
        else
            return Integer.valueOf(property);
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
