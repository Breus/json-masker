package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.stream.Stream;


final class AllowModeTest {

    @ParameterizedTest
    @MethodSource("testAllowMode")
    void targetKeyAllowMode(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(), testInstance.input(),
                testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> testAllowMode() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-allow-mode.json").stream();
    }

    @ParameterizedTest
    @MethodSource("targetKeyAllowModeNotPretty")
    void targetKeyAllowModeNotPretty(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(), testInstance.input(),
                testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> targetKeyAllowModeNotPretty() {
        JsonMasker jsonMasker = JsonMasker.getMasker(JsonMaskingConfig.builder()
                .allowKeys("allowedKey")
                .build());
        return Stream.of(
                new JsonMaskerTestInstance("""
                         [                               \s
                          {
                            "allowedKey": "yes",
                            "notAllowedKey": "hello"
                          }
                        ]
                        """, """
                         [                               \s
                          {
                            "allowedKey": "yes",
                            "notAllowedKey": "***"
                          }
                        ]
                        """, jsonMasker
                ),
                new JsonMaskerTestInstance("""
                            [
                              false,
                              "value2",
                              123,
                              false,
                              null,
                              {
                                "allowedKey": "yes",
                                "notAllowedKey": "hello"
                              },
                              "value3"
                            ]
                        """, """
                            [
                              "&&&",
                              "***",
                              "###",
                              "&&&",
                              null,
                              {
                                "allowedKey": "yes",
                                "notAllowedKey": "***"
                              },
                              "***"
                            ]
                        """, jsonMasker
                ),
                new JsonMaskerTestInstance("""
                            [
                              "value1",
                              "value2",
                              123,
                              false,
                              null,
                              {
                                "allowedKey": "yes",
                                "notAllowedKey": "hello"
                              },
                              "value3"
                            ]
                        """, """
                            [
                              "***",
                              "***",
                              "###",
                              "&&&",
                              null,
                              {
                                "allowedKey": "yes",
                                "notAllowedKey": "***"
                              },
                              "***"
                            ]
                        """, jsonMasker
                ),
                new JsonMaskerTestInstance("""
                        ["value",{}]
                        """, """
                        ["***",{}]
                        """, jsonMasker
                ),
                new JsonMaskerTestInstance("""
                        [ {     "_@":  [   [  "This is a random value",{    "allowedKey": "This is allowed" }  ]  ,    [   "This is a random value",  {
                                               \s
                                      "allowedKey":   "This is allowed" }    ] ,     {   "allowedKey":    "This is allowed" }]  },   [   {
                                     "allowedKey":  "This is allowed" }    ,  {
                                         "_+<#?[&":  [ "This is a random value",  {
                                               \s
                                     "allowedKey":  "This is allowed" }    ]   ,"<%*^&()?":   {
                                               \s
                                       "allowedKey":   "This is allowed" }    ,   "%-%[ ;=!}    )(":  {
                                               \s
                                     "allowedKey":    "This is allowed" }  , "?":    {
                                     "allowedKey": "This is allowed" }     }   ,     {   "allowedKey":    "This is allowed" }],  {
                                               \s
                                     "allowedKey":    "This is allowed" }  ,{
                                               \s
                                     "*()_$-?(": [   {
                                               \s
                                        "allowedKey":   "This is allowed" } ] ,  "%=>@^}   [  -&": {
                                     ";=":   {
                                       "allowedKey": "This is allowed" }    , "[ ^,  ^[  .*":  {
                                      "allowedKey":  "This is allowed" }    ,   "$]  {  (?%<":    {
                                       "allowedKey":    "This is allowed" } ,    "allowedKey":   "This is allowed" }  }    , {
                                      "}    =!.^$: ]":    {
                                               \s
                                         "allowedKey":  "This is allowed", "-:   ?|_!&;@": {
                                               \s
                                     "allowedKey":  "This is allowed" }     },  "allowedKey":  "This is allowed",    "+,   )>[ [ =;":  {
                                       "^@.$,  *": {   "allowedKey":  "This is allowed" }  ,     "_[  [  =": [  "This is a random value",     {
                                     "allowedKey": "This is allowed" } ],   "+>":  [   "This is a random value", {
                                        "allowedKey": "This is allowed" }    ]   }  ,   "} >+]    |_(^":   {
                                        "?}-}    -*+!":    {
                                      "allowedKey":    "This is allowed" } , "*.:   }<^{    *&%":   [   "This is a random value",   {
                                               \s
                                         "allowedKey":  "This is allowed" }    ] }     }  ] \s
                       \s""", """
                        [ {     "_@":  [   [  "***",{    "allowedKey": "This is allowed" }  ]  ,    [   "***",  {
                                               \s
                                      "allowedKey":   "This is allowed" }    ] ,     {   "allowedKey":    "This is allowed" }]  },   [   {
                                     "allowedKey":  "This is allowed" }    ,  {
                                         "_+<#?[&":  [ "***",  {
                                               \s
                                     "allowedKey":  "This is allowed" }    ]   ,"<%*^&()?":   {
                                               \s
                                       "allowedKey":   "This is allowed" }    ,   "%-%[ ;=!}    )(":  {
                                               \s
                                     "allowedKey":    "This is allowed" }  , "?":    {
                                     "allowedKey": "This is allowed" }     }   ,     {   "allowedKey":    "This is allowed" }],  {
                                               \s
                                     "allowedKey":    "This is allowed" }  ,{
                                               \s
                                     "*()_$-?(": [   {
                                               \s
                                        "allowedKey":   "This is allowed" } ] ,  "%=>@^}   [  -&": {
                                     ";=":   {
                                       "allowedKey": "This is allowed" }    , "[ ^,  ^[  .*":  {
                                      "allowedKey":  "This is allowed" }    ,   "$]  {  (?%<":    {
                                       "allowedKey":    "This is allowed" } ,    "allowedKey":   "This is allowed" }  }    , {
                                      "}    =!.^$: ]":    {
                                               \s
                                         "allowedKey":  "This is allowed", "-:   ?|_!&;@": {
                                               \s
                                     "allowedKey":  "This is allowed" }     },  "allowedKey":  "This is allowed",    "+,   )>[ [ =;":  {
                                       "^@.$,  *": {   "allowedKey":  "This is allowed" }  ,     "_[  [  =": [  "***",     {
                                     "allowedKey": "This is allowed" } ],   "+>":  [   "***", {
                                        "allowedKey": "This is allowed" }    ]   }  ,   "} >+]    |_(^":   {
                                        "?}-}    -*+!":    {
                                      "allowedKey":    "This is allowed" } , "*.:   }<^{    *&%":   [   "***",   {
                                               \s
                                         "allowedKey":  "This is allowed" }    ] }     }  ] \s
                       \s""", jsonMasker
                ),
                new JsonMaskerTestInstance("""
                        {
                                   "|":    true,   ")+*":   {
                                    "allowedKey": "Nested allowed value" }   ,"number"    :13452547 ,     ",    #":   false }
                        """,
                        """
                        {
                                   "|":    "&&&",   ")+*":   {
                                    "allowedKey": "Nested allowed value" }   ,"number"    :"###" ,     ",    #":   "&&&" }
                        """, jsonMasker
                )
        );
    }
}
