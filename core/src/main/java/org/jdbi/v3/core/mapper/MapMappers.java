package org.jdbi.v3.core.mapper;

import java.util.Locale;
import java.util.function.UnaryOperator;
import org.jdbi.v3.core.config.JdbiConfig;
import org.jdbi.v3.meta.Beta;

@Beta
public class MapMappers implements JdbiConfig<MapMappers> {
    @Beta
    public static final UnaryOperator<String> NOP = UnaryOperator.identity();
    @Beta
    public static final UnaryOperator<String> LOCALE_LOWER = s -> s.toLowerCase(Locale.ROOT);
    @Beta
    public static final UnaryOperator<String> LOCALE_UPPER = s -> s.toUpperCase(Locale.ROOT);

    private UnaryOperator<String> caseChange;
    private boolean forceNewApi;

    public MapMappers() {
        // TODO law of least surprise: change to nop in jdbi4
        caseChange = LOCALE_LOWER;
        forceNewApi = false;
    }

    private MapMappers(MapMappers that) {
        caseChange = that.caseChange;
        forceNewApi = that.forceNewApi;
    }

    @Beta
    public UnaryOperator<String> getCaseChange() {
        return caseChange;
    }

    @Beta
    public MapMappers setCaseChange(UnaryOperator<String> caseChange) {
        this.caseChange = caseChange;
        return this;
    }

    @Override
    public MapMappers createCopy() {
        return new MapMappers(this);
    }
}
