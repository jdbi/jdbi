/**
 * Copyright (c), Data Geekery GmbH, contact@datageekery.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.jdbi.v3.lib.internal.org_jooq.jool_java_8.v0_9_14;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * @author Lukas Eder
 */
abstract class Sum<N> {
    long count;

    void add(Sum<N> sum) {
        add0(sum.result());
        count += sum.count;
    }

    void add(N value) {
        add0(value);
        count += 1;
    }

    void and(Sum<N> sum) {
        and0(sum.result());
    }

    void and(N value) {
        and0(value);
    }

    void or(Sum<N> sum) {
        or0(sum.result());
    }

    void or(N value) {
        or0(value);
    }

    abstract void add0(N value);
    abstract void and0(N value);
    abstract void or0(N value);
    abstract N result();
    abstract N avg();

    @SuppressWarnings({ "unchecked", "rawtypes" })
    static <N> Sum<N> create(N value) {
        Sum<N> result;

        if (value instanceof Byte)
            result = (Sum) new OfByte();
        else if (value instanceof Short)
            result = (Sum) new OfShort();
        else if (value instanceof Integer)
            result = (Sum) new OfInt();
        else if (value instanceof Long)
            result = (Sum) new OfLong();
        else if (value instanceof Float)
            result = (Sum) new OfFloat();
        else if (value instanceof Double)
            result = (Sum) new OfDouble();
        else if (value instanceof BigInteger)
            result = (Sum) new OfBigInteger();
        else if (value instanceof BigDecimal)
            result = (Sum) new OfBigDecimal();
        else
            throw new IllegalArgumentException("Cannot calculate sums for value : " + value);

        result.add(value);
        return result;
    }

    static class OfByte extends Sum<Byte> {
        byte result;

        @Override
        void add0(Byte value) {
            result += value;
        }

        @Override
        Byte result() {
            return result;
        }

        @Override
        void and0(Byte value) {
            result &= value;
        }

        @Override
        void or0(Byte value) {
            result |= value;
        }

        @Override
        Byte avg() {
            return (byte) (result / count);
        }
    }
    static class OfShort extends Sum<Short> {
        short sum;

        @Override
        void add0(Short value) {
            sum += value;
        }

        @Override
        void and0(Short value) {
            sum &= value;
        }

        @Override
        void or0(Short value) {
            sum |= value;
        }

        @Override
        Short result() {
            return sum;
        }

        @Override
        Short avg() {
            return (short) (sum / count);
        }
    }
    static class OfInt extends Sum<Integer> {
        int sum;

        @Override
        void add0(Integer value) {
            sum += value;
        }

        @Override
        void and0(Integer value) {
            sum &= value;
        }

        @Override
        void or0(Integer value) {
            sum |= value;
        }

        @Override
        Integer result() {
            return sum;
        }

        @Override
        Integer avg() {
            return (int) (sum / count);
        }
    }
    static class OfLong extends Sum<Long> {
        long sum;

        @Override
        void add0(Long value) {
            sum += value;
        }

        @Override
        void and0(Long value) {
            sum &= value;
        }

        @Override
        void or0(Long value) {
            sum |= value;
        }

        @Override
        Long result() {
            return sum;
        }

        @Override
        Long avg() {
            return sum / count;
        }
    }
    static class OfFloat extends Sum<Float> {
        float sum;

        @Override
        void add0(Float value) {
            sum += value;
        }

        @Override
        void and0(Float value) {
            throw new UnsupportedOperationException();
        }

        @Override
        void or0(Float value) {
            throw new UnsupportedOperationException();
        }

        @Override
        Float result() {
            return sum;
        }

        @Override
        Float avg() {
            return sum / (float) count;
        }
    }
    static class OfDouble extends Sum<Double> {
        double sum;

        @Override
        void add0(Double value) {
            sum += value;
        }

        @Override
        void and0(Double value) {
            throw new UnsupportedOperationException();
        }

        @Override
        void or0(Double value) {
            throw new UnsupportedOperationException();
        }

        @Override
        Double result() {
            return sum;
        }

        @Override
        Double avg() {
            return sum / (double) count;
        }
    }
    static class OfBigInteger extends Sum<BigInteger> {
        BigInteger sum = BigInteger.ZERO;

        @Override
        void add0(BigInteger value) {
            sum = sum.add(value);
        }

        @Override
        void and0(BigInteger value) {
            throw new UnsupportedOperationException();
        }

        @Override
        void or0(BigInteger value) {
            throw new UnsupportedOperationException();
        }

        @Override
        BigInteger result() {
            return sum;
        }

        @Override
        BigInteger avg() {
            return sum.divide(BigInteger.valueOf(count));
        }
    }
    static class OfBigDecimal extends Sum<BigDecimal> {
        BigDecimal sum = BigDecimal.ZERO;

        @Override
        void add0(BigDecimal value) {
            sum = sum.add(value);
        }

        @Override
        void and0(BigDecimal value) {
            throw new UnsupportedOperationException();
        }

        @Override
        void or0(BigDecimal value) {
            throw new UnsupportedOperationException();
        }

        @Override
        BigDecimal result() {
            return sum;
        }

        @Override
        BigDecimal avg() {
            return sum.divide(BigDecimal.valueOf(count), RoundingMode.HALF_EVEN);
        }
    }
}
