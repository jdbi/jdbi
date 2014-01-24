package org.skife.jdbi.v2.sqlobject;

import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Method;
import java.sql.SQLException;

import org.skife.jdbi.v2.Query;
import org.skife.jdbi.v2.SQLStatement;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizer;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizerFactory;
import org.skife.jdbi.v2.sqlobject.SqlStatementCustomizingAnnotation;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD})
@SqlStatementCustomizingAnnotation(RegistryAwareMapBean.MapAsBeanFactory.class)
public @interface RegistryAwareMapBean {

	public static class MapAsBeanFactory implements SqlStatementCustomizerFactory {

		@Override
		public SqlStatementCustomizer createForMethod(Annotation annotation,
				Class sqlObjectType, final Method method) {
			return new SqlStatementCustomizer() {				
				@Override
				public void apply(SQLStatement q) throws SQLException {
					if (Query.class.isInstance(q)) {
						Query query = (Query) q;
						query.registerMapper(new TestMappingRegistryAware.RegistryAwareBeanMapperFactory(method.getReturnType()));
					}
				}
			};
		}

		@Override
		public SqlStatementCustomizer createForType(Annotation annotation,
				Class sqlObjectType) {
			throw new UnsupportedOperationException("Not allowed on types");			
		}

		@Override
		public SqlStatementCustomizer createForParameter(Annotation annotation,
				Class sqlObjectType, Method method, Object arg) {
			throw new UnsupportedOperationException("Not allowed on parameters");
		}
		
	}
	
}
