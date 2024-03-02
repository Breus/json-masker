package dev.blaauwendraad.masker.json;

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

    private static Stream<JsonMaskerTestInstance> notPrettyFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile(
                "test-not-pretty.json"
        ).stream();
    }

    private static Stream<JsonMaskerTestInstance> notPrettyJson() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("targetKey"));
        return Stream.of(
                new JsonMaskerTestInstance("""
                        {  "targetKey":   "hello"}
                        """, """
                        {  "targetKey":   "***"}
                        """, jsonMasker),
                new JsonMaskerTestInstance("""
                        [[[{           "targetKey":            "hello"}], {"targetKey2": "hello", "targetKey": "hi     hi"}]]
                        """, """
                        [[[{           "targetKey":            "***"}], {"targetKey2": "hello", "targetKey": "***"}]]
                        """, jsonMasker),
                new JsonMaskerTestInstance("""
                                [ {
                                  "targetKey" : {
                                    "?„+Uo?\\b" : {
                                      "\\"" : [ "aaa" ]
                                    }
                                  }
                                }, "R\\u0010f\\u0010" ]
                        """, """
                                [ {
                                  "targetKey" : {
                                    "?„+Uo?\\b" : {
                                      "\\"" : [ "***" ]
                                    }
                                  }
                                }, "R\\u0010f\\u0010" ]
                        """, jsonMasker),
                new JsonMaskerTestInstance("""
                         { }
                        """, """
                         { }
                        """, jsonMasker),
                new JsonMaskerTestInstance("""
                        {
                        "targetKey":"value",
                        "array":[1,2,3],
                        "nestedObject":{
                        "innerKey":"innerValue",
                        "targetKey":"nestedTarget"
                        }
                        }
                        """, """
                        {
                        "targetKey":"***",
                        "array":[1,2,3],
                        "nestedObject":{
                        "innerKey":"innerValue",
                        "targetKey":"***"
                        }
                        }
                        """, jsonMasker));
    }
}