package masker.json;

import java.util.Set;

public record JsonMaskerTestInstance(Set<String> targetKeys, String input,
                                     String expectedOutput, Integer obfuscationLength) {
}