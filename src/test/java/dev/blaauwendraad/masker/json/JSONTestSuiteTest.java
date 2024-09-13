package dev.blaauwendraad.masker.json;

import com.fasterxml.jackson.core.JacksonException;
import dev.blaauwendraad.masker.json.config.JsonMaskingConfig;
import org.assertj.core.api.Assertions;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test cases from <a href="https://github.com/nst/JSONTestSuite">JSONTestSuite</a>
 */
public class JSONTestSuiteTest {

    private static final List<String> PRODUCING_STACK_OVER_FLOW = List.of(
            "n_structure_100000_opening_arrays.json",
            "n_structure_open_array_object.json"
    );

    private static final List<String> INVALID_UTF_8 = List.of(
            "i_string_1st_surrogate_but_2nd_missing.json",
            "i_string_1st_valid_surrogate_2nd_invalid.json",
            "i_string_incomplete_surrogate_and_escape_valid.json",
            "i_string_incomplete_surrogate_pair.json",
            "i_string_incomplete_surrogates_escape_valid.json",
            "i_string_invalid_lonely_surrogate.json",
            "i_string_invalid_surrogate.json",
            "i_string_inverted_surrogates_U+1D11E.json",
            "i_string_lone_second_surrogate.json",
            "n_string_1_surrogate_then_escape.json",
            "n_string_1_surrogate_then_escape_u.json",
            "n_string_1_surrogate_then_escape_u1.json",
            "n_string_1_surrogate_then_escape_u1x.json",
            "n_string_backslash_00.json",
            "n_string_escape_x.json",
            "n_string_escaped_ctrl_char_tab.json",
            "n_string_escaped_emoji.json",
            "n_string_incomplete_escaped_character.json",
            "n_string_incomplete_surrogate.json",
            "n_string_incomplete_surrogate_escape_invalid.json",
            "n_string_invalid-utf-8-in-escape.json",
            "n_string_invalid_backslash_esc.json",
            "n_string_invalid_unicode_escape.json",
            "n_string_invalid_utf8_after_escape.json",
            "n_string_unicode_CapitalU.json",
            "n_structure_open_open.json"
    );

    private static final List<String> INVALID_JSON_JACKSON_DIFFERENT_BEHAVIOR = List.of(
            "i_string_UTF-16LE_with_BOM.json",
            "i_string_UTF8_surrogate_U+D800.json",
            "i_string_not_in_unicode_range.json",
            "i_string_overlong_sequence_2_bytes.json",
            "i_string_utf16BE_no_BOM.json",
            "i_string_utf16LE_no_BOM.json"
    );
    public static final Path JSON_TEST_SUITE_PATH = Path.of("src/test/JSONTestSuite/");

