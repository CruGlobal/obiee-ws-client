package org.ccci.obiee.client.rowmap.impl;

import java.lang.reflect.Field;

public interface Converter<T>
{

    public T convert(String xmlValue, Field field);
}
