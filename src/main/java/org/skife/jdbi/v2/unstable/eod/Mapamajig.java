package org.skife.jdbi.v2.unstable.eod;

import com.fasterxml.classmate.ResolvedType;
import com.fasterxml.classmate.members.ResolvedMethod;
import org.skife.jdbi.v2.tweak.ResultSetMapper;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

class Mapamajig
{
    public List<MapperFactory> factories = new CopyOnWriteArrayList<MapperFactory>();

    private final ConcurrentHashMap<ResolvedType, ResultSetMapper> cache = new ConcurrentHashMap<ResolvedType, ResultSetMapper>();

    public void add(ResultSetMapper mapper)
    {
        this.add(new InferredMapperFactory(mapper));
    }

    public void add(MapperFactory factory)
    {
        factories.add(factory);
        cache.clear();
    }

    ResultSetMapper mapperFor(ResolvedMethod method, ResolvedType returnType)
    {
        if (method.getRawMember().isAnnotationPresent(Mapper.class)) {
            Mapper mapper = method.getRawMember().getAnnotation(Mapper.class);
            try {
                return mapper.value().newInstance();
            }
            catch (Exception e) {
                throw new RuntimeException("unable to invoke default ctor on " + method, e);
            }
        }


        ResultSetMapper cached_mapper = cache.get(returnType);
        if (cached_mapper != null) {
            return cached_mapper;
        }

        if (method.getRawMember().isAnnotationPresent(Mapper.class)) {
            Mapper mapper = method.getRawMember().getAnnotation(Mapper.class);
            try {
                final ResultSetMapper rsm = mapper.value().newInstance();
                cache.put(returnType, rsm);
                return rsm;
            }
            catch (Exception e) {
                throw new RuntimeException("unable to invoke default ctor on " + method, e);
            }
        }


        for (MapperFactory factory : factories) {
            if (factory.accepts(returnType.getErasedType())) {
                final ResultSetMapper mapper = factory.mapperFor(returnType.getErasedType());
                cache.put(returnType, mapper);
                return mapper;
            }
        }

        throw new UnsupportedOperationException("Not Yet Implemented!");
    }
}
