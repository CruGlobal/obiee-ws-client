package org.ccci.obiee.client.rowmap.annotation;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies a variable to be used in the form as a filter parameter.
 *  
 * @author William Randall
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface ReportParamVariable 
{
	/**
	 * Specifies the name of the parameter.  This is optional.  If it is not specified, the
	 * variable name (as it is) will be used instead.
	 */
	String name() default "";
}
