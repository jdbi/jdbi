package org.jdbi.v3.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.time.Clock;
import java.time.OffsetDateTime;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizer;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizerFactory;
import org.jdbi.v3.sqlobject.customizer.SqlStatementCustomizingAnnotation;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@SqlStatementCustomizingAnnotation(BindTime.Factory.class)
public @interface BindTime {

    class Factory implements SqlStatementCustomizerFactory {
        @Override
        public SqlStatementCustomizer createForType(Annotation annotation, Class<?> sqlObjectType) {
            return stmt -> stmt.bind("now", OffsetDateTime.now(stmt.getConfig(Config.class).clock));
        }
    }

    class Config implements JdbiConfig<Config> {
        public Clock clock;

        @Override
        public Config createCopy() {
            Config copy = new Config();
            copy.clock = this.clock;
            return copy;
        }
    }
}
