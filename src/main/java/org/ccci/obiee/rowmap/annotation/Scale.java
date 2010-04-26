package org.ccci.obiee.rowmap.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Must annotate a field of type BigDecimal.  Indicates the expected scale
 * of the BigDecimals to be loaded from the Analytics environment.
 * 
 * This exists primarily to allow values to display as '30.00' instead of '30.0'.
 * 
 * @author Matt Drees
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Scale
{
    int value();
}
