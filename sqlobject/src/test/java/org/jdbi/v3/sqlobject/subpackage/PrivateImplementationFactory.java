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
package org.jdbi.v3.sqlobject.subpackage;

import org.jdbi.v3.sqlobject.TestBeanBinder;

/**
 * Factory class that creates an instance whose type inherits from a public type (PublicInterface) but is of a type that is
 * inaccessible itself outside of its subpackage.
 *
 * This is used to verify that such instances' methods can be accessed via BeanBinding in the same way that they can be accessed
 * outside of the package via polymorphism.
 */
public class PrivateImplementationFactory
{
    private static class PrivateImplementation implements TestBeanBinder.PublicInterface {
        @Override
        public String getValue() {
            return "IShouldBind";
        }
    }

    public static TestBeanBinder.PublicInterface create() {
        return new PrivateImplementation();
    }
}
