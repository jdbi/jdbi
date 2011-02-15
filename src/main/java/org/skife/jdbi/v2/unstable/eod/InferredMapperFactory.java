package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.TypeResolver;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.util.List;

class InferredMapperFactory implements MapperFactory
{
    private final static TypeResolver tr = new TypeResolver();
    private final Class maps;
    private final ResultSetMapper mapper;

    public InferredMapperFactory(ResultSetMapper mapper)
    {
        this.mapper = mapper;
        ResolvedType rt = tr.resolve(mapper.getClass());
        List<ResolvedType> rs = rt.typeParametersFor(ResultSetMapper.class);
        if (rs.isEmpty() || rs.get(0).getErasedType().equals(Object.class)) {
            throw new UnsupportedOperationException("Must use a concretely typed ResultSetMapper here");
        }

        maps = rs.get(0).getErasedType();
    }

    public boolean accepts(Class type)
    {
        return maps.equals(type);
    }

    public ResultSetMapper mapperFor(Class type)
    {
        return mapper;
    }
}
