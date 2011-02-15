package org.ccci.obiee.client.rowmap.impl;

import java.lang.reflect.Field;
import java.util.EnumSet;
import java.util.Set;

import org.ccci.obiee.client.rowmap.Converter;
import org.ccci.obiee.client.rowmap.annotation.ObiFieldValue;

public class EnumConverter<T extends Enum<T>> implements Converter<T>
{

    private final Class<T> enumType;
    private final Set<T> possibleEnumValues;
    private final Field mappingField;
    
    public EnumConverter(Class<T> enumType)
    {
        this.enumType = enumType;
        this.possibleEnumValues = EnumSet.allOf(enumType);
        this.mappingField = getObiValueField();
    }

    private Field getObiValueField()
    {
        Field[] fields = enumType.getDeclaredFields();
        for(Field field : fields)
        {
            if (field.isAnnotationPresent(ObiFieldValue.class))
            {
                if (field.getType() != String.class)
                {
                    throw new IllegalArgumentException(String.format(
                        "field %s is not of type String, yet is annotated @%s",
                        field,
                        ObiFieldValue.class.getSimpleName()
                    ));
                }
                field.setAccessible(true);
                return field;
            }
        }
        throw new IllegalArgumentException(String.format(
            "enum type %s does not have a field annotated @%s",
            enumType,
            ObiFieldValue.class.getSimpleName()
        ));
    }


    @Override
    public T convert(String xmlValue, Field field)
    {
        for (T possibleEnum : possibleEnumValues)
        {
            String code = (String) getFieldValueFromAccessibleField(possibleEnum, mappingField);
            if (code == null)
                throw new IllegalArgumentException(String.format(
                    "siebel value field %s contains null value",
                    mappingField
                ));
            if (code.equals(xmlValue))
                return possibleEnum;
        }
        throw new IllegalArgumentException(String.format(
            "The value returned by OBI (%s) does not correspond to any enum of type %s",
            xmlValue,
            enumType
        ));
    }
    
    Object getFieldValueFromAccessibleField(Object obj, Field field) throws AssertionError
    {
        Object fieldValueObject;
        try
        {
            fieldValueObject = field.get(obj);
        }
        catch (IllegalAccessException e)
        {
            throw new AssertionError("field " + field + " was forced to be accessible");
        }
        return fieldValueObject;
    }


}
