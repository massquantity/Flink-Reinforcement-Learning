package com.mass.task;

import com.mass.source.KafkaSource;

import java.io.IOException;
import java.util.Properties;

public class FileToKafka {
    public static void main(String[] args) throws IOException, InterruptedException {
        Properties kafkaProps = new Properties();
        kafkaProps.setProperty("bootstrap.servers", "localhost:9092");
        kafkaProps.setProperty("ack", "1");
        kafkaProps.setProperty("batch.size", "16384");
        kafkaProps.setProperty("linger.ms", "1");
        kafkaProps.setProperty("buffer.memory", "33554432");
        kafkaProps.setProperty("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
        kafkaProps.setProperty("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

        //  ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        //  InputStream is = classloader.getResourceAsStream("/news_data.csv");
        String dataPath = FileToKafka.class.getResource("/tianchi.csv").getFile();
        KafkaSource.sendData(kafkaProps, dataPath, false);
    }
}
