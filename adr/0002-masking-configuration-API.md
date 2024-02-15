# 2. API design for JSON masking configurations

Date: 23-01-2024

## Status

Accepted by:

- [Breus](https://github.com/Breus)
- [gavlyukovskiy](https://github.com/gavlyukovskiy)
- [donavdey](https://github.com/donavdey)

## Context

Before releasing version 1.x, we want to make the JSON masking configuration API as extensible as possible while
providing predictable defaults. The current API was influenced by performance considerations which no longer apply due
to certain optimizations done to the implementation. This resulted in a suboptimal API both in terms of extensibility
and in terms of default behaviour.

## Decisions

### Change the default masking behaviors for all JSON types

By default we
1. We will not make a masking decision based on type. 
    The main reason is that masking is explicitly requested by the user by specifying the target keys (either block
    or allow) and it would be unexpected that we do not mask an explicitly requested key because it's a number and 
    number masking is not enabled by default.
2. We will enable length obfuscation by default as length on its own might leak information about the original value.
3. We will mask all types into strings.
    This is done to keep masking consistent across types and to make it apparent that the value was masked (while `"***"`
    is a good indicator for strings, the `888` or `0` for numbers, or `false` for booleans is not)
4. We will use different masks for each original type to keep information about the original type, the strings will 
    be masked as `"***"`, numbers as `"###"` and booleans as `"&&&"`.  
  Keeping type information by default would be helpful when debugging and, while this leaks some information about 
    original _data_, it does not leak the data itself, but rather an implementation detail about _how_ data is stored
    internally. That is likely to be publicly available (e.g. by providing an OpenAPI schema) or it would be possible
    to infer the type from the key itself.
5. The `null` type (value) will remain unmasked, but we make the API extendable to be able to support that if someone requests it, providing a good reason.

### Allow custom masking configurations per target key

We want to provide ability to customize masking per the target key(s) with the options above and also accommodate for
custom strategies in the future (i.e. leave last 4 digits, preserve special symbols, mask email with preserving domain, etc.).

### Change the JSON masking configuration API

The API for configuring `json-masker` will change to make it more consistent and extendable and allow customizations
per JSON type.

We will provide configuration options to customize all the defaults as well as the ability to restore the original
behaviour of pre `1.0.0`.

## Consequences

Therefore the API will be changed to allow these masking options:
```java
public class MaskingConfig {
    // Number masking, mutually exclusive options
    private Boolean disableNumberMasking = false;    // disables number masking
    private String maskNumbersWithString = "###";    // default
    private Integer maskNumberWith;                  // mask number with another number, i.e. 0. Preserves the type 
    private Integer maskNumberDigitsWith;            // mask all digits of a number with masking digit, must be 1 <= x <= 9 to avoid leading zeroes, preserves original length

    // String masking, mutually exclusive options
    private String maskStringsWith = "***";         // default
    private Char maskStringCharactersWith;          // mask all characters with masking character, preserves original length

    // Boolean masking, mutually exclusive options
    private Boolean disableBooleanMasking = false;  // disables boolean masking
    private String maskBooleansWithString = "&&&";  // default
    private Boolean maskBooleanWith;                // mask boolean with another boolean, i.e. false. Preserves the type
    
    public static class Builder {
        ...
        public Builder disableNumberMasking() { ... };
        public Builder maskNumbersWith(String) { ... };
        public Builder maskNumbersWith(int) { ... };
        public Builder maskNumberDigitsWith(int) { ... };

        public Builder maskStringsWith(String) { ... };
        public Builder maskStringCharactersWith(char) { ... };

        public Builder disableBooleanMasking() { ... };
        public Builder maskBooleanWith(String) { ... };
        public Builder maskBooleanWith(boolean) { ... };
    }
} 
```

Current [JsonMaskingConfig](../src/main/java/dev/blaauwendraad/masker/json/config/JsonMaskingConfig.java) will be changed 
to have a global per type configuration as well as per-key configuration:
```java
public class JsonmaskingConfig {
    private final Set<String> targetKeys;
    private final TargetKeyMode targetKeyMode;
    private final MaskingConfig defaultConfig;
    private final Map<String, MaskingConfig> targetKeyConfigs;
    
    public static class Builder {
        ...

        // mask mode
        public Builder mask(String... targets) { ... };
        public Builder mask(String target, MaskingConfig config) { ... };
        public Builder mask(Set<String> targets, MaskingConfig config) { ... };
        // hypothetical custom masker, most likely will be its own type instead of Function
        public Builder mask(String target, Function<String | Number | Boolean, String> mapper) { ... };
        public Builder mask(Set<String> targets, Function<String | Number | Boolean, String> mapper) { ... };

        // allow mode
        public Builder allow(String... targets) { ... };
        
        // optional shortcuts that delegate to default config builder
        private final MaskingConfig.Builder defaultConfigBuilder = MaskingConfig.builder();

        public Builder disableNumberMasking(int value) { defaultConfigBuilder.disableNumberMasking() };
        public Builder maskNumbersWith(String value) { defaultConfigBuilder.maskNumbersWith(value) };
        public Builder maskNumbersWith(int value) { defaultConfigBuilder.maskNumbersWith(value) };
        public Builder maskNumberDigitsWith(int value) { defaultConfigBuilder.maskNumberDigitsWith(value) };

        public Builder maskStringsWith(String value) { defaultConfigBuilder.maskStringsWith(value) };
        public Builder maskStringCharactersWith(char value) { defaultConfigBuilder.maskStringCharactersWith(value) };

        public Builder disableBooleanMasking() { defaultConfigBuilder.disableBooleanMasking() };
        public Builder maskBooleanWith(String value) { defaultConfigBuilder.maskBooleanWith(value) };
        public Builder maskBooleanWith(boolean value) { defaultConfigBuilder.maskBooleanWith(value) };
    }
}
```

The `JsonmaskingConfig.getDefault(Set<String> targets)` and
`JsonmaskingConfig.custom(Set<String> targets, TargetKeyMode targetKeyMode)` will be removed (deprecated?) in favor of 
specifying the keys on the builder, so that every key can be provided with a config:
```java
JsonmaskingConfig.builder()
      .mask("clientPin")
      .mask("cardNumber", MaskingConfig.builder()
        .maskNumberDigitsWith(8) // preserves type and length of cardNumber if number
        .maskStringCharactersWith('*') // preserves type and length of cardNumber if string
        .maskBooleanWith(false) // preserves type of cardNumber if boolean (¯\_(ツ)_/¯)
        .build()
      )
      .build();
```

Therefore the pre `1.0.0` behaviour can be restored by using the following config:
```java
JsonMasker.getMasker(Set.of("email", "iban"))

->

JsonMasker.getMasker(
    JsonmaskingConfig.builder()
          .mask("email", "iban")
          .maskStringCharactersWith('*')
          .disableNumberMasking()
          .disableBooleanMasking()
          .build()
)
```
or, when number masking was configured:
```java
JsonMasker.getMasker(
    JsonMaskingConfig.builder().mask(Set.of("clientPin"))
          .maskNumberValuesWith(8)
          .build()
)

->

JsonMasker.getMasker(
    JsonmaskingConfig.builder()
          .mask("clientPin")
          .maskStringCharactersWith('*')
          .maskNumberDigitsWith(8)
          .disableBooleanMasking()
          .build()
)
```

or, when number masking was configured together with length obfuscation:
```java
JsonMasker.getMasker(
    JsonMaskingConfig.builder().mask(Set.of("clientPin"))
          .maskNumberValuesWith(8)
          .obfuscationLength(3)
          .build()
)

->

JsonMasker.getMasker(
    JsonmaskingConfig.builder()
          .mask("clientPin")
          .maskStringsWith("***")
          .maskNumbersWith(888)
          .disableBooleanMasking()
          .build()
)
``` 