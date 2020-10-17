package com.mass.task;

import com.mass.entity.RecordEntity;
import com.mass.util.RecordToEntity;
import com.mass.map.RecordMap;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.serialization.SimpleStringSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.DataStreamSource;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.connectors.kafka.FlinkKafkaConsumer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.json.JSONObject;
import org.json.JSONTokener;

import java.io.InputStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Properties;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;


public class RecordToMysql {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(1);

        Properties prop = new Properties();
        prop.setProperty("bootstrap.servers", "localhost:9092");
        prop.setProperty("group.id", "rec");
        prop.setProperty("key.deserializer", StringDeserializer.class.getName());
        prop.setProperty("value.deserializer",
                Class.forName("org.apache.kafka.common.serialization.StringDeserializer").getName());
        DataStreamSource<String> stream = env.addSource(
                new FlinkKafkaConsumer<>("flink-rl", new SimpleStringSchema(), prop));
        //    stream.print();

        DataStream<RecordEntity> stream2 = stream.map(new RecordMap());
        stream2.addSink(new CustomMySqlSink());

        env.execute("RecordToMySql");
    }

    private static class CustomMySqlSink extends RichSinkFunction<RecordEntity> {
        Connection conn;
        PreparedStatement stmt;

        @Override
        public void open(Configuration parameters) throws Exception {
            conn = DriverManager.getConnection("jdbc:mysql://localhost:3306/flink", "root", "123456");
            stmt = conn.prepareStatement("INSERT INTO record (user, item, time) VALUES (?, ?, ?)");
        }

        @Override
        public void close() throws Exception {
            stmt.close();
            conn.close();
        }

        @Override
        public void invoke(RecordEntity value, Context context) throws Exception {
            stmt.setInt(1, value.getUserId());
            stmt.setInt(2, value.getItemId());
            DateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            stmt.setString(3, sdf.format(value.getTime()));
            stmt.executeUpdate();
        }
    }
}


