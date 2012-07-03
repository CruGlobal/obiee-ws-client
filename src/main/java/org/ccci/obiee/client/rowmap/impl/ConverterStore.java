package org.ccci.obiee.client.rowmap.impl;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

import org.ccci.obiee.client.rowmap.Converter;
import org.ccci.obiee.client.rowmap.annotation.Scale;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class ConverterStore
{

    private final Map<Class<?>, Converter<?>> converters = new HashMap<Class<?>, Converter<?>>();
    
    
    /**
     * Returns the an appropriate converter for the given type, if the given type is known;
     * otherwise {@code null} is returned.
     * 
     * @param <T> The type of objects that are created by the returned converter.
     */
    public <T> Converter<T> getConverter(Class<T> fieldType)
    {
        @SuppressWarnings("unchecked") //The compiler checked the types match in addConverter()
        Converter<T> converter = (Converter<T>) converters.get(fieldType);
        return converter;
    }
    
    /**
     * Should only be called during initially construction
     */
    public <T> void addConverter(Class<T> fieldType, Converter<T> converter)
    {
        converters.put(fieldType, converter);
    }
    

    /**
     * Returns a copy of this converter store, with the addition of the given converters
     */
    public ConverterStore copyAndAdd(ConverterStore additionalConverters)
    {
        ConverterStore copy = new ConverterStore();
        copy.converters.putAll(this.converters);
        copy.converters.putAll(additionalConverters.converters);
        return copy;
    }
    
    public static ConverterStore buildDefault()
    {
        ConverterStore converterStore = new ConverterStore();
        converterStore.addConverter(String.class, new Converter<String>()
            {
                public String convert(String xmlValue, Field field)
                {
                    return xmlValue;
                }
            });
        
        Converter<Integer> integerConverter = new Converter<Integer>()
        {
            public Integer convert(String xmlValue, Field field)
            {
                if (empty(xmlValue)) return null;
                return Integer.valueOf(xmlValue);
            }
        };
        converterStore.addConverter(Integer.class, integerConverter);
        converterStore.addConverter(Integer.TYPE, integerConverter);
        
        Converter<Long> longConverter = new Converter<Long>()
        {
            public Long convert(String xmlValue, Field field)
            {
                if (empty(xmlValue)) return null;
                return Long.valueOf(xmlValue);
            }
        };
        converterStore.addConverter(Long.class, longConverter);
        converterStore.addConverter(Long.TYPE, longConverter);
        
        Converter<Double> doubleConverter = new Converter<Double>()
        {
            public Double convert(String xmlValue, Field field)
            {
                if (empty(xmlValue)) return null;
                return Double.valueOf(xmlValue);
            }
        };
        converterStore.addConverter(Double.class, doubleConverter);
        converterStore.addConverter(Double.TYPE, doubleConverter);
        
        converterStore.addConverter(LocalDate.class, new Converter<LocalDate>()
            {
                DateTimeFormatter isoFormatter = ISODateTimeFormat.date();
                
                public LocalDate convert(String xmlValue, Field field)
                {
                    if (empty(xmlValue)) return null;
                    DateTime parsedDateTime = isoFormatter.parseDateTime(xmlValue);
                    return parsedDateTime.toLocalDate();
                }
            });
        
        converterStore.addConverter(DateTime.class, new Converter<DateTime>()
            {
                DateTimeFormatter isoFormatter = ISODateTimeFormat.dateHourMinuteSecond();
                
                public DateTime convert(String xmlValue, Field field)
                {
                    if (empty(xmlValue)) return null;
                    return isoFormatter.parseDateTime(xmlValue);
                }
            });
        
        converterStore.addConverter(BigDecimal.class, new Converter<BigDecimal>()
            {
                
                public BigDecimal convert(String xmlValue, Field field)
                {
                    if (empty(xmlValue)) return null;
                    
                    BigDecimal parsed = new BigDecimal(xmlValue);
                    
                    if (field.isAnnotationPresent(Scale.class))
                    {
                        return parsed.setScale(field.getAnnotation(Scale.class).value());
                    }
                    else
                    {
                        return parsed;
                    }
                }
            });
        return converterStore;
    }

    private static boolean empty(String string)
    {
        return string == null || string.length() == 0;
    }

}
