package org.ccci.obiee.client.rowmap.impl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

import javax.xml.xpath.XPathExpression;
import javax.xml.xpath.XPathExpressionException;

import org.ccci.obiee.client.rowmap.Converter;
import org.ccci.obiee.client.rowmap.DataRetrievalException;
import org.ccci.obiee.client.rowmap.RowmapConfigurationException;
import org.ccci.obiee.client.rowmap.annotation.Column;
import org.w3c.dom.Node;

class RowBuilder<T>
{
    final Map<ReportColumnId, XPathExpression> columnToValueExpressionMapping;
    
    final Map<ReportColumnId, Field> columnToFieldMapping = new HashMap<ReportColumnId, Field>();
    private final ConverterStore converterStore;
    private final Constructor<T> rowConstructor;
    
    public RowBuilder(Map<ReportColumnId, XPathExpression> columnToValueExpressionMapping, Class<T> rowType, ConverterStore converterStore)
    {
        this.columnToValueExpressionMapping = columnToValueExpressionMapping;
        this.converterStore = converterStore;
        
        rowConstructor = getRowConstructor(rowType);
        Class<?> clazz = rowType;
        while(!clazz.equals(Object.class))
        {
            for (Field field : clazz.getDeclaredFields())
            {
                if (field.isAnnotationPresent(Column.class))
                {
                    ReportColumnId columnId = ReportColumnId.buildColumnId(field);
                    if (columnToFieldMapping.containsKey(columnId))
                    {
                        throw new RowmapConfigurationException(String.format(
                            "two fields are mapped to the same report column: %s, and %s",
                            field,
                            columnToFieldMapping.get(columnId)));
                    }
                    
                    checkFieldHasCorrespondingReportColumn(rowType, field, columnId);
                    checkFieldTypeIsSupported(converterStore, field);
                    ensureFieldAccessible(field);
                    columnToFieldMapping.put(columnId, field);
                }
            }
            clazz = clazz.getSuperclass();
        }
        
    }

    private Constructor<T> getRowConstructor(Class<T> rowType)
    {
        Constructor<T> constructor;
        try
        {
            constructor = rowType.getConstructor();
        }
        catch (NoSuchMethodException e)
        {
            throw new RowmapConfigurationException(String.format(
                "Invalid row type; %s does not have a no-arg constructor",
                rowType
            ), e);
        }
        constructor.setAccessible(true);
        return constructor;
    }

    private void ensureFieldAccessible(Field field)
    {
        if (!field.isAccessible())
        {
            field.setAccessible(true);
        }
    }

    private void checkFieldTypeIsSupported(ConverterStore converterStore, Field field)
    {
        if (converterStore.getConverter(field.getType()) == null)
        {
            throw new RowmapConfigurationException(String.format(
                "%s is not of a type that this system can use (%s).  If you need to convert to this type, add an appropriate converter",
                field,
                field.getType()));
        }
    }

    private void checkFieldHasCorrespondingReportColumn(Class<T> rowType, Field field, ReportColumnId columnId)
    {
        if (!this.columnToValueExpressionMapping.containsKey(columnId))
        {
            throw new DataRetrievalException(
                String.format(
                    "the schema returned from OBIEE does not match the given row type.  " +
                    "There is no report column named '%s', as required by %s.%s", 
                    columnId,
                    rowType.getName(),
                    field.getName()));
        }
    }

    public T buildRowInstance(Node rowNode)
    {
        try
        {
            T rowInstance = buildRowInstance();
            for (ReportColumnId columnId : columnToFieldMapping.keySet())
            {
                String value = getXmlValue(rowNode, columnId);
                Field field = columnToFieldMapping.get(columnId);
                Object converted = convert(value, field);
                updateModel(rowInstance, field, converted);
            }
            return rowInstance;
        }
        catch (Exception e)
        {
            throw new DataRetrievalException("unable to parse row: " + rowNode, e);
        }
    }

    private void updateModel(T rowInstance, Field field, Object converted) throws AssertionError
    {
        try
        {
            field.set(rowInstance, converted);
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError("field.setAccessible(true) should have been called by now");
        }
    }

    private String getXmlValue(Node rowNode, ReportColumnId columnId)
    {
        String value;
        try
        {
            value = columnToValueExpressionMapping.get(columnId).evaluate(rowNode);
        }
        catch (XPathExpressionException e)
        {
            throw new DataRetrievalException(String.format(
                "unable to extract textual value from column %s",
                columnId), e);
        }
        return value;
    }

    private T buildRowInstance()
    {
        T rowInstance;
        try
        {
            rowInstance = rowConstructor.newInstance();
        }
        catch (Exception e)
        {
            throw new DataRetrievalException("can't create a new row instance of type " + rowConstructor.getDeclaringClass(), e);
        }
        return rowInstance;
    }

    private Object convert(String value, Field field)
    {
        Converter<?> converter = converterStore.getConverter(field.getType());
        Object converted;
        try
        {
            converted = converter.convert(value, field);
        }
        catch (RuntimeException e)
        {
            throw new DataRetrievalException("unable to parse; " + value, e);
        }

        if (converted != null && !field.getType().isInstance(converted)
                || converted == null && field.getType().isPrimitive())
        {
            throw new RowmapConfigurationException(String.format(
                "converter %s returned %s%s, which is not assignable to %s, which is of type %s", 
                converter, 
                converted,
                converted == null ? "" : ", of type " + converted.getClass().getName(),
                field,
                field.getType()
                ));
        }
        return converted;
    }
    
}