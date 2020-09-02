package com.mass.recommend.mlflow;

import com.mass.entity.UserConsumed;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.json.JSONObject;
import redis.clients.jedis.Jedis;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static com.mass.util.FormatTimestamp.format;
import static com.mass.util.TypeConvert.convertJSON;
import static com.mass.util.TypeConvert.convertString;

public class MLflowRecommender extends RichFlatMapFunction<
        UserConsumed, Tuple4<Integer, List<Integer>, String, Integer>> {
    private final int histNum;
    private final int numRec;
    private static Jedis jedis;
    private static HttpURLConnection con;
    private MapState<Integer, List<Integer>> lastRecState;

    public MLflowRecommender(int numRec, int histNum) {
        this.histNum = histNum;
        this.numRec = numRec;
    }

    @Override
    public void open(Configuration parameters) {
        jedis = new Jedis("localhost", 6379);
        MapStateDescriptor<Integer, List<Integer>> lastRecStateDesc =
                new MapStateDescriptor<>(
                        "lastRecState", TypeInformation.of(new TypeHint<Integer>() {}),
                        TypeInformation.of(new TypeHint<List<Integer>>() {}));
        lastRecState = getRuntimeContext().getMapState(lastRecStateDesc);
    }

    @Override
    public void close() {
        jedis.close();
    //    lastRecState.clear();
    }

    private void buildConnection() throws IOException {
        URL obj = new URL("http://127.0.0.1:5000/invocations");
        con = (HttpURLConnection) obj.openConnection();
        con.setRequestMethod("POST");
        con.setRequestProperty("Content-Type", "application/json; format=pandas-split");
        // con.setRequestProperty("Connection", "Keep-Alive");
        con.setConnectTimeout(5000);
        con.setReadTimeout(5000);
        con.setDoOutput(true);
    }

    private void writeOutputStream(String jsonString) throws IOException {
        DataOutputStream wr = new DataOutputStream(con.getOutputStream());
        wr.write(jsonString.getBytes(StandardCharsets.UTF_8));
        wr.flush();
        wr.close();
    }

    @Override
    public void flatMap(UserConsumed value,
                        Collector<Tuple4<Integer, List<Integer>, String, Integer>> out)
            throws Exception {

        buildConnection();
        int userId = value.userId;
        List<Integer> items = value.items;
        long timestamp = value.windowEnd;
        String time = format(timestamp);

        if (items.size() == this.histNum) {
            String jsonString = convertString(items);
            writeOutputStream(jsonString);
            int responseCode = con.getResponseCode();
            System.out.println("Posted parameters : " + jsonString);
            System.out.println("Response Code : " + responseCode);

            List<Integer> recommend = new ArrayList<>();
            String printOut;
            if (responseCode == 200) {
                BufferedReader br = new BufferedReader(new InputStreamReader(con.getInputStream()));
                String inputLine;
                StringBuilder message = new StringBuilder();
                while ((inputLine = br.readLine()) != null) {
                    message.append(inputLine);
                }
                br.close();
                con.disconnect();

                JSONObject res = convertJSON(message.toString());
                for (int i = 0; i < res.length(); i++) {
                    recommend.add(res.getInt(String.format("%s", i)));
                }

                int hotRecommend = numRec - recommend.size();
                if (hotRecommend > 0) {
                    for (int i = 1; i < hotRecommend + 1; i++) {
                        String item = jedis.get(String.valueOf(i));
                        if (null != item) {
                            recommend.add(Integer.parseInt(item));
                        }
                    }
                }
                printOut = String.format("user: %d, recommend(%d) + hot(%d): %s, time: %s",
                        userId, recommend.size(), hotRecommend, recommend, time);
            } else {
                for (int i = 1; i < numRec + 1; i++) {
                    String item = jedis.get(String.valueOf(i));
                    if (null != item) {
                        recommend.add(Integer.parseInt(item));
                    }
                }
                printOut = String.format("user: %d, bad request, recommend hot(%d): %s, " +
                        "time: %s", userId, recommend.size(), recommend, time);
            }

            System.out.println(printOut);
            System.out.println(StringUtils.repeat("=", 60));

            int lastReward;
            List<Integer> lastRecommend = lastRecState.get(userId);
            if (lastRecommend != null) {
                lastReward = 0;
                for (int rec : lastRecommend) {
                    for (int click : items) {   // map item index
                        if (rec == click) {
                            lastReward++;
                        }
                    }
                }
            } else {
                lastReward = -1;
            }

            lastRecState.put(userId, recommend);
            out.collect(Tuple4.of(userId, recommend, time, lastReward));
        }
    }
}


