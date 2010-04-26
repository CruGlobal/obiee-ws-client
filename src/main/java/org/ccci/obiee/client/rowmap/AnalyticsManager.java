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
     */
    <T> List<T> query(Class<T> class1);

    /**
     * Closes this manager
     */
    void close();

}
