package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Set;

class MaskingNonStandardCharactersTest {

    @Test
    void maskingNonStandardCharacters() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("привіт", "💩"));

        Assertions.assertEquals(
                """
                {
                  "привіт": "*****",
                  "otherKey": null,
                  "💩": "************",
                  "someObject": {
                    "привіт": "*****",
                    "otherKey": null,
                    "💩": {
                        "💩": "************"
                    }
                  },
                  "someArray": [
                    "💩",
                    "💩".
                    {
                      "привіт": "*****",
                      "otherKey": null,
                      "💩": {
                          "💩": "************"
                      }
                    }
                  ]
                }
                """,
                jsonMasker.mask(
                        """
                        {
                          "привіт": "hello",
                          "otherKey": null,
                          "💩": "shit happens",
                          "someObject": {
                            "привіт": "hello",
                            "otherKey": null,
                            "💩": {
                                "💩": "shit happens"
                            }
                          },
                          "someArray": [
                            "💩",
                            "💩".
                            {
                              "привіт": "hello",
                              "otherKey": null,
                              "💩": {
                                  "💩": "shit happens"
                              }
                            }
                          ]
                        }
                        """
                )
        );
    }

    @Test
    void maskingNonStandardCharactersInAllowMode() {
        JsonMasker jsonMasker = JsonMasker.getMasker(
                JsonMaskingConfig.custom(Set.of("привіт", "otherKey", "someArray"), JsonMaskingConfig.TargetKeyMode.ALLOW).build()
        );

        Assertions.assertEquals(
                """
                {
                  "привіт": "hello",
                  "otherKey": null,
                  "💩": "************",
                  "someObject": {
                    "привіт": "hello",
                    "otherKey": null,
                    "💩": {
                        "💩": "************"
                    }
                  },
                  "someArray": [
                    "💩",
                    "💩".
                    {
                      "привіт": "hello",
                      "otherKey": null,
                      "💩": {
                          "💩": "shit happens"
                      }
                    }
                  ]
                }
                """,
                jsonMasker.mask(
                        """
                        {
                          "привіт": "hello",
                          "otherKey": null,
                          "💩": "shit happens",
                          "someObject": {
                            "привіт": "hello",
                            "otherKey": null,
                            "💩": {
                                "💩": "shit happens"
                            }
                          },
                          "someArray": [
                            "💩",
                            "💩".
                            {
                              "привіт": "hello",
                              "otherKey": null,
                              "💩": {
                                  "💩": "shit happens"
                              }
                            }
                          ]
                        }
                        """
                )
        );
    }
}
