package org.ccci.obiee.client.rowmap;

import java.util.List;

/**
 * The main API point for querying the Analytics data source.
 * 
 * Instances are not thread-safe.
 * 
 * Once closed, methods will throw {@link IllegalStateException}s if called.
 * 
 * @author Matt Drees
 *
 */
public interface AnalyticsManager
{

    /**
     * 
     * @param <T>
     * @param class1
     * @return
     * @throws Exception 
     */
    <T> List<T> query(Class<T> class1) throws Exception;
    
    <T> List<T> query(Class<T> class1, Object reportParams) throws Exception;

    /**
     * Closes this manager
     */
    void close();

}
