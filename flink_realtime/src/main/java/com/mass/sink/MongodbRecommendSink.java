package com.mass.sink;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.bson.Document;

import java.util.List;

public class MongodbRecommendSink
        extends RichSinkFunction<Tuple4<Integer, List<Integer>, String, Integer>> {
    static MongoClient mongoClient;
    static MongoDatabase database;
    static MongoCollection<Document> recCollection;

    @Override
    public void open(Configuration parameters) {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        database = mongoClient.getDatabase("flink-rl");
        recCollection = database.getCollection("recommendResults");
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public void invoke(Tuple4<Integer, List<Integer>, String, Integer> value, Context context) {
        Document recDoc = new Document()
                .append("user", value.f0)
                .append("itemRec", value.f1)
                .append("time", value.f2)
                .append("lastReward", value.f3);
        recCollection.insertOne(recDoc);
    }
}


