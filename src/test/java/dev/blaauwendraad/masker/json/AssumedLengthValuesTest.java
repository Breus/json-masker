package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Test;

/**
 * The test suite covers masking of values whose length is assumed by the masker: "true", "false" and "null"
 */
class AssumedLengthValuesTest {
    @Test
    void shouldMaskConstantSizeValues() {
        JsonMaskingConfig config = JsonMaskingConfig.builder().maskKeys("mask").build();
        JsonMasker jsonMasker = JsonMasker.getMasker(config);

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, "{\"mask\":false}", "{\"mask\":\"&&&\"}");
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, "{\"mask\":true}", "{\"mask\":\"&&&\"}");
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, "{\"mask\":null}", "{\"mask\":null}");
    }
    @Test
    void shouldAllowConstantSizeValuesInMaskMode() {
        JsonMaskingConfig config = JsonMaskingConfig.builder().allowKeys("mask").build();
        JsonMasker jsonMasker = JsonMasker.getMasker(config);

        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, "{\"mask\":false}", "{\"mask\":false}");
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, "{\"mask\":true}", "{\"mask\":true}");
        JsonMaskerTestUtil.assertJsonMaskerApiEquivalence(jsonMasker, "{\"mask\":null}", "{\"mask\":null}");
    }
}
