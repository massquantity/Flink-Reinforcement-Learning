package com.mass.task;

import com.mass.entity.RecordEntity;
import com.mass.sink.MongodbRecordSink;
import com.mass.source.CustomFileSource;
import com.mass.util.Property;
import com.mass.util.RecordToEntity;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

public class RecordToMongoDB {
    public static void main(String[] args) throws Exception {
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

        DataStream<RecordEntity> recordStream = sourceStream.map(new RichMapFunction<String, RecordEntity>() {
                    private InputStream userStream;
                    private InputStream itemStream;
                    private JSONObject userJSON;
                    private JSONObject itemJSON;

                    @Override
                    public void open(Configuration parameters) throws FileNotFoundException {
                        userStream = new FileInputStream(Property.getStrValue("user_map"));
                        itemStream = new FileInputStream(Property.getStrValue("item_map"));
                        try {
                            userJSON = new JSONObject(new JSONTokener(userStream));
                            itemJSON = new JSONObject(new JSONTokener(itemStream));
                        } catch (NullPointerException e) {
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public RecordEntity map(String value) {
                        return RecordToEntity.getRecord(value, userJSON, itemJSON);
                   }

                    @Override
                    public void close() throws Exception {
                        userStream.close();
                        itemStream.close();
                    }
                }
            );

        recordStream.addSink(new MongodbRecordSink());
        env.execute("RecordToMongoDB");
    }
}
