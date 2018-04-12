package org.jdbi.v3.core.mapper.reflect;

import java.beans.IntrospectionException;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public class ImmutableInfo {

    private List<PropertyDescriptor> descriptors;

    public ImmutableInfo(Class immutable, Class builder) {
        List<Method> getter = Arrays.asList(immutable.getMethods());
        List<Method> setter = Arrays.asList(builder.getMethods());

        descriptors = getter.stream().map(g -> {
            try {
                return new PropertyDescriptor(g.getName(), g, setter.stream().filter(s -> s.getName().equals(g.getName())).findFirst().get());
            } catch (IntrospectionException e) {
                throw new IllegalArgumentException();
            }
        }).collect(Collectors.toList());
    }

    /**
     * A Getter for descriptors.
     *
     * @return the value of descriptors
     */
    public List<PropertyDescriptor> getPropertyDescriptors() {
        return descriptors;
    }
}
