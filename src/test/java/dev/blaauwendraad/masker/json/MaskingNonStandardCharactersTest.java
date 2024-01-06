package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class MaskingNonStandardCharactersTest {

    @Test
    void maskingNonStandardCharacters() {
        JsonMasker jsonMasker = JsonMasker.getMasker(Set.of("привіт", "💩"));

        assertThat(jsonMasker.mask(
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
        )).isEqualTo("""
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
                """);
    }

    @Test
    void maskingNonStandardCharactersInAllowMode() {
        JsonMasker jsonMasker = JsonMasker.getMasker(
                JsonMaskingConfig.custom(Set.of("привіт", "otherKey", "someArray"), JsonMaskingConfig.TargetKeyMode.ALLOW).build()
        );

        assertThat(jsonMasker.mask(
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
        )).isEqualTo("""
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
                """);
    }
}
