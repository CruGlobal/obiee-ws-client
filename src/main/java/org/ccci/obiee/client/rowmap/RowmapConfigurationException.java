package org.ccci.obiee.client.rowmap;

/**
 * Indicates a problem in the configuration of a row type.  
 * 
 * @author Matt Drees
 */
public class RowmapConfigurationException extends RuntimeException
{


    public RowmapConfigurationException(String message, Exception cause)
    {
        super(message, cause);
    }
    

    public RowmapConfigurationException(String message)
    {
        super(message);
    }


    private static final long serialVersionUID = 1L;
}
