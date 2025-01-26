package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Set;
import java.util.stream.Stream;


class UnicodeCharacterTest {

    @ParameterizedTest
    @MethodSource("unicodeCharacters")
    void unicodeCharacter(JsonMaskerTestInstance testInstance) {
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(testInstance.jsonMasker(), testInstance.input(),
                testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> unicodeCharacters() {
        return Stream.of(
                new JsonMaskerTestInstance(
                        """
                       {
                         "targetKey2": {
                           "targetKey3": {}
                         },
                         "khb\\u0007 ": true,
                         "\\u001C\\u000F": true,
                         "=E\\u0018Xi=": {
                           ":": "\\u000F\\u0017\\u0017\\u000Bs\\b\\u0014X",
                           "targetKey2": [],
                           "targetKey4": "kA=Đ-"
                         }
                       }
                       """,
                        """
                       {
                         "targetKey2": {
                           "targetKey3": {}
                         },
                         "khb\\u0007 ": true,
                         "\\u001C\\u000F": true,
                         "=E\\u0018Xi=": {
                           ":": "\\u000F\\u0017\\u0017\\u000Bs\\b\\u0014X",
                           "targetKey2": [],
                           "targetKey4": "kA=Đ-"
                         }
                       }
                       """,
                        JsonMasker.getMasker(Set.of("targetKey1", "targetKey2"))),
                new JsonMaskerTestInstance(
                        """
                       {
                         "someKey": "\\u2020",
                         "otherKey": null
                       }
                       """,
                        """
                       {
                         "someKey": "***",
                         "otherKey": null
                       }
                       """,
                        JsonMasker.getMasker(Set.of("someKey"))),
                new JsonMaskerTestInstance(
                        """
                       {
                         "someKey": "a\\u2020b",
                         "otherKey": null
                       }
                       """,
                        """
                       {
                         "someKey": "***",
                         "otherKey": null
                       }
                       """,
                        JsonMasker.getMasker(Set.of("someKey"))),
                new JsonMaskerTestInstance(
                        """
                       {
                         "someKey": "a\\\\\\u2020b"
                       }
                       """,
                        """
                       {
                         "someKey": "***"
                       }
                       """,
                        JsonMasker.getMasker(Set.of("someKey"))),
                new JsonMaskerTestInstance(
                        """
                       {
                         "someKey": [
                           {
                             "someKey": "\\u0003\\u0015",
                             "otherKey": null
                           }
                         ]
                       }
                       """,
                        """
                       {
                         "someKey": [
                           {
                             "someKey": "***",
                             "otherKey": null
                           }
                         ]
                       }
                       """,
                        JsonMasker.getMasker(Set.of("someKey"))),
                new JsonMaskerTestInstance(
                        """
                       {
                         "someKey": "\\u0014"
                       }
                       """,
                        """
                       {
                         "someKey": "***"
                       }
                       """,
                        JsonMasker.getMasker(Set.of("someKey"))),
                new JsonMaskerTestInstance(
                        """
                       {
                         "someKey": "\\u0014\\u0085"
                       }
                       """,
                        """
                       {
                         "someKey": "***"
                       }
                       """,
                        JsonMasker.getMasker(Set.of("someKey"))),
                new JsonMaskerTestInstance(
                        """
                       {
                         "someKey": "\\u0085"
                       }
                       """,
                        """
                       {
                         "someKey": "***"
                       }
                       """,
                        JsonMasker.getMasker(Set.of("someKey"))),
                new JsonMaskerTestInstance(
                        """
                        {
                          "maskMe": "secret",
                          "̀": "secret",
                          "€": "secret",
                          "†": "secret",
                          "䀀": "secret",
                          "𐍈": "secret",
                          "💩": "secret",
                          "encoded": {
                            "\\u006D\\u0061\\u0073\\u006B\\u004D\\u0065": "secret",
                            "\\u0300": "secret",
                            "\\u20AC": "secret",
                            "\\u2020": "secret",
                            "\\u4000": "secret",
                            "\\uD800\\uDF48": "secret",
                            "\\uD83D\\uDCA9": "secret"
                          }
                        }
                        """,
                        """
                        {
                          "maskMe": "***",
                          "̀": "***",
                          "€": "***",
                          "†": "***",
                          "䀀": "***",
                          "𐍈": "***",
                          "💩": "***",
                          "encoded": {
                            "\\u006D\\u0061\\u0073\\u006B\\u004D\\u0065": "***",
                            "\\u0300": "***",
                            "\\u20AC": "***",
                            "\\u2020": "***",
                            "\\u4000": "***",
                            "\\uD800\\uDF48": "***",
                            "\\uD83D\\uDCA9": "***"
                          }
                        }
                        """,
                        JsonMasker.getMasker(Set.of("maskMe", "̀", "�", "€", "†", "䀀", "𐍈", "💩"))),
                new JsonMaskerTestInstance(
                        """
                        {
                          "maskMe": "secret",
                          "̀": "secret",
                          "€": "secret",
                          "†": "secret",
                          "䀀": "secret",
                          "𐍈": "secret",
                          "💩": "secret",
                          "\\u0065\\u006e\\u0063\\u006f\\u0064\\u0065\\u0064": {
                            "\\u006D\\u0061\\u0073\\u006B\\u004D\\u0065": "secret",
                            "\\u0300": "secret",
                            "\\u20AC": "secret",
                            "\\u2020": "secret",
                            "\\u4000": "secret",
                            "\\uD800\\uDF48": "secret",
                            "\\uD83D\\uDCA9": "secret"
                          }
                        }
                        """,
                        """
                        {
                          "maskMe": "***",
                          "̀": "***",
                          "€": "***",
                          "†": "***",
                          "䀀": "***",
                          "𐍈": "***",
                          "💩": "***",
                          "\\u0065\\u006e\\u0063\\u006f\\u0064\\u0065\\u0064": {
                            "\\u006D\\u0061\\u0073\\u006B\\u004D\\u0065": "***",
                            "\\u0300": "***",
                            "\\u20AC": "***",
                            "\\u2020": "***",
                            "\\u4000": "***",
                            "\\uD800\\uDF48": "***",
                            "\\uD83D\\uDCA9": "***"
                          }
                        }
                        """,
                        JsonMasker.getMasker(
                                JsonMaskingConfig.builder()
                                        .maskJsonPaths(
                                                "$.maskMe",
                                                "$.encoded.maskMe",
                                                "$.̀",
                                                "$.encoded.̀",
                                                "$.�",
                                                "$.encoded.�",
                                                "$.€",
                                                "$.encoded.€",
                                                "$.†",
                                                "$.encoded.†",
                                                "$.䀀",
                                                "$.encoded.䀀",
                                                "$.𐍈",
                                                "$.encoded.𐍈",
                                                "$.💩",
                                                "$.encoded.💩"
                                        )
                                        .build())),
                new JsonMaskerTestInstance(
                        """
                       {
                         "\\u0300": "1st byte mismatch",
                         "\\u00AA": "2nd byte mismatch",
                         "\\u00A9": "secret"
                       }
                       """,
                        """
                       {
                         "\\u0300": "1st byte mismatch",
                         "\\u00AA": "2nd byte mismatch",
                         "\\u00A9": "***"
                       }
                       """,
                        JsonMasker.getMasker(Set.of("©"))),
                new JsonMaskerTestInstance(
                        """
                        {
                          "\\u0800": "1st byte mismatch",
                          "\\u2182": "2nd byte mismatch",
                          "\\u20AD": "3rd byte mismatch",
                          "\\u20AC": "secret"
                        }
                        """,
                        """
                        {
                          "\\u0800": "1st byte mismatch",
                          "\\u2182": "2nd byte mismatch",
                          "\\u20AD": "3rd byte mismatch",
                          "\\u20AC": "***"
                        }
                        """,
                        JsonMasker.getMasker(Set.of("€"))),
                new JsonMaskerTestInstance(
                        """
                        {
                          "\\uDB3D\\uDCA9": "1st byte mismatch",
                          "\\uD84D\\uDCA8": "2nd byte mismatch",
                          "\\uD83C\\uDCA9": "3rd byte mismatch",
                          "\\uD83D\\uDCA8": "4th byte mismatch",
                          "\\uD83D\\uDCA9": "secret"
                        }
                        """,
                        """
                        {
                          "\\uDB3D\\uDCA9": "1st byte mismatch",
                          "\\uD84D\\uDCA8": "2nd byte mismatch",
                          "\\uD83C\\uDCA9": "3rd byte mismatch",
                          "\\uD83D\\uDCA8": "4th byte mismatch",
                          "\\uD83D\\uDCA9": "***"
                        }
                        """,
                        JsonMasker.getMasker(Set.of("💩")))
        );
    }
}
