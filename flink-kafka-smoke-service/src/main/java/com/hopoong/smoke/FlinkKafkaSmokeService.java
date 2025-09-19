package com.hopoong.smoke;

import org.apache.flink.api.common.functions.AggregateFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.connector.kafka.source.enumerator.initializer.OffsetsInitializer;
import org.apache.flink.connector.kafka.source.reader.deserializer.KafkaRecordDeserializationSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.PrintSinkFunction;
import org.apache.flink.streaming.api.functions.windowing.ProcessWindowFunction;
import org.apache.flink.streaming.api.windowing.assigners.TumblingProcessingTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.streaming.api.windowing.windows.TimeWindow;
import org.apache.flink.util.Collector;
import org.apache.kafka.clients.consumer.ConsumerRecord;

/**
 * 두 Kafka 토픽(user-action-events / user-action-events.shadow)의 레코드 수를
 * Processing Time 기준 1분 텀블링 윈도우로 카운트하여 콘솔에 출력.
 * - 메시지 바이트/스키마/타임스탬프는 전혀 읽지 않음(값이 null이어도 OK)
 * - Avro/Schema Registry 의존성 없음
 */
public class FlinkKafkaSmokeService {

    public static final String TOPIC_PRIMARY = "user-action-events";
    public static final String TOPIC_SHADOW  = "user-action-events.shadow";

    public static void main(String[] args) throws Exception {
        ParameterTool params = ParameterTool.fromArgs(args);
        final String bootstrap  = params.get("bootstrap", "localhost:9092");
        final int parallelism   = params.getInt("p", 1);

        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setParallelism(parallelism);

        // 1) Kafka Source: 값 파싱 없이 "레코드가 왔다"는 사실만 이벤트로 변환
        KafkaSource<SideEvent> primarySource = KafkaSource.<SideEvent>builder()
                .setBootstrapServers(bootstrap)
                .setTopics(TOPIC_PRIMARY)
                .setGroupId("minute-counter-primary")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setDeserializer(new CountEveryRecordDeser("PRIMARY"))
                .build();

        KafkaSource<SideEvent> shadowSource = KafkaSource.<SideEvent>builder()
                .setBootstrapServers(bootstrap)
                .setTopics(TOPIC_SHADOW)
                .setGroupId("minute-counter-shadow")
                .setStartingOffsets(OffsetsInitializer.latest())
                .setDeserializer(new CountEveryRecordDeser("SHADOW"))
                .build();

        // Processing Time 윈도우 → 워터마크 불필요
        DataStream<SideEvent> primary = env.fromSource(primarySource, org.apache.flink.api.common.eventtime.WatermarkStrategy.noWatermarks(), "primary");
        DataStream<SideEvent> shadow  = env.fromSource(shadowSource,  org.apache.flink.api.common.eventtime.WatermarkStrategy.noWatermarks(), "shadow");

        // 2) side(PRIMARY/SHADOW)로 keyBy → 1분 텀블링(Processing Time) → 카운트
        DataStream<CountResult> perMinuteCounts =
                primary.union(shadow)
                        .keyBy(e -> e.side)
                        .window(TumblingProcessingTimeWindows.of(Time.minutes(1)))
                        .aggregate(new CountAgg(), new WindowResult());

        // 3) 출력
        perMinuteCounts.addSink(new PrintSinkFunction<>()).name("print-per-minute-counts");

        env.execute("user-action-events minute counter (simple)");
    }

    // ===== 모델 =====
    public static class SideEvent {
        public String side; // "PRIMARY" | "SHADOW"
        public SideEvent() {}
        public SideEvent(String side) { this.side = side; }
        @Override public String toString() { return "SideEvent{side='" + side + "'}"; }
    }

    public static class CountResult {
        public String side;
        public long windowStart;
        public long windowEnd;
        public long count;
        public CountResult() {}
        public CountResult(String side, long windowStart, long windowEnd, long count) {
            this.side = side; this.windowStart = windowStart; this.windowEnd = windowEnd; this.count = count;
        }
        @Override public String toString() {
            return "CountResult{side=" + side +
                    ", windowStart=" + windowStart +
                    ", windowEnd=" + windowEnd +
                    ", count=" + count + "}";
        }
    }

    // ===== 값/헤더를 전혀 읽지 않고, 레코드 1건당 SideEvent 1건을 방출하는 역직렬화기 =====
    public static final class CountEveryRecordDeser implements KafkaRecordDeserializationSchema<SideEvent> {
        private final String side;
        public CountEveryRecordDeser(String side) { this.side = side; }

        @Override
        public void deserialize(ConsumerRecord<byte[], byte[]> record, Collector<SideEvent> out) {
            // value가 null이어도 OK. 파싱 없이 무조건 1건 방출
            out.collect(new SideEvent(side));
        }

        @Override
        public TypeInformation<SideEvent> getProducedType() {
            return TypeInformation.of(SideEvent.class);
        }
    }

    // ===== 윈도우 집계 =====
    public static final class CountAgg implements AggregateFunction<SideEvent, Long, Long> {
        @Override public Long createAccumulator() { return 0L; }
        @Override public Long add(SideEvent value, Long acc) { return acc + 1; }
        @Override public Long getResult(Long acc) { return acc; }
        @Override public Long merge(Long a, Long b) { return a + b; }
    }

    public static final class WindowResult extends ProcessWindowFunction<Long, CountResult, String, TimeWindow> {
        @Override
        public void process(String side, Context ctx, Iterable<Long> elements, Collector<CountResult> out) {
            long cnt = elements.iterator().next();
            TimeWindow w = ctx.window();
            out.collect(new CountResult(side, w.getStart(), w.getEnd(), cnt));
        }
    }
}
