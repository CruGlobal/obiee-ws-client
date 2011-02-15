package org.ccci.obiee.client.rowmap.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies the column in the report from which the annotated field is populated.  The combination of a 'Table Heading' and a 'Column Heading'
 * uniquely identifies a column in a given report.
 *  
 * @author Matt Drees
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Column
{

    /**
     * Specifies the 'Table Heading' portion of this report column identifier.  This is required. 
     */
    String tableHeading();

    /**
     * Specifies the 'Column Heading' portion of this report column identifier. This is optional, and if left
     * unspecified, the column heading is derived from the annotated field name according to the following
     * algorithm:
     * <ol>
     * <li>The first character is upper-cased</li>
     * <li>a space is inserted in between each lower-to-upper case character transition</li>
     * </ol>
     * <p>
     * For example, the field name 'designationNumber' will become 'Designation Number'. The intended effect is
     * idiomatic java field names will be naturally converted to an idiomatic OBIEE column heading name. Some
     * column headings may need to be explicitly configured due to symbols that are invalid in java field names
     * (such as dashes).
     * 
     */
    String columnHeading() default "";

}