    @ParameterizedTest(name = "{0}")
    @MethodSource("mustPassSuite")
    void mustPassSuiteWithNoopMaskerShouldBeEquivalentToJackson(String testName, JsonTestSuiteFile file) {
        // masks everything with ValueMasker that tracks the content, but returns the same value back
        JsonMasker jsonMasker = JsonMasker.getMasker(
                JsonMaskingConfig.builder()
                        .allowKeys(Set.of())
                        .maskStringsWith(ValueMaskers.withRawValueFunction(value -> value))
                        .maskNumbersWith(ValueMaskers.withRawValueFunction(value -> value))
                        .maskBooleansWith(ValueMaskers.withRawValueFunction(value -> value))
                        .build()
        );

        byte[] actual = jsonMasker.mask(file.originalContent);

        // must be equivalent to being parsed by jackson
        try {
            assertThat(ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(actual))
                    .as("Failed for input: " + new String(actual, StandardCharsets.UTF_8))
                    .isEqualTo(ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(file.originalContent));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mayPassSuite")
    void mayPassSuiteWithNoopMaskerShouldNotFail(String testName, JsonTestSuiteFile file) {
        // masks everything with ValueMasker that tracks the content, but returns the same value back
        JsonMasker jsonMasker = JsonMasker.getMasker(
                JsonMaskingConfig.builder()
                        .allowKeys(Set.of())
                        .maskStringsWith(ValueMaskers.withRawValueFunction(value -> value))
                        .maskNumbersWith(ValueMaskers.withRawValueFunction(value -> value))
                        .maskBooleansWith(ValueMaskers.withRawValueFunction(value -> value))
                        .build()
        );

        byte[] actual = jsonMasker.mask(file.originalContent);

        if (INVALID_JSON_JACKSON_DIFFERENT_BEHAVIOR.contains(file.name)) {
            // for these jackson behavior is different from java.lang.String parsing of UTF-8 characters
            return;
        }

        // if jackson can parse it - out should be equivalent
        try {
            assertThat(ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(actual))
                    .as("Failed for input: " + new String(actual, StandardCharsets.UTF_8))
                    .isEqualTo(ParseAndMaskUtil.DEFAULT_OBJECT_MAPPER.readTree(file.originalContent));
        } catch (JacksonException e) {
            // JsonMasker didn't fail, but jackson can't parse it, nothing to compare
            // suck cases are also tested by shouldMaskAllTestCasesPredictably
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("mustFailSuite")
    void mustFailSuiteWithNoopMaskerShouldOnlyFailWithInvalidJsonException(String testName, JsonTestSuiteFile file) {
        // masks everything with ValueMasker that tracks the content, but returns the same value back
        JsonMasker jsonMasker = JsonMasker.getMasker(
                JsonMaskingConfig.builder()
                        .allowKeys(Set.of())
                        .maskStringsWith(ValueMaskers.withRawValueFunction(value -> value))
                        .maskNumbersWith(ValueMaskers.withRawValueFunction(value -> value))
                        .maskBooleansWith(ValueMaskers.withRawValueFunction(value -> value))
                        .build()
        );

        if (PRODUCING_STACK_OVER_FLOW.contains(file.name)) {
            Assertions.assertThatThrownBy(() -> jsonMasker.mask(file.originalContent))
                    .isInstanceOf(InvalidJsonException.class);
        } else {
            jsonMasker.mask(file.originalContent);
        }
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allWithoutStackOverFlow")
    void shouldMaskAllTestCasesPredictably(String testName, JsonTestSuiteFile file) {
        // masks everything
        JsonMasker jsonMasker = JsonMasker.getMasker(
                JsonMaskingConfig.builder()
                        .allowKeys(Set.of())
                        .build()
        );

        byte[] actual = jsonMasker.mask(file.originalContent);

        Assertions.assertThat(file.maskedContent).isNotNull();
        Assertions.assertThat(new String(actual, StandardCharsets.UTF_8))
                .isEqualTo(new String(file.maskedContent, StandardCharsets.UTF_8));
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("allWithoutStackOverFlow")
    void mustPassSuiteWithNoopTextFunction(String testName, JsonTestSuiteFile file) {
        // masks everything with withTextFunction, that is equivalent to default masker settings
        JsonMasker jsonMasker = JsonMasker.getMasker(
                JsonMaskingConfig.builder()
                        .allowKeys(Set.of())
                        .maskStringsWith(ValueMaskers.withTextFunction(value -> "***"))
                        .maskNumbersWith(ValueMaskers.withTextFunction(value -> "###"))
                        .maskBooleansWith(ValueMaskers.withTextFunction(value -> "&&&"))
                        .build()
        );

        if (INVALID_UTF_8.contains(file.name)) {
            Assertions.assertThatThrownBy(() -> jsonMasker.mask(file.originalContent))
                    .isInstanceOf(InvalidJsonException.class);
        } else {
            byte[] actual = jsonMasker.mask(file.originalContent);

            Assertions.assertThat(file.maskedContent).isNotNull();
            Assertions.assertThat(new String(actual, StandardCharsets.UTF_8))
                    .isEqualTo(new String(file.maskedContent, StandardCharsets.UTF_8));
        }
    }

    private static Stream<Arguments> mustPassSuite() {
        return loadSuite(name -> name.startsWith("y_"))
                .stream()
                .map(file -> Arguments.of(file.name, file));
    }

    private static Stream<Arguments> mayPassSuite() {
        return loadSuite(name -> name.startsWith("i_"))
                .stream()
                .map(file -> Arguments.of(file.name, file));
    }

    private static Stream<Arguments> mustFailSuite() {
        return loadSuite(name -> name.startsWith("n_"))
                .stream()
                .map(file -> Arguments.of(file.name, file));
    }

    private static Stream<Arguments> allWithoutStackOverFlow() {
        return loadSuite(name -> !PRODUCING_STACK_OVER_FLOW.contains(name))
                .stream()
                .map(file -> Arguments.of(file.name, file));
    }

    private static List<JsonTestSuiteFile> loadSuite(Predicate<String> predicate) {
        try (Stream<Path> files = Files.list(JSON_TEST_SUITE_PATH .resolve("original"))) {
            var tests = files
                    .filter(file -> predicate.test(file.getFileName().toString()))
                    .map(file -> {
                        try {
                            var fileName = file.getFileName().toString();
                            var content = Files.readAllBytes(file);
                            if (PRODUCING_STACK_OVER_FLOW.contains(fileName)) {
                                return new JsonTestSuiteFile(fileName, content, null);
                            }
                            var maskedContent = Files.readAllBytes(JSON_TEST_SUITE_PATH.resolve("masked/%s".formatted(fileName)));
                            return new JsonTestSuiteFile(fileName, content, maskedContent);
                        } catch (IOException e) {
                            throw new IllegalStateException(e);
                        }
                    })
                    .toList();
            if (tests.isEmpty()) {
                throw new IllegalStateException("Not test suites loaded for filter");
            }
            return tests;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    record JsonTestSuiteFile(String name, byte[] originalContent, byte @Nullable [] maskedContent) {
    }
}
