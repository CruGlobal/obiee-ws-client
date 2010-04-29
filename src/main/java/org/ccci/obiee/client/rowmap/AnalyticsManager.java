package org.ccci.obiee.client.rowmap;


/**
 * The main API point for querying the Analytics system.
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
     * Creates a {@link Query} object for executing queries against the Analytics system.
     * @param <T> the type of rows that will be returned
     * @param rowType specifies the type of rows that will be returned, and is used to determine which report to query against
     * @return the new {@code Query} instance
     * @throws RowmapConfigurationException if the given rowType is configured incorrectly
     */
    <T> Query<T> createQuery(Class<T> rowType);
    
    /**
     * Closes this manager
     */
    void close();

}
