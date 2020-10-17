package com.mass.task;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Collector;

import java.util.Arrays;

public class ReadFromFile {
    public static void main(String[] args) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();

        // DataStream<RecordEntity> stream = env.addSource(new CustomFileSource());
        // String dataPath = ReadFromFile.class.getResource("/tianchi.csv").getFile();
        String dataPath = Thread.currentThread().getContextClassLoader().getResource("tianchi.csv").getFile();
        DataStream<String> stream = env.readTextFile(dataPath);
        DataStream<Tuple2<Integer, String>> result = stream.flatMap(
                new FlatMapFunction<String, Tuple2<Integer, String>>() {
            @Override
            public void flatMap(String value, Collector<Tuple2<Integer, String>> out) {
                String[] splitted = value.split(",");
                boolean first = false;
                try {
                    Integer.parseInt(splitted[0]);
                } catch (NumberFormatException e) {
                    System.out.println("skip header line...");
                    first = true;
                }
                if (!first) {
                    String ratings = String.join(",", Arrays.copyOfRange(splitted, 4, 5));
                    out.collect(new Tuple2<>(Integer.valueOf(splitted[0]), ratings));
                }
            }
        }).keyBy(0);

        result.print();
        env.execute();
    }
}
