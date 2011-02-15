package org.ccci.obiee.client.rowmap;

/**
 * Thrown when there is a problem retrieving data from the Answers server.  This could be due to an underlying network problem,
 * an authentication problem, a timeout problem, an unexpected response, etc. 
 * 
 * @author Matt Drees
 */
public class DataRetrievalException extends RuntimeException
{

    public DataRetrievalException(String message, Exception cause)
    {
        super(message, cause);
    }

    public DataRetrievalException(String message)
    {
        super(message);
    }

    private static final long serialVersionUID = 1L;
}
