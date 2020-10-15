package com.mass.window;

import com.mass.entity.RecordEntity;
import com.mass.entity.UserConsumed;
import org.apache.flink.api.common.state.ValueState;
import org.apache.flink.api.common.state.ValueStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;

import java.io.IOException;
import java.util.LinkedList;

public class ItemCollectWindowFunction extends ProcessWindowFunction<RecordEntity, UserConsumed, Tuple, TimeWindow> {

    private final int histNum;
    private ValueState<LinkedList<Integer>> itemState;

    public ItemCollectWindowFunction(int histNum) {
        this.histNum = histNum;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        ValueStateDescriptor<LinkedList<Integer>> itemStateDesc = new ValueStateDescriptor<>(
                "itemState", TypeInformation.of(new TypeHint<LinkedList<Integer>>() {}), new LinkedList<>());
        itemState = getRuntimeContext().getState(itemStateDesc);
    }

    @Override
    public void process(Tuple key, Context ctx, Iterable<RecordEntity> elements, Collector<UserConsumed> out)
            throws IOException {
        int userId = key.getField(0);
        long windowEnd = ctx.window().getEnd();

        LinkedList<Integer> histItems = itemState.value();
    //    if (histItems == null) {
    //        histItems = new LinkedList<>();
    //    }
        elements.forEach(e -> histItems.add(e.getItemId()));
        while (!histItems.isEmpty() && histItems.size() > histNum) {
            histItems.poll();
        }
        itemState.update(histItems);
        out.collect(UserConsumed.of(userId, histItems, windowEnd));
    }

    @Override
    public void close() {
        itemState.clear();
    }
}
