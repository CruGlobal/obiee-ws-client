package org.ccci.obiee.client.rowmap;

import java.lang.reflect.Field;

public interface Converter<T>
{

    /**
     * Converts an xml attribute (of type String) to a {@code T}.  Implementations may use the {@code field} 
     * parameter to read annotations that in some way configure the conversion.  The returned value will be
     * stored in the given field by the caller of this method.  The field's type is guaranteed to be of type {@code T}.
     * Implementations of this interface may freely ignore this parameter.
     * 
     * @param xmlValue the value returned by the OBI server
     * @param field the eventual destination of the converted value
     * @return an appropriate java translation of the OBI value
     */
    public T convert(String xmlValue, Field field);
}
