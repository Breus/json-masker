package dev.blaauwendraad.masker.json;

import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.util.Set;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

public class ArrayInputTest {
    @ParameterizedTest
    @MethodSource("testArrayInputFile")
    void arrayInput(JsonMaskerTestInstance testInstance) {
        assertThat(testInstance.jsonMasker().mask(testInstance.input())).isEqualTo(testInstance.expectedOutput());
    }

    private static Stream<JsonMaskerTestInstance> testArrayInputFile() throws IOException {
        return JsonMaskerTestUtil.getJsonMaskerTestInstancesFromFile("test-array-input.json").stream();
    }

    /**
     * Easy reproducer of the above test
     */
    @Test
    void test() {
        System.out.println(JsonMasker.getMasker(JsonMaskingConfig.builder()
                                                        .allowKeys(Set.of("allowedKey"))
                                                        .build())
                                   .mask("""
                                             [                                                                                      
                                               {
                                                 "allowedKey": "yes",
                                                 "notAllowedKey": "hello"
                                               }
                                             ]
                                         """));
    }
}
