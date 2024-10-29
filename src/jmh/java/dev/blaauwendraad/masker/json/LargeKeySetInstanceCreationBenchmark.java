package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.util.JsonStringCharacters;
import dev.blaauwendraad.masker.randomgen.RandomJsonGenerator;

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
import org.openjdk.jmh.annotations.Warmup;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Warmup(iterations = 1)
@Fork(value = 1)
@Measurement(iterations = 1)
@OutputTimeUnit(TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
public class LargeKeySetInstanceCreationBenchmark {

    @org.openjdk.jmh.annotations.State(Scope.Thread)
    @NullUnmarked
    public static class State {
        @Param({"100", "1000", "10000"})
        int numberOfTargetKeys;

        @Param({"10", "100", "1000"})
        int keyLength;

        Set<String> targetKeys = new HashSet<>();

        @Setup
        public synchronized void setup() throws IOException {
            Random random = new Random(BenchmarkUtils.STATIC_RANDOM_SEED);
            List<Character> characters =
                    JsonStringCharacters.mergeCharSets(
                                    JsonStringCharacters.getPrintableAsciiCharacters(),
                                    JsonStringCharacters.getUnicodeControlCharacters(),
                                    JsonStringCharacters.getRandomPrintableUnicodeCharacters())
                            .stream()
                            .toList();
            for (int i = 0; i < numberOfTargetKeys; i++) {
                targetKeys.add(getRandomString(random, keyLength, characters));
            }
        }

        private String getRandomString(Random random, int length, List<Character> allowedCharacters) {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < length; i++) {
                Character randomCharacter = allowedCharacters.get(random.nextInt(allowedCharacters.size()));
                if (randomCharacter == '\\') {
                    // escape the escape character
                    sb.append(randomCharacter);
                }
                sb.append(randomCharacter);
            }
            return sb.toString();
        }
    }

    @Benchmark
    public JsonMasker jsonMasker(State state) {
        return JsonMasker.getMasker(state.targetKeys);
    }
}
