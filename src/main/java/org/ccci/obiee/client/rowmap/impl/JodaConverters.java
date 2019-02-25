package org.ccci.obiee.client.rowmap.impl;

import org.ccci.obiee.client.rowmap.Converter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

import java.lang.reflect.Field;
import java.util.Date;

class JodaConverters
{
    static void addJodaTimeConverters(ConverterStore converterStore)
    {

        converterStore.addConverter(LocalDate.class, new Converter<LocalDate>()
        {
            DateTimeFormatter isoFormatter = ISODateTimeFormat.localDateOptionalTimeParser();

            public LocalDate convert(String xmlValue, Field field)
            {
                if (empty(xmlValue)) return null;
                DateTime parsedDateTime = isoFormatter.parseDateTime(xmlValue);
                return parsedDateTime.toLocalDate();
            }
        });

        converterStore.addConverter(DateTime.class, new Converter<DateTime>()
        {
            DateTimeFormatter isoFormatter =
                ISODateTimeFormat.dateHourMinuteSecond()
                    .withZone(DateTimeZone.UTC);

            public DateTime convert(String xmlValue, Field field)
            {
                if (empty(xmlValue)) return null;
                return isoFormatter.parseDateTime(xmlValue);
            }
        });

    }

    static boolean isJodaField(Class<?> fieldType)
    {
        return fieldType.equals(LocalDate.class) || fieldType.equals(DateTime.class);
    }

    static Object convertJodaValueToReportVariableValue(Object jodaValue)
    {
        if(jodaValue instanceof LocalDate)
        {
            return convertLocalDateToXmlDate((LocalDate) jodaValue);
        }
        else if(jodaValue instanceof DateTime)
        {
            return convertDateTimeToUTCDate((DateTime) jodaValue);
        }
        else
        {
            throw new IllegalArgumentException("illegal: " + jodaValue);
        }
    }

    /*
     * See note in AnalyticsManagerImpl.convertLocalDateToXmlDate()
     */
    private static String convertLocalDateToXmlDate(LocalDate localDate)
    {
        return "date '" + ISODateTimeFormat.yearMonthDay().print(localDate) + "'";
    }

    /*
     * See note in AnalyticsManagerImpl.convertDateTimeToUTCDate()
     */
    private static Date convertDateTimeToUTCDate(DateTime dateTime)
    {
        DateTime correctedDateTime = dateTime.withZoneRetainFields(DateTimeZone.UTC);
        return correctedDateTime.toDate();
    }



    private static boolean empty(String string)
    {
        return string == null || string.length() == 0;
    }

}
