package com.mass.task;

import com.mass.entity.RecordEntity;
import com.mass.entity.UserConsumed;
import com.mass.recommend.mlflow.MLflowRecommender;
import com.mass.recommend.onnx.ONNXRecommender;
import com.mass.sink.MongodbRecommendSink;
import com.mass.source.CustomFileSource;
import com.mass.window.ItemCollectWindowFunction;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.assigners.DynamicEventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

public class SessionRecommend {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        DataStream<RecordEntity> stream = env.addSource(new CustomFileSource("/news_data.csv", false));
        stream.assignTimestampsAndWatermarks(
            new BoundedOutOfOrdernessTimestampExtractor<RecordEntity>(Time.seconds(10)) {
                @Override
                public long extractTimestamp(RecordEntity element) {
                    return element.getTime();
                }
            })
            .keyBy("userId")  // return tuple
            .window(EventTimeSessionWindows.withGap(Time.hours(1)))
            .process(new ItemCollectWindowFunction(10))
            .keyBy(UserConsumed::getUserId)
            .process(new ONNXRecommender(10, 10))
            .addSink(new MongodbRecommendSink());

        env.execute("SessionRecommend");
    }
}
