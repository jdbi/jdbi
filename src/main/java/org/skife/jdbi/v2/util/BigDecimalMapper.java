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
package org.skife.jdbi.v2.util;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;

public class BigDecimalMapper extends TypedMapper<BigDecimal>
{
    public BigDecimalMapper()
    {
        super();
    }

    public BigDecimalMapper(int index)
    {
        super(index);
    }

    public BigDecimalMapper(String name)
    {
        super(name);
    }

    @Override
    protected BigDecimal extractByName(ResultSet r, String name) throws SQLException
    {
        return r.getBigDecimal(name);
    }

    @Override
    protected BigDecimal extractByIndex(ResultSet r, int index) throws SQLException
    {
        return r.getBigDecimal(index);
    }

    public static final BigDecimalMapper FIRST = new BigDecimalMapper();
}
