package com.mass.source;

import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.source.RichSourceFunction;
import org.apache.flink.streaming.api.functions.source.SourceFunction;
import org.apache.flink.streaming.api.functions.source.RichParallelSourceFunction;
import com.mass.entity.RecordEntity;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;

public class CustomFileSource extends RichSourceFunction<RecordEntity> {

    private BufferedReader br;
    private Boolean header;
    private String filePath;

    public CustomFileSource(String filePath, Boolean header) {
        this.filePath = filePath;
        this.header = header;
    }

    @Override
    public void open(Configuration parameters) throws IOException {
        String dataPath = CustomFileSource.class.getResource(filePath).getFile();
        // String dataPath = CustomFileSource.class.getClassLoader().getResource("path").getFile();
        br = new BufferedReader(new FileReader(dataPath));
    }

    @Override
    public void close() throws IOException {
        br.close();
    }

    @Override
    public void run(SourceContext<RecordEntity> ctx) throws IOException, InterruptedException {
        String temp;
        int i = 0;
        while ((temp = br.readLine()) != null) {
            i++;
            if (!header && i == 1) continue;  // skip header line
            String[] line = temp.split(",");
            int userId = Integer.valueOf(line[0]);
            int itemId = Integer.valueOf(line[4]);
            long time = Long.valueOf(line[5]);
            ctx.collect(new RecordEntity(userId, itemId, time));
            Thread.sleep(2000L);
        }
    }

    @Override
    public void cancel() { }
}


