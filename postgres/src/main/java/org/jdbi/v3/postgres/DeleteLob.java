package org.jdbi.v3.postgres;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import org.jdbi.v3.postgres.internal.DeleteLobCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;

/**
 * When used in combination with a SQL Update that returns a large-object OID as a result,
 * will delete the referenced large object as part of the statement execution.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlStatementCustomizingAnnotation(DeleteLobCustomizerFactory.class)
public @interface DeleteLob {
    /**
     * The sql update (or statement name if using a statement locator) to be executed. The default value will use
     * the method name of the method being annotated. This default behavior is only useful in conjunction
     * with a statement locator.
     *
     * @return the SQL string (or name)
     */
    String value() default "";

    /**
     * The generated key column name of large object oids that should be deleted.
     * @return the generated key column name
     */
    String lobOidKey() default "";
}
