package com.mass.window;

import com.mass.entity.RecordEntity;
import com.mass.entity.UserConsumed;
import org.apache.flink.api.java.tuple.Tuple;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import redis.clients.jedis.Jedis;

import java.util.ArrayList;
import java.util.List;

public class ItemCollectWindowFunctionRedis extends ProcessWindowFunction<RecordEntity, UserConsumed, Tuple, TimeWindow> {

    private final int histNum;
    private static Jedis jedis;

    public ItemCollectWindowFunctionRedis(int histNum) {
        this.histNum = histNum;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        jedis = new Jedis("localhost", 6379);
    }

    @Override
    public void process(Tuple key, Context ctx, Iterable<RecordEntity> elements, Collector<UserConsumed> out) {
        int userId = key.getField(0);
        long windowEnd = ctx.window().getEnd();

        String keyList = "hist_" + userId;
        elements.forEach(e -> jedis.rpush(keyList , String.valueOf(e.getItemId())));
        while (jedis.llen(keyList) > histNum) {
            jedis.lpop(keyList);
        }

        List<String> histItemsJedis = jedis.lrange(keyList, 0, -1);
        List<Integer> histItems = new ArrayList<>();
        for (String s : histItemsJedis) {
            histItems.add(Integer.parseInt(s));
        }
        out.collect(UserConsumed.of(userId, histItems, windowEnd));
    }

    @Override
    public void close() {
        jedis.close();
    }
}
