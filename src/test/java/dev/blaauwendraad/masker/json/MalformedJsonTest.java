package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskerAlgorithmType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

class MalformedJsonTest {
    @ParameterizedTest
    @MethodSource("malformedFile")
    void malformedJsonFile(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), testInstance.jsonMasker().mask(testInstance.input()));
    }

    @ParameterizedTest
    @MethodSource("malformedJson")
    void malformedJson(JsonMaskerTestInstance testInstance) {
        Assertions.assertEquals(testInstance.expectedOutput(), testInstance.jsonMasker().mask(testInstance.input()));
    }

    @Test
    void name() {
        JsonMasker jsonMasker = JsonMasker.getMasker("targetKey");
        String json = """
                        [ {
                          "targetKey" : {
                            "?„+Uo?\\b" : {
                              "\\"" : [ "aaa" ]
                            }
                          }
                        }, "R\\u0010f\\u0010" ]
                """;
        Assertions.assertEquals(json, jsonMasker.mask(json));
    }

    private static Stream<JsonMaskerTestInstance> malformedFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile(
                "test-malformed.json",
                Set.of(JsonMaskerAlgorithmType.values())
        ).stream();
    }

    private static Stream<JsonMaskerTestInstance> malformedJson() {
        return Stream.of(new JsonMaskerTestInstance("""
                                                    {  "hello":   "hello"}
                                                    """, """
                                                         {  "hello":   "*****"}
                                                         """, JsonMasker.getMasker("hello")));
    }
}