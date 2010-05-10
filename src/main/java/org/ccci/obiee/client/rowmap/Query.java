package org.ccci.obiee.client.rowmap;

import java.util.List;

import org.ccci.obiee.rowmap.annotation.ReportParamVariable;

/**
 * Used to build and execute queries against the OBIEE Answers system.
 * 
 * @author Matt Drees
 *
 * @param <T> the type of rows that will be returned
 */
public interface Query<T>
{

    /**
     * Restricts the results to be returned.  This is semantically equivalent to an SQL 'where' clause.
     * 
     * @param selection should contain a set of fields each annotated {@link ReportParamVariable}, and these fields will
     *            be used in filter clauses.  If there are no fields with this annotation, an exception will be thrown.
     * @return this Query instance to enable convenient method chaining
     */
    public Query<T> withSelection(Object selection);
    
    /**
     * Adds sort parameters to the query.  This is semantically equivalent to an SQL 'order by' clause.
     * @param sortColumn
     * @return this Query instance to enable convenient method chaining
     */
    public Query<T> orderBy(ReportColumn<T> sortColumn, SortDirection direction);

    /**
     * Executes the query and returns the returned row object. This should be invoked when exactly one row in the returned
     * rowset is expected. If there are more than one row in the returned rowset, or if there are zero rows
     * returned, a {@link DataRetrievalException} is thrown with an appropriate message.
     * 
     * @return a row object mapped from the returned rowset
     * @throws DataRetrievalException if there is an error communicating with the Answers system.
     * @throws RowmapConfigurationException if the given rowType is configured incorrectly
     */
    public T getSingleResult();
    
    /**
     * Executes the query and returns a {@link List} of the returned row objects.
     * 
     * @return a List of row objects mapped from the returned rowset
     * @throws DataRetrievalException if there is an error communicating with the Answers system.
     * @throws RowmapConfigurationException if the given rowType is configured incorrectly
     */
    public List<T> getResultList();
    
}
