package com.mass.agg;

import org.apache.flink.api.common.functions.AggregateFunction;
import com.mass.entity.RecordEntity;

public class CountAgg implements AggregateFunction<RecordEntity, Long, Long> {

    @Override
    public Long createAccumulator() {
        return 0L;
    }

    @Override
    public Long add(RecordEntity value, Long accumulator) {
        return accumulator + 1;
    }

    @Override
    public Long getResult(Long accumulator) {
        return accumulator;
    }

    @Override
    public Long merge(Long a, Long b) {
        return a + b;
    }
}

