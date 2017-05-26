package org.skife.jdbi.v2;

public interface TypedFolder2<AccumulatorType> extends Folder2<AccumulatorType> {
    Class<AccumulatorType> getAccumulatorType();
}
