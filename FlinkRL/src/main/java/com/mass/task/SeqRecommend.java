package com.mass.task;

import com.mass.entity.RecordEntity;
import com.mass.recommend.fastapi.FastapiRecommender;
// import com.mass.recommend.onnx.ONNXRecommender;
import com.mass.sink.MongodbRecommendSink;
import com.mass.source.CustomFileSource;
import com.mass.window.ItemCollectWindowFunction;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;

public class SeqRecommend {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<RecordEntity> stream = env.addSource(new CustomFileSource("tianchi.csv", false));
        stream.assignTimestampsAndWatermarks(
                new BoundedOutOfOrdernessTimestampExtractor<RecordEntity>(Time.seconds(10)) {
            @Override
            public long extractTimestamp(RecordEntity element) {
                return element.getTime();
            }
        })
        .keyBy("userId")  // return tuple
        // .keyBy(RecordEntity -> RecordEntity.userId)  // return Integer
        .timeWindow(Time.seconds(60), Time.seconds(20))
        .process(new ItemCollectWindowFunction(10))
        .keyBy("windowEnd")
        .flatMap(new FastapiRecommender(8, 10, "ddpg", false))
        .addSink(new MongodbRecommendSink());

        env.execute("ItemSeqRecommend");
    }
}



