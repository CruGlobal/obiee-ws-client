package org.ccci.obiee.client.rowmap;

/**
 * Indicates whether results ought to be sorted ascending or descending.
 * 
 * @author Matt Drees
 */
public enum SortDirection 
{
    ASCENDING("asc"),
    DESCENDING("desc");

    String code;
    
    private SortDirection(String code)
    {
        this.code = code;
    }

    public String toCode()
    {
        return code;
    }
    
    public static SortDirection fromCode(String code)
    {
        if (code.equals("asc")) return ASCENDING;
        if (code.equals("desc")) return DESCENDING;
        throw new IllegalArgumentException("No such code: " + code);
    }

    public SortDirection opposite()
    {
        switch (this)
        {
            case ASCENDING:
                return DESCENDING;
            case DESCENDING:
                return ASCENDING;
            default:
                throw new AssertionError();
        }
    }
}
