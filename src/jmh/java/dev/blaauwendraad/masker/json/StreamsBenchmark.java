package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import dev.blaauwendraad.masker.json.util.JsonPathTestUtils;
import org.jspecify.annotations.NullUnmarked;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Level;
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
import java.io.Closeable;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1, time = 3)
@Fork(value = 1)
@Measurement(iterations = 1, time = 3)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class StreamsBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @NullUnmarked
    public static class State {
        static final String INPUT_FILE_STREAM_NAME = "input.json";
        static final String OUTPUT_FILE_STREAM_NAME = "output.json";

        @Param({"ByteArrayStream", "FileStream", "PipedStream"})
        String streamInputType;
        @Param({"ByteArrayStream", "FileStream", "PipedStream"})
        String streamOutputType;
        @Param({"2mb"})
        String jsonSize;
        @Param({"unicode"})
        String characters;
        @Param({"0.1"})
        double maskedKeyProbability;
        @Param({"true"})
        boolean jsonPath;

        private InputStream inputStream;
        private OutputStream outputStream;
        private Closeable pipeConsumer;
        private Closeable pipeProducer;
        private ExecutorService executor;
        private JsonMasker jsonMasker;

        @Setup(Level.Invocation) // streams need to be reset for each benchmark method execution
        public synchronized void setup() throws IOException {
            // prepare a json
            Set<String> targetKeys = BenchmarkUtils.getTargetKeys(20);
            byte[] json = BenchmarkUtils.randomJson(targetKeys, jsonSize, characters, maskedKeyProbability).getBytes(StandardCharsets.UTF_8);

            // prepare a file for FileStreams
            FileWriter inputFileWriter = new FileWriter(INPUT_FILE_STREAM_NAME);
            inputFileWriter.write(new String(json, StandardCharsets.UTF_8));
            inputFileWriter.flush();
            inputFileWriter.close();

            executor = Executors.newFixedThreadPool(2);

            // create streams
            inputStream = createInputStream(json, streamInputType);
            outputStream = createOutputStream(streamOutputType);

            JsonMaskingConfig.Builder builder = JsonMaskingConfig.builder();
            if (jsonPath) {
                builder.maskJsonPaths(JsonPathTestUtils.transformToJsonPathKeys(targetKeys, new String(json, StandardCharsets.UTF_8)));
            } else {
                builder.maskKeys(targetKeys);
            }
            jsonMasker = JsonMasker.getMasker(builder.build());
        }

        @TearDown(Level.Invocation) // streams need to be reset for each benchmark method execution
        public synchronized void tearDown() throws IOException {
            executor.shutdownNow();
            if (pipeConsumer != null) {
                pipeConsumer.close();
            }
            if (pipeProducer != null) {
                pipeProducer.close();
            }
            inputStream.close();
            outputStream.close();
            Files.deleteIfExists(Path.of(State.INPUT_FILE_STREAM_NAME));
            Files.deleteIfExists(Path.of(State.OUTPUT_FILE_STREAM_NAME));
        }

        private InputStream createInputStream(byte[] json, String inputStreamType) throws IOException {
            return switch (inputStreamType) {
                case "ByteArrayStream" -> new ByteArrayInputStream(json);
                case "FileStream" -> new FileInputStream(INPUT_FILE_STREAM_NAME);
                case "PipedStream" -> {
                    PipedOutputStream consumerPipedOutputStream = new PipedOutputStream(); // NOSONAR it is closed in the other method
                    PipedInputStream consumerPipedInputStream = new PipedInputStream(consumerPipedOutputStream); // NOSONAR it is closed in the other method
                    executor.submit(() -> {
                        try {
                            consumerPipedOutputStream.write(json);
                            consumerPipedOutputStream.flush();
                            consumerPipedOutputStream.close();
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        }
                    });
                    pipeProducer = consumerPipedOutputStream;
                    yield consumerPipedInputStream;
                }
                default -> throw new IllegalArgumentException("Unknown stream type");
            };
        }

        private OutputStream createOutputStream(String outputStreamType) throws IOException {
            return switch (outputStreamType) {
                case "ByteArrayStream" -> new ByteArrayOutputStream();
                case "FileStream" -> new FileOutputStream(OUTPUT_FILE_STREAM_NAME);
                case "PipedStream" -> {
                    PipedOutputStream consumerPipedOutputStream = new PipedOutputStream(); // NOSONAR it is closed in the other method
                    PipedInputStream consumerPipedInputStream = new PipedInputStream(consumerPipedOutputStream); // NOSONAR it is closed in the other method
                    executor.submit(() -> {
                        try {
                            while (true) {
                                consumerPipedInputStream.readAllBytes();
                                Thread.sleep(1000);
                            }
                        } catch (IOException e) {
                            throw new UncheckedIOException(e);
                        } catch (InterruptedException e) {
                            // finish the execution
                        }
                    });
                    pipeConsumer = consumerPipedInputStream;
                    yield consumerPipedOutputStream;
                }
                default -> throw new IllegalArgumentException("Unknown stream type");
            };
        }
    }

    @Benchmark
    public void jsonMaskerStreams(State state) {
        state.jsonMasker.mask(state.inputStream, state.outputStream);
    }
}
