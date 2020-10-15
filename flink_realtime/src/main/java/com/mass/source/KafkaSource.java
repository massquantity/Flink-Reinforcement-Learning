package com.mass.source;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.clients.producer.Callback;

import java.io.*;
import java.util.Properties;

public class KafkaSource {
    public static void sendData(Properties kafkaProps, String dataPath, Boolean header)
            throws IOException, InterruptedException {
        KafkaProducer<String, String> producer = new KafkaProducer<>(kafkaProps);
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(dataPath)));
        String temp;
        int i = 0;
        while ((temp = br.readLine()) != null) {
            i++;
            if (header && i == 1) continue;  // skip header line
            String[] splitted = temp.split(",");
            String[] record = {splitted[0], splitted[1], splitted[3]};
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

        br.close();
        producer.close();
    }
}
