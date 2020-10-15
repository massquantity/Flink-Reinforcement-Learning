package com.mass.process;

import com.mass.entity.TopItemEntity;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.api.common.state.ListState;

import java.util.ArrayList;
import java.util.List;
import java.util.Comparator;

public class TopNHotItems extends KeyedProcessFunction<Tuple, TopItemEntity, List<String>> {

    private ListState<TopItemEntity> itemState;
    private final int topSize;

    public TopNHotItems(int topSize) {
        this.topSize = topSize;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        ListStateDescriptor<TopItemEntity> itemStateDesc = new ListStateDescriptor<>("itemState", TopItemEntity.class);
        itemState = getRuntimeContext().getListState(itemStateDesc);
    }

    @Override
    public void processElement(TopItemEntity value, Context ctx, Collector<List<String>> out) throws Exception {
        itemState.add(value);
        ctx.timerService().registerEventTimeTimer(value.getWindowEnd() + 1);
    }

    @Override
    public void onTimer(long timestamp, OnTimerContext ctx, Collector<List<String>> out) throws Exception {
        List<TopItemEntity> allItems = new ArrayList<>();
        for (TopItemEntity item : itemState.get()) {
            allItems.add(item);
        }
        itemState.clear();

        allItems.sort(new Comparator<TopItemEntity>() {
            @Override
            public int compare(TopItemEntity o1, TopItemEntity o2) {
                return (int) (o2.getCounts() - o1.getCounts());
            }
        });

        List<String> ret = new ArrayList<>();
        long lastTime = ctx.getCurrentKey().getField(0);
        ret.add(String.valueOf(lastTime));  // add timestamp at first
        allItems.forEach(i -> ret.add(String.valueOf(i.getItemId())));
        out.collect(ret);
    }
}


