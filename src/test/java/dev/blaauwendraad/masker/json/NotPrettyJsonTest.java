package dev.blaauwendraad.masker.json;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

class NotPrettyJsonTest {
    @ParameterizedTest
    @MethodSource("notPrettyFile")
    void notPrettyFileTest(JsonMaskerTestInstance testInstance) {
        assertThat(testInstance.jsonMasker().mask(testInstance.input())).isEqualTo(testInstance.expectedOutput());
    }

    @ParameterizedTest
    @MethodSource("notPrettyJson")
    void notPrettyJsonTest(JsonMaskerTestInstance testInstance) {
        assertThat(testInstance.jsonMasker().mask(testInstance.input())).isEqualTo(testInstance.expectedOutput());
    }

    @Test
    void name() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("targetKey"));
        String json = """
                        [ {
                          "targetKey" : {
                            "?„+Uo?\\b" : {
                              "\\"" : [ "aaa" ]
                            }
                          }
                        }, "R\\u0010f\\u0010" ]
                """;
        assertThat(jsonMasker.mask(json)).isEqualTo(json);
    }

    private static Stream<JsonMaskerTestInstance> notPrettyFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile(
                "test-not-pretty.json"
        ).stream();
    }

    private static Stream<JsonMaskerTestInstance> notPrettyJson() {
        return Stream.of(new JsonMaskerTestInstance("""
                {  "hello":   "hello"}
                """, """
                {  "hello":   "***"}
                """, JsonMasker.getMasker(Set.of("hello"))));
    }
}