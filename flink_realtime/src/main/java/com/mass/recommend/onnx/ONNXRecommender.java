package com.mass.recommend.onnx;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.OrtSession.Result;
import com.mass.entity.UserConsumed;
import com.mass.feature.BuildFeature;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.state.ListState;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapState;
import org.apache.flink.api.common.typeinfo.TypeHint;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple4;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.functions.KeyedProcessFunction;
import org.apache.flink.util.Collector;
import redis.clients.jedis.Jedis;

import java.util.*;

import static com.mass.util.FormatTimestamp.format;
import static com.mass.util.TypeConvert.convertEmbedding;

public class ONNXRecommender extends
        KeyedProcessFunction<Integer, UserConsumed, Tuple4<Integer, List<Integer>, String, Integer>> {

    private final int histNum;
    private final int numRec;
    private static Jedis jedis;
    private static OrtEnvironment env;
    private static OrtSession session;
    private ListState<Integer> lastRecState;
    private BuildFeature stateBuilder = new BuildFeature();

    public ONNXRecommender(int numRec, int histNum) {
        this.histNum = histNum;
        this.numRec = numRec;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        jedis = new Jedis("localhost", 6379);
        env = OrtEnvironment.getEnvironment();
        String modelPath = ONNXRecommender.class.getClassLoader().getResource("model.onnx").getFile();
        session = env.createSession(modelPath, new OrtSession.SessionOptions());
        ListStateDescriptor<Integer> lastRecStateDesc = new ListStateDescriptor<Integer>("lastRecState",
                TypeInformation.of(new TypeHint<Integer>() {}));
        lastRecState = getRuntimeContext().getListState(lastRecStateDesc);
        stateBuilder.openFile();
    }

    @Override
    public void close() throws Exception {
        jedis.close();
        session.close();
        env.close();
        lastRecState.clear();
        stateBuilder.close();
    }

    @Override
    public void processElement(UserConsumed value, Context ctx,
                               Collector<Tuple4<Integer, List<Integer>, String, Integer>> out) throws Exception {
        int userId = value.userId;
        List<Integer> items = value.items;
        long timestamp = value.windowEnd;
        String time = format(timestamp);

        List<Integer> recommend;
        String printOut;
        if (items.size() == this.histNum) {
            recommend = getRec(userId, items);
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
            recommend = new ArrayList<>();
            for (int i = 1; i < numRec + 1; i++) {
                String item = jedis.get(String.valueOf(i));
                if (null != item) {
                    recommend.add(Integer.parseInt(item));
                }
            }
            printOut = String.format("user: %d, no history, recommend hot(%d): %s, " +
                    "time: %s", userId, recommend.size(), recommend, time);
        }

        System.out.println(printOut);
        System.out.println(StringUtils.repeat("=", 60));

        int lastReward = updateLastReward(userId, items, recommend);
        out.collect(Tuple4.of(userId, recommend, time, lastReward));
    }

    private List<Integer> getRec(int userId, List<Integer> items) throws OrtException {
        List<Float> stateEmbeds = stateBuilder.getFeatures(userId, items);
        float[][] inputData = new float[1][stateEmbeds.size()];
        for (int i = 0; i < stateEmbeds.size(); i++) {
            inputData[0][i] = stateEmbeds.get(i);
        }
    //    stateEmbeds.toArray(inputData[0]);

        String inputName = session.getInputNames().iterator().next();
        OnnxTensor dataTensor = OnnxTensor.createTensor(env, inputData);
        Result output = session.run(Collections.singletonMap(inputName, dataTensor));
        long[][] res = (long[][]) output.get(0).getValue();
        System.out.println("rec for user " + userId + ": " + Arrays.deepToString(res));

        List<Integer> recommend = new ArrayList<>();
        for (int i = 0; i < res[0].length; i++) {
            recommend.add((int) res[0][i]);
        }
        return recommend;
    }

    private int updateLastReward(int userId, List<Integer> items, List<Integer> recommend) throws Exception {
        int lastReward;
        if (lastRecState.get() != null) {
            lastReward = 0;
            for (int rec : lastRecState.get()) {
                for (int click : items) {
                    if (rec == click) {
                        lastReward++;
                    }
                }
            }
        } else {
            lastReward = -1;
        }

        lastRecState.clear();
        for (int rec : recommend) {
            lastRecState.add(rec);
        }
        return lastReward;
    }
}




