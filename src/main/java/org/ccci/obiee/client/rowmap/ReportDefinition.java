package org.ccci.obiee.client.rowmap;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.ccci.obiee.rowmap.annotation.Column;
import org.ccci.obiee.rowmap.annotation.ReportPath;

/**
 * Encapsulates a row type with validation logic and methods to retrieve {@link ReportColumn}s.
 * 
 * @author Matt Drees
 * @param <T>
 */
public class ReportDefinition<T>
{

    private final Class<T> rowType;

    private final Map<String, ReportColumn<T>> columns;
    
    public ReportDefinition(Class<T> rowType)
    {
        validate(rowType);
        columns = buildColumns(rowType);
        this.rowType = rowType;
    }

    private void validate(Class<T> rowType)
    {
        if (!rowType.isAnnotationPresent(ReportPath.class))
        {
            throw new IllegalArgumentException(
                rowType.getName() + " is not a valid OBIEE report row; it is not annotated @" + ReportPath.class.getSimpleName());
        }
    }

    private Map<String, ReportColumn<T>> buildColumns(Class<T> rowType)
    {
        Map<String, ReportColumn<T>> columns = new HashMap<String, ReportColumn<T>>();
        for (Field field : rowType.getDeclaredFields())
        {
            if (field.isAnnotationPresent(Column.class))
            {
                ReportColumn<T> column = new ReportColumn<T>(field, rowType);
                columns.put(column.getName(), column);
            }
        }
        return columns;
    }

    public String getName()
    {
        return rowType.getSimpleName();
    }

    /**
     * @param columnName
     * @return the corresponding {@link ReportColumn}
     * @throws IllegalArgumentException if there is no column with the given name
     */
    public ReportColumn<T> getColumn(String columnName)
    {
        ReportColumn<T> column = columns.get(columnName);
        if (column == null)
        {
            throw new IllegalArgumentException("There is no column named " + columnName);
        }
        return column;
    }

    public Set<ReportColumn<T>> getColumns()
    {
        return new HashSet<ReportColumn<T>>(columns.values());
    }

    public Class<T> getRowType()
    {
        return rowType;
    }

}
