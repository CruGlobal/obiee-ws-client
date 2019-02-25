package org.ccci.obiee.client.rowmap;

import java.lang.reflect.Field;

/**
 * Represents a 'column' of a report; it maps one-to-one with a <code>@Column</code>-annotated field
 * in a row type.
 * 
 * See {@link ReportDefinition#getColumn(String)}.
 * 
 * @author Matt Drees
 * @param <T> indicates the row type of the associated report
 */
public class ReportColumn<T>
{

    private final Field field;
    private final Class<T> rowType;

    ReportColumn(Field field, Class<T> rowType)
    {
        field.setAccessible(true);
        this.field = field;
        this.rowType = rowType;
    }

    public Field getField()
    {
        return field;
    }

    public String getName()
    {
        return field.getName();
    }

    public Object getValue(T row)
    {
        if (row == null)
            throw new NullPointerException("row is null");
        if (! rowType.isInstance(row))
            throw new IllegalArgumentException(String.format(
                "row %s (of type %s) is not an instance of %s",
                row,
                row.getClass(),
                rowType
            ));
        try
        {
            return field.get(row);
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError("field should be accessible", e);
        }
    }
    
    @Override
    public String toString()
    {
        return "ReportColumn[" + getName() + "]";
    }
}
