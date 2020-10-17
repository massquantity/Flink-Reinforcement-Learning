package com.mass.task;

import com.mass.agg.CountAgg;
import com.mass.entity.RecordEntity;
import com.mass.map.RecordMap;
import com.mass.process.TopNPopularItems;
import com.mass.sink.TopNRedisSink;
import com.mass.window.CountProcessWindowFunction;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.streaming.connectors.redis.RedisSink;
import org.apache.flink.streaming.connectors.redis.common.config.FlinkJedisPoolConfig;
import org.apache.flink.util.Collector;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Properties;

public class IntervalPopularItems {
    private static final int topSize = 17;
    private static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        FlinkJedisPoolConfig redisConf = new FlinkJedisPoolConfig
                .Builder()
                .setHost("localhost")
                .setPort(6379)
                .build();
        Properties prop = new Properties();
        prop.setProperty("bootstrap.servers", "localhost:9092");
        prop.setProperty("group.id", "rec");
        prop.setProperty("key.deserializer", StringDeserializer.class.getName());
        prop.setProperty("value.deserializer",
                Class.forName("org.apache.kafka.common.serialization.StringDeserializer").getName());
        DataStreamSource<String> sourceStream = env
                .addSource(new FlinkKafkaConsumer<>("flink-rl", new SimpleStringSchema(), prop));

        DataStream<Tuple2<String, String>> topItem = sourceStream
                .map(new RecordMap())
                .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<RecordEntity>() {
                    @Override
                    public long extractAscendingTimestamp(RecordEntity element) {
                        return element.getTime();
                    }
                })
                // .keyBy("itemId")  // if using POJOs, will convert to tuple
                // .keyBy(i -> i.getItemId())
                .keyBy(RecordEntity::getItemId)
                .timeWindow(Time.minutes(60), Time.seconds(80))
                .aggregate(new CountAgg(), new CountProcessWindowFunction())
                .keyBy("windowEnd")
                .process(new TopNPopularItems(topSize))
                .flatMap(new FlatMapFunction<List<String>, Tuple2<String, String>>() {
                    @Override
                    public void flatMap(List<String> value, Collector<Tuple2<String, String>> out) {
                        showTime(value.get(0));
                        for (int rank = 1; rank < value.size(); ++rank) {
                            String item = value.get(rank);
                            System.out.println(String.format("item: %s, rank: %d", item, rank));
                            out.collect(Tuple2.of(item, String.valueOf(rank)));
                        }
                    }
                });

        topItem.addSink(new RedisSink<Tuple2<String, String>>(redisConf, new TopNRedisSink()));
        env.execute("IntervalPopularItems");
    }

    private static void showTime(String timestamp) {
        long windowTime = Long.valueOf(timestamp);
        Date date = new Date(windowTime);
        System.out.println("============== Top N Items during " + sdf.format(date) + " ==============");
    }
}
