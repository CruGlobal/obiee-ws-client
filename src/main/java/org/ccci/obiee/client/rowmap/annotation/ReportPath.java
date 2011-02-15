package org.ccci.obiee.client.rowmap.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates which report the annotated class is mapped to.
 * 
 * @author Matt Drees
 *
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReportPath
{

    /**
     * The 'path' of the report, e.g. "/shared/CCCi/SSW/SAI Donations"
     */
    String value();

}
