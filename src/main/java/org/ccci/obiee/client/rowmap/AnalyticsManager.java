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
     * @param reportDefinition specifies which OBIEE report to query against
     * @return the new {@code Query} instance
     * @throws RowmapConfigurationException if the given rowType is configured incorrectly
     */
    <T> Query<T> createQuery(ReportDefinition<T> reportDefinition);
    
    /**
     * Closes this manager
     */
    void close();

}
