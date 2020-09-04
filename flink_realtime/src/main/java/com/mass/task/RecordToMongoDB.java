package com.mass.task;

import com.mass.entity.RecordEntity;
import com.mass.sink.MongodbRecordSink;
import com.mass.util.RecordToEntity;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.flink.util.Collector;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.util.Properties;

public class RecordToMongoDB {
    public static void main(String[] args) throws Exception{
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        Properties prop = new Properties();
        prop.setProperty("bootstrap.servers", "localhost:9092");
        prop.setProperty("group.id", "rec");
        prop.setProperty("key.deserializer", StringDeserializer.class.getName());
        prop.setProperty("value.deserializer",
                Class.forName("org.apache.kafka.common.serialization.StringDeserializer").getName());
        DataStreamSource<String> sourceStream = env.addSource(
                new FlinkKafkaConsumer<>("flink-rl", new SimpleStringSchema(), prop));
    //    sourceStream.print();

        DataStream<RecordEntity> recordStream = sourceStream.map(
                (MapFunction<String, RecordEntity>) RecordToEntity::getRecord);
        recordStream.addSink(new MongodbRecordSink());

        //    DataStream<Tuple2<Integer, Integer>> consumedStream = sourceStream.map(
        //        new MapFunction<String, Tuple2<Integer, Integer>> () {
        //            @Override
        //            public Tuple2<Integer, Integer> map(String value) throws Exception {
        //                String[] elements = value.split(",");
        //                int userId = Integer.valueOf(elements[0]);
        //                int itemId = Integer.valueOf(elements[1]);
        //                return Tuple2.of(userId, itemId);
        //            }
        //        }
        //    );

        env.execute("RecordToMongoDB");
    }
}
