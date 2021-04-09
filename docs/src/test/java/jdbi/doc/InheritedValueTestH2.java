package jdbi.doc;

import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableSet;
import org.jdbi.v3.core.Jdbi;
import org.jdbi.v3.core.argument.Argument;
import org.jdbi.v3.core.codec.Codec;
import org.jdbi.v3.core.generic.GenericType;
import org.jdbi.v3.core.mapper.ColumnMapper;
import org.jdbi.v3.core.qualifier.QualifiedType;
import org.jdbi.v3.core.rule.DatabaseRule;
import org.jdbi.v3.core.rule.H2DatabaseRule;
import org.jdbi.v3.guava.codec.TypeResolvingCodecFactory;
import org.jdbi.v3.sqlobject.SingleValue;
import org.jdbi.v3.sqlobject.SqlObjectPlugin;
import org.jdbi.v3.sqlobject.customizer.Bind;
import org.jdbi.v3.sqlobject.customizer.BindBean;
import org.jdbi.v3.sqlobject.statement.SqlQuery;
import org.jdbi.v3.sqlobject.statement.SqlUpdate;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;

// this test is in doc because it needs guava and sqlobject and sqlobject already imports guava.
// the only other place it could go to is in sqlobject and it tests a guava class, not a sqlobject class.
public class InheritedValueTestH2 {

    // SQL object dao
    public interface DataDao {

        @SqlUpdate("INSERT INTO data (id, value) VALUES (:bean.id, :bean.value)")
        int storeData(@BindBean("bean") Bean<Value<String>> bean);

        @SqlUpdate("INSERT INTO data (id, value) VALUES (:id, :value)")
        int storeData(@Bind("id") String id, @Bind("value") Value<String> data);

        @SqlQuery("SELECT value from data where id = :id")
        Value<String> loadData(@Bind("id") String id);
    }

    public interface SetDao {

        @SqlUpdate("INSERT INTO data (id, value) VALUES (:id, :value)")
        int storeData(@Bind("id") String id, @Bind("value") Set<AutoValue> data);

        @SqlUpdate("INSERT INTO data (id, value) VALUES (:bean.id, :bean.value)")
        int storeData(@BindBean("bean") AutoValueBean bean);

        @SqlQuery("SELECT value from data where id = :id")
        @SingleValue
        Set<AutoValue> loadData(@Bind("id") String id);
    }

    @Rule
    public DatabaseRule dbRule = new H2DatabaseRule().withPlugin(new SqlObjectPlugin());

    public static final QualifiedType<Value<String>> DATA_TYPE = QualifiedType.of(new GenericType<Value<String>>() {});
    public static final QualifiedType<Set<AutoValue>> AUTOVALUE_SET_TYPE = QualifiedType.of(new GenericType<Set<AutoValue>>() {});

    @Before
    public void setUp() {
        dbRule.getJdbi().useHandle(h -> {
            h.execute("CREATE TABLE data (id VARCHAR PRIMARY KEY, value VARCHAR)");
        });
    }

    @Test
    public void testType() {
        Jdbi jdbi = dbRule.getJdbi();

        // register the codec with JDBI
        jdbi.registerCodecFactory(TypeResolvingCodecFactory.forSingleCodec(DATA_TYPE, new DataCodec()));

        StringValue data = new StringValue("one");

        String dataId = UUID.randomUUID().toString();

        // store object
        int result = jdbi.withExtension(DataDao.class, dao -> dao.storeData(dataId, data));

        assertEquals(1, result);

        // load object
        Value<String> restoredData = jdbi.withHandle(h -> h.createQuery("SELECT value from data where id = :id")
            .bind("id", dataId)
            .mapTo(DATA_TYPE).first());

        assertNotSame(data, restoredData);
        assertEquals(data, restoredData);
    }

    @Test
    public void testBean() {
        Jdbi jdbi = dbRule.getJdbi();

        // register the codec with JDBI
        jdbi.registerCodecFactory(TypeResolvingCodecFactory.forSingleCodec(DATA_TYPE, new DataCodec()));

        StringBean stringBean = new StringBean(UUID.randomUUID().toString(), new StringValue("two"));

        // store object
        int result = jdbi.withExtension(DataDao.class, dao -> dao.storeData(stringBean));

        assertEquals(1, result);

        // load object
        Value<String> restoredData = jdbi.withExtension(DataDao.class, dao -> dao.loadData(stringBean.getId()));

        assertNotSame(stringBean.getValue(), restoredData);
        assertEquals(stringBean.getValue(), restoredData);
    }

