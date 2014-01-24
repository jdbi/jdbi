/*
 * Copyright (C) 2004 - 2013 Brian McCallister
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.skife.jdbi.v2.sqlobject;

import static org.junit.Assert.assertEquals;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.InvocationTargetException;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.util.Map;
import java.util.UUID;

import org.h2.jdbcx.JdbcDataSource;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;
import org.skife.jdbi.v2.MappingRegistry;
import org.skife.jdbi.v2.MappingRegistryAware;
import org.skife.jdbi.v2.ResultSetMapperFactory;
import org.skife.jdbi.v2.StatementContext;
import org.skife.jdbi.v2.tweak.ExtractableResultSetMapper;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import com.google.common.collect.Maps;

public class TestMappingRegistryAware {

	private DBI    dbi;
    private Handle handle;
	
	@Before
	public void setUp() throws Exception {
		JdbcDataSource ds = new JdbcDataSource();
        ds.setURL(String.format("jdbc:h2:mem:%s;DATABASE_TO_UPPER=FALSE", UUID.randomUUID()));
        dbi = new DBI(ds);
        handle = dbi.open();

        handle.execute("create table something (id bigint primary key, value int)");
	}

	@After
	public void tearDown() throws Exception {
		handle.execute("drop table something");
        handle.close();
	}

	@Test
	public void testMappingRegistryAware() throws Exception {
		handle.createStatement("insert into something (id,value) values (1,1)").execute();
		Dao dao = handle.attach(Dao.class);
		Something thing = dao.getSomething();
		assertEquals(1L, thing.getId().longValue());
		assertEquals(1, thing.getValue().intValue());		
	}

	public static abstract class Dao {

		@SqlQuery("select * from something where id=1")
		@RegistryAwareMapBean
		public abstract Something getSomething();

	}

	public static class Something {
		private Long id;
		private Integer value;

		public Integer getValue() {
			return value;
		}

		public void setValue(Integer value) {
			this.value = value;
		}

		public Long getId() {
			return id;
		}

		public void setId(Long id) {
			this.id = id;
		}

		public Something() {
		}

	}

	public static class RegistryAwareBeanMapperFactory implements
			ResultSetMapperFactory, MappingRegistryAware {

		private MappingRegistry registry;
		private final Class<?> type;
		
		public RegistryAwareBeanMapperFactory(Class<?> type) {
			this.type = type;
		}

		@Override
		public void setMappingRegistry(MappingRegistry registry) {
			this.registry = registry;
		}

		@Override
		public boolean accepts(Class type, StatementContext ctx) {
			return this.type.equals(type);
		}

		@Override
		public ResultSetMapper mapperFor(Class type, StatementContext ctx) {
			return new RegistryAwareBeanMapper(type, registry);
		}

	}

	public static class RegistryAwareBeanMapper<T> implements ResultSetMapper<T>,
			MappingRegistryAware {

		private final Class<T> type;
		private final Map<String, PropertyDescriptor> properties = Maps
				.newHashMap();
		private MappingRegistry registry;

		public RegistryAwareBeanMapper(Class<T> type, MappingRegistry registry) {
			this.type = type;
			this.registry = registry;
			BeanInfo info;
			try {
				info = Introspector.getBeanInfo(type);
			} catch (IntrospectionException e) {
				throw new IllegalStateException(e);
			}
			for (PropertyDescriptor descriptor : info.getPropertyDescriptors()) {
				properties.put(descriptor.getName(), descriptor);
			}
		}

		@Override
		public void setMappingRegistry(MappingRegistry registry) {
			this.registry = registry;
		}

		@Override
		public T map(int index, ResultSet r, StatementContext ctx)
				throws SQLException {
			T bean = null;
			try {
				bean = type.newInstance();
			} catch (Exception e) {
				throw new IllegalStateException(
						"Could not instantiate bean of type: " + type, e);
			}

			ResultSetMetaData metadata = r.getMetaData();

			for (int i = 1; i <= metadata.getColumnCount(); i++) {
				String columnName = metadata.getColumnLabel(i);
				System.out.println("iterating for column name: " + columnName);
				PropertyDescriptor descriptor = properties.get(columnName);

				try {
					if (descriptor != null) {
						Class<?> fieldType = descriptor.getPropertyType();
						ResultSetMapper<?> mapper = registry.mapperFor(
								fieldType, ctx, false);
						if (ExtractableResultSetMapper.class.isInstance(mapper)) {
							Object value = ((ExtractableResultSetMapper<?>) mapper)
									.extractByIndex(r, i);
							try {
								descriptor.getWriteMethod().invoke(bean, value);
							} catch (IllegalArgumentException e) {
								throw new SQLException(e);
							} catch (IllegalAccessException e) {
								throw new SQLException(e);
							} catch (InvocationTargetException e) {
								throw new SQLException(e);
							}
						}
					}
				} catch (SecurityException e) {
					throw new SQLException(e);
				}
			}
			return bean;
		}

	}

}
