package com.mass.window;

import com.mass.entity.TopItemEntity;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

public class CountProcessWindowFunction extends ProcessWindowFunction<Long, TopItemEntity, Integer, TimeWindow> {

    @Override
    public void process(Integer itemId, Context ctx, Iterable<Long> aggregateResults, Collector<TopItemEntity> out) {
        long windowEnd = ctx.window().getEnd();
        long count = aggregateResults.iterator().next();
        out.collect(TopItemEntity.of(itemId, windowEnd, count));
    }
}