    @Test
    public void testCollection() {
        Jdbi jdbi = dbRule.getJdbi();

        // register codec
        jdbi.registerCodecFactory(TypeResolvingCodecFactory.builder()
            .addCodec(AUTOVALUE_SET_TYPE, new AutoValueSetCodec())
            .addCodec(DATA_TYPE, new DataCodec())
            .build());

        String id = UUID.randomUUID().toString();
        Set<AutoValue> data = ImmutableSet.of(AutoValue.create("one"), AutoValue.create("two"), AutoValue.create("three"));

        // store object
        int result = jdbi.withExtension(SetDao.class, dao -> dao.storeData(id, data));

        assertEquals(1, result);

        // load object
        Set<AutoValue> restoredData = jdbi.withExtension(SetDao.class, dao -> dao.loadData(id));

        assertNotSame(data, restoredData);
        assertEquals(data, restoredData);
    }

    @Test
    public void testCollectionBean() {
        Jdbi jdbi = dbRule.getJdbi();

        // register codec
        jdbi.registerCodecFactory(TypeResolvingCodecFactory.builder()
            .addCodec(AUTOVALUE_SET_TYPE, new AutoValueSetCodec())
            .addCodec(DATA_TYPE, new DataCodec())
            .build());

        AutoValueBean bean = new AutoValueBean(
            UUID.randomUUID().toString(),
            ImmutableSet.of(AutoValue.create("one"), AutoValue.create("two"), AutoValue.create("three")));

        // store object
        int result = jdbi.withExtension(SetDao.class, dao -> dao.storeData(bean));

        assertEquals(1, result);

        // load object
        Set<AutoValue> restoredData = jdbi.withExtension(SetDao.class, dao -> dao.loadData(bean.getId()));

        assertNotSame(bean.getValue(), restoredData);
        assertEquals(bean.getValue(), restoredData);
    }

    public static class DataCodec implements Codec<Value<String>> {

        @Override
        public ColumnMapper<Value<String>> getColumnMapper() {
            return (r, idx, ctx) -> {
                return new StringValue(r.getString(idx));
            };
        }

        @Override
        public Function<Value<String>, Argument> getArgumentFunction() {
            return data -> (idx, stmt, ctx) -> {
                stmt.setString(idx, data.getValue());
            };
        }
    }

    public static class AutoValueSetCodec implements Codec<Set<AutoValue>> {

        @Override
        public ColumnMapper<Set<AutoValue>> getColumnMapper() {
            return (r, idx, ctx) -> {
                ImmutableSet.Builder<AutoValue> builder = ImmutableSet.builder();
                String value = r.getString(idx);
                if (value != null) {
                    Splitter.on(':').split(value).forEach(s -> builder.add(AutoValue.create(s)));
                }
                return builder.build();
            };
        }

        @Override
        public Function<Set<AutoValue>, Argument> getArgumentFunction() {
            return data -> (idx, stmt, ctx) -> {
                String value = Joiner.on(':').join(data.stream().map(AutoValue::getValue).collect(Collectors.toSet()));
                stmt.setString(idx, value);
            };
        }
    }

    public interface Value<T> {

        T getValue();
    }

    public static class StringValue implements Value<String> {

        private final String value;

        public StringValue(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            StringValue stringValue = (StringValue) o;
            return Objects.equals(value, stringValue.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }

    public interface Bean<T> {

        String getId();

        T getValue();
    }

    public static class StringBean implements Bean<Value<String>> {

        private final String id;

        private final Value<String> value;

        public StringBean(String id, Value<String> value) {
            this.id = id;
            this.value = value;
        }

        @Override
        public String getId() {
            return id;
        }

        @Override
        public Value<String> getValue() {
            return value;
        }
    }

    public static class AutoValueBean {

        private final String id;
        private final Set<AutoValue> value;

        public AutoValueBean(String id, Set<AutoValue> value) {
            this.id = id;
            this.value = value;
        }

        public String getId() {
            return id;
        }

        public Set<AutoValue> getValue() {
            return value;
        }
    }

    public abstract static class AutoValue {

        public static AutoValue create(String value) {
            return new AutoValueImpl(value);
        }

        public abstract String getValue();
    }

    static class AutoValueImpl extends AutoValue {

        private final String value;

        AutoValueImpl(String value) {
            this.value = value;
        }

        @Override
        public String getValue() {
            return value;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            AutoValueImpl autoValue = (AutoValueImpl) o;
            return Objects.equals(value, autoValue.value);
        }

        @Override
        public int hashCode() {
            return Objects.hash(value);
        }
    }
}
