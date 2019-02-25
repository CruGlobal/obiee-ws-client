package org.ccci.obiee.client.rowmap.impl;

import org.ccci.obiee.client.rowmap.Converter;
import org.testng.annotations.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;

public class ConverterStoreTest
{

    ConverterStore store = ConverterStore.buildDefault();

    @Test
    void testLocalDateConverterWithoutTime() {
        final Converter<LocalDate> converter = store.getConverter(LocalDate.class);
        final LocalDate localDate = converter.convert("2019-02-12", null);
        assertThat(localDate, is(equalTo(LocalDate.of(2019, 2, 12))));
    }

    @Test
    void testLocalDateConverterWithTime() {
        final Converter<LocalDate> converter = store.getConverter(LocalDate.class);
        final LocalDate localDate = converter.convert("2019-02-12T00:00:00", null);
        assertThat(localDate, is(equalTo(LocalDate.of(2019, 2, 12))));
    }

    @Test
    void testLocalDateTimeConverter() {
        final Converter<LocalDateTime> converter = store.getConverter(LocalDateTime.class);
        final LocalDateTime localDateTime = converter.convert("2019-02-12T01:02:03", null);
        assertThat(localDateTime, is(equalTo(LocalDateTime.of(2019, 2, 12, 1, 2, 3))));
    }
}
