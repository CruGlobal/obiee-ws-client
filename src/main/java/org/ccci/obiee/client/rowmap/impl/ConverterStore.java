package org.ccci.obiee.client.rowmap.impl;

import org.ccci.obiee.client.rowmap.Converter;
import org.ccci.obiee.client.rowmap.annotation.Scale;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.HashMap;
import java.util.Map;

import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME;
import static java.time.format.DateTimeFormatter.ISO_LOCAL_TIME;
import static org.ccci.obiee.client.rowmap.impl.JodaTimeAvailability.isJodaAvailable;

public class ConverterStore
{

    /** A formatter that supports an ISO date, with an optional time component */
    static final DateTimeFormatter ISO_LOCAL_DATE_OPTIONAL_TIME;
    static {
        ISO_LOCAL_DATE_OPTIONAL_TIME = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .append(ISO_LOCAL_DATE)
            .optionalStart()
            .appendLiteral('T')
            .append(ISO_LOCAL_TIME)
            .toFormatter();
    }

    private final Map<Class<?>, Converter<?>> converters = new HashMap<>();
    
    
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
     * Should only be called during initial construction
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
        converterStore.addConverter(String.class, (xmlValue, field) -> xmlValue);
        
        Converter<Integer> integerConverter = (xmlValue, field) -> {
            if (empty(xmlValue)) return null;
            return Integer.valueOf(xmlValue);
        };
        converterStore.addConverter(Integer.class, integerConverter);
        converterStore.addConverter(Integer.TYPE, integerConverter);
        
        Converter<Long> longConverter = (xmlValue, field) -> {
            if (empty(xmlValue)) return null;
            return Long.valueOf(xmlValue);
        };
        converterStore.addConverter(Long.class, longConverter);
        converterStore.addConverter(Long.TYPE, longConverter);
        
        Converter<Double> doubleConverter = (xmlValue, field) -> {
            if (empty(xmlValue)) return null;
            return Double.valueOf(xmlValue);
        };
        converterStore.addConverter(Double.class, doubleConverter);
        converterStore.addConverter(Double.TYPE, doubleConverter);
        
        converterStore.addConverter(LocalDate.class, (xmlValue, field) -> {
            if (empty(xmlValue)) return null;
            return LocalDate.parse(xmlValue, ISO_LOCAL_DATE_OPTIONAL_TIME);
        });
        
        converterStore.addConverter(LocalDateTime.class, (xmlValue, field) -> {
            if (empty(xmlValue)) return null;
            return LocalDateTime.parse(xmlValue, ISO_LOCAL_DATE_TIME.withZone(ZoneOffset.UTC));
        });
        
        converterStore.addConverter(BigDecimal.class, (xmlValue, field) -> {
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
        });

        if (isJodaAvailable())
        {
            JodaConverters.addJodaTimeConverters(converterStore);
        }

        return converterStore;
    }


    private static boolean empty(String string)
    {
        return string == null || string.length() == 0;
    }

}
