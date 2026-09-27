/*
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
package org.jdbi.v3.core.mapper.reflect.nonpublic;

/**
 * Types that are not public, in a package that holds no Jdbi code, so Jdbi reaches them only through the
 * accessible object strategy.
 */
public final class NonPublicTypes {
    public static final Class<?> HIDDEN_BEAN = HiddenBean.class;
    public static final Class<?> HIDDEN_CONSTRUCTED = HiddenConstructed.class;
    public static final Class<?> HIDDEN_FACTORY_MADE = HiddenFactoryMade.class;
    public static final Class<?> HIDDEN_IMMUTABLE = HiddenImmutable.class;
    public static final Class<?> HIDDEN_RECORD = HiddenRecord.class;

    private NonPublicTypes() {}

    public static Object hiddenBean(long id, String name) {
        HiddenBean bean = new HiddenBean();
        bean.setId(id);
        bean.setName(name);
        return bean;
    }

    public static Object hiddenImmutable(long id, String name) {
        return ImmutableHiddenImmutable.builder().id(id).name(name).build();
    }

    public static VisibleBean visibleBean(long id) {
        VisibleBean bean = new VisibleBean();
        bean.setId(id);
        return bean;
    }
}
