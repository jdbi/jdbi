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

package org.skife.jdbi.v2;

import java.math.BigDecimal;

class SampleBean {
    private Long longField;
    protected String protectedStringField;
    public int packagePrivateIntField;
    BigDecimal privateBigDecimalField;
    SampleValueType valueTypeField;

    public Long getLongField() {
        return longField;
    }

    public void setLongField(Long longField) {
        this.longField = longField;
    }

    public String getProtectedStringField() {
        return protectedStringField;
    }

    protected void setProtectedStringField(String protectedStringField) {
        this.protectedStringField = protectedStringField;
    }

    public int getPackagePrivateIntField() {
        return packagePrivateIntField;
    }

    /* default */ void setPackagePrivateIntField(int packagePrivateIntField) {
        this.packagePrivateIntField = packagePrivateIntField;
    }

    public BigDecimal getPrivateBigDecimalField() {
        return privateBigDecimalField;
    }

    private void setPrivateBigDecimalField(BigDecimal privateBigDecimalField) {
        this.privateBigDecimalField = privateBigDecimalField;
    }

    public SampleValueType getValueTypeField() {
        return valueTypeField;
    }

    public void setValueTypeField(SampleValueType valueTypeField) {
        this.valueTypeField = valueTypeField;
    }
}
