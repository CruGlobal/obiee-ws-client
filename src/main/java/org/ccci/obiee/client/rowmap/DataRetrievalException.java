package org.ccci.obiee.client.rowmap;


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
