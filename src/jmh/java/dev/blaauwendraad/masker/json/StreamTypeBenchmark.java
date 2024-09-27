package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.databind.JsonNode;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.jspecify.annotations.NullUnmarked;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1)
@Fork(value = 1)
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class StreamTypeBenchmark {

    static final String INPUT_FILE_STREAM_NAME = "input.json";
    static final String OUTPUT_FILE_STREAM_NAME = "output.json";

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @NullUnmarked
    public static class State {
        @Param({"ByteArrayStream", "FileStream"})
        String streamInputType;
        @Param({"ByteArrayStream", "FileStream"})
        String streamOutputType;
        @Param({"10mb"})
        String jsonSize;

        byte[] json;

        private JsonMasker jsonMasker;

        @Setup
        public synchronized void setup() throws IOException {
            // prepare a json
            Set<String> targetKeys = BenchmarkUtils.getTargetKeys(20);
            JsonNode jsonNode = BenchmarkUtils.randomJson(targetKeys, jsonSize, "unicode", 0.1);
            json = jsonNode.toString().getBytes(StandardCharsets.UTF_8);

            // prepare an input file for FileStreams
            try (FileWriter inputFileWriter = new FileWriter(INPUT_FILE_STREAM_NAME)) {
                inputFileWriter.write(new String(json, StandardCharsets.UTF_8));
                inputFileWriter.flush();
            }

            // create a masker
            JsonMaskingConfig.Builder builder = JsonMaskingConfig.builder();
            builder.maskJsonPaths(BenchmarkUtils.collectJsonPaths(jsonNode, targetKeys));

            jsonMasker = JsonMasker.getMasker(builder.build());
        }

        @TearDown
        public synchronized void tearDown() throws IOException {
            Files.deleteIfExists(Path.of(INPUT_FILE_STREAM_NAME));
            Files.deleteIfExists(Path.of(OUTPUT_FILE_STREAM_NAME));
        }
    }

    private InputStream createInputStream(byte[] json, String inputStreamType) throws IOException {
        return switch (inputStreamType) {
            case "ByteArrayStream" -> new ByteArrayInputStream(json);
            case "FileStream" -> new FileInputStream(INPUT_FILE_STREAM_NAME);
            default -> throw new IllegalArgumentException("Unknown stream type");
        };
    }

    private OutputStream createOutputStream(String outputStreamType) throws IOException {
        return switch (outputStreamType) {
            case "ByteArrayStream" -> new ByteArrayOutputStream();
            case "FileStream" -> new FileOutputStream(OUTPUT_FILE_STREAM_NAME);
            default -> throw new IllegalArgumentException("Unknown stream type");
        };
    }

    @Benchmark
    public void jsonMaskerStreams(State state) throws IOException {
        try (InputStream inputStream = createInputStream(state.json, state.streamInputType);
             OutputStream outputStream = createOutputStream(state.streamOutputType)) {
            state.jsonMasker.mask(inputStream, outputStream);
        }
    }
}
