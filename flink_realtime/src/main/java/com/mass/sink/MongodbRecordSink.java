package com.mass.sink;

import com.mass.entity.RecordEntity;
import com.mongodb.client.*;
import com.mongodb.client.model.Updates;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.sink.RichSinkFunction;
import org.bson.Document;

import java.util.ArrayList;

import static com.mass.util.FormatTimestamp.format;
import static com.mongodb.client.model.Filters.eq;

public class MongodbRecordSink extends RichSinkFunction<RecordEntity> {
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> recordCollection;
    private static MongoCollection<Document> consumedCollection;

    @Override
    public void open(Configuration parameters) {
        mongoClient = MongoClients.create("mongodb://localhost:27017");
        database = mongoClient.getDatabase("flink-rl");
        recordCollection = database.getCollection("records");
        consumedCollection = database.getCollection("userConsumed");
    }

    @Override
    public void close() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    @Override
    public void invoke(RecordEntity value, Context context) {
        Document recordDoc = new Document()
                .append("user", value.getUserId())
                .append("item", value.getItemId())
                .append("time", format(value.getTime()));
        recordCollection.insertOne(recordDoc);

        int userId = value.getUserId();
        int itemId = value.getItemId();
        long timestamp = value.getTime();
        String time = format(timestamp);
        System.out.println(time);
        FindIterable fe = consumedCollection.find(eq("user", userId));
        if (null == fe.first()) {
            Document consumedDoc = new Document();
            consumedDoc.append("user", userId);
            consumedDoc.append("consumed", new ArrayList<Integer>());
            consumedDoc.append("time", new ArrayList<String>());
            consumedCollection.insertOne(consumedDoc);
        } else {
            consumedCollection.updateOne(eq("user", userId), Updates.addToSet("consumed", itemId));
            consumedCollection.updateOne(eq("user", userId), Updates.addToSet("time", time));
        }
    }
}

