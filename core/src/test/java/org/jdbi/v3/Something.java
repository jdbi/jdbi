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
package org.jdbi.v3;

/**
 *
 */
public class Something
{
    private int id;
    private String name;
    private Integer integerValue;
    private int intValue;

    public Something()
    {
    }

    public Something(int id, String name)
    {
        this.id = id;
        this.name = name;
    }

    public int getId()
    {
        return id;
    }

    public void setId(int id)
    {
        this.id = id;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }


    public Integer getIntegerValue()
    {
        return integerValue;
    }

    public void setIntegerValue(Integer integerValue)
    {
        this.integerValue = integerValue;
    }

    public int getIntValue()
    {
        return intValue;
    }

    public void setIntValue(int intValue)
    {
        this.intValue = intValue;
    }

    // Issue #61: @BindBean fails if there is a writable but not readable property, so let's have one...
    public void setWithoutGetter(String bogus)
    {
        throw new UnsupportedOperationException();
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) return true;
        if (!(o instanceof Something)) return false;

        Something something = (Something) o;

        if (id != something.id) return false;
        if (intValue != something.intValue) return false;
        if (integerValue != null ? !integerValue.equals(something.integerValue) : something.integerValue != null)
            return false;
        if (name != null ? !name.equals(something.name) : something.name != null) return false;

        return true;
    }

    @Override
    public int hashCode()
    {
        int result = id;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + (integerValue != null ? integerValue.hashCode() : 0);
        result = 31 * result + intValue;
        return result;
    }

    @Override
    public String toString()
    {
        return "Something{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", integerValue=" + integerValue +
                ", intValue=" + intValue +
                '}';
    }
}
