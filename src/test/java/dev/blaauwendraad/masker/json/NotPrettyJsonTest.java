package dev.blaauwendraad.masker.json;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;


class NotPrettyJsonTest {

    @ParameterizedTest
    @MethodSource("notPrettyJson")
    void notPrettyJsonTest(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(), testInstance.input(),
                testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> notPrettyJson() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("targetKey"));
        return Stream.of(
                new JsonMaskerTestInstance("""
                        {
                        "key":              "value",  \s
                        "targetKey1"
                                               \s
                        :
                                               \s
                                               \s
                                     {
                                        "targetKey":     "value"
                                     }  ,    \s
                                  "targetKey"   :
                                               \s
                            123
                        }
                        """, """
                        {
                        "key":              "value",  \s
                        "targetKey1"
                                               \s
                        :
                                               \s
                                               \s
                                     {
                                        "targetKey":     "***"
                                     }  ,    \s
                                  "targetKey"   :
                                               \s
                            "###"
                        }
                        """, jsonMasker),
                new JsonMaskerTestInstance("""
                         {
                          "targetKey2" : {
                            "targetKey3" : {     }
                          },
                          "khb\\u0007 " : true,
                          "\\u001C\\u000F" : true,
                          "=E\\u0018Xi=" : {
                            ":" : "\\u000F\\u0017\\u0017\\u000Bs\\b\\u0014X",
                            "targetKey2" : [ {    }   ],
                            "targetKey4" : "kA=Đ-"
                          },
                          "targetKey1": { }}
                        """, """
                         {
                          "targetKey2" : {
                            "targetKey3" : {     }
                          },
                          "khb\\u0007 " : true,
                          "\\u001C\\u000F" : true,
                          "=E\\u0018Xi=" : {
                            ":" : "\\u000F\\u0017\\u0017\\u000Bs\\b\\u0014X",
                            "targetKey2" : [ {    }   ],
                            "targetKey4" : "kA=Đ-"
                          },
                          "targetKey1": { }}
                        """, jsonMasker),
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