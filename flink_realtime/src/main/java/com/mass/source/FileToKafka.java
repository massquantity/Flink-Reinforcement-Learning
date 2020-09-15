package com.mass.source;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.Callback;

import java.io.*;
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
        KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps);

        //  ClassLoader classloader = Thread.currentThread().getContextClassLoader();
        //  InputStream is = classloader.getResourceAsStream("/news_data.csv");
        String dataPath = FileToKafka.class.getResource("/news_data.csv").getFile();
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dataPath)));
        String temp;
        int i = 0;
        while ((temp = br.readLine()) != null) {
            i++;
            if (i > 1) {  // // skip header line
                String[] splitted = temp.split(",");
                String[] record = {splitted[0], splitted[4], splitted[5]};
                String csvString = String.join(",", record);
                producer.send(new ProducerRecord<>("flink-rl", "news", csvString), new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata metadata, Exception e) {
                        if (e != null) {
                            e.printStackTrace();
                        }
                    }
                });
                Thread.sleep(500L);
            }
        }

        br.close();
        producer.close();
    }
}



