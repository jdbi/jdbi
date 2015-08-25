/*
 * Copyright (C) 2004 - 2014 Brian McCallister
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

package org.skife.jdbi.v2;

import java.math.BigDecimal;

class SampleBean {
    private Long longField;
    protected String stringField;
    public int intField;
    BigDecimal bigDecimalField;

    public Long getLongField() {
        return longField;
    }

    public void setLongField(Long longField) {
        this.longField = longField;
    }

    public String getStringField() {
        return stringField;
    }

    protected void setStringField(String stringField) {
        this.stringField = stringField;
    }

    public int getIntField() {
        return intField;
    }

    /* default */ void setIntField(int intField) {
        this.intField = intField;
    }

    public BigDecimal getBigDecimalField() {
        return bigDecimalField;
    }

    private void setBigDecimalField(BigDecimal bigDecimalField) {
        this.bigDecimalField = bigDecimalField;
    }
}
