package org.ccci.obiee.client.rowmap;

/**
 * A factory object to construct {@link AnalyticsManager} instances.  There should generally be one {@link AnalyticsManagerFactory} 
 * per application.  It should be thread-safe once constructed and configured.
 * 
 * 
 * @author Matt Drees
 */
public interface AnalyticsManagerFactory
{

    /**
     * Creates a new {@link AnalyticsManager}.  The manager may be used to execute queries.  When finished, the client is
     * responsible for calling {@link AnalyticsManager#close()}.
     * 
     * An AnalyticsManager keeps its own session with Obiee Answers, and doesn't log itself back in if its session times out.  So, these
     * 
     * @return
     */
    AnalyticsManager createAnalyticsManager();

}
