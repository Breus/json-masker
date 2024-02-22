# High-performance JSON masker

[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/Breus/json-masker/build.yml?query=branch%3Amaster)](https://github.com/Breus/json-masker/actions/workflows/build.yml?query=branch%3Amaster)
[![Maven Central](https://img.shields.io/maven-central/v/dev.blaauwendraad/json-masker)](https://central.sonatype.com/artifact/dev.blaauwendraad/json-masker)
[![Sonar Quality Gate](https://sonarcloud.io/api/project_badges/measure?project=Breus_json-masker&metric=alert_status)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Coverage](https://sonarcloud.io/api/project_badges/measure?project=Breus_json-masker&metric=coverage)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Reliability](https://sonarcloud.io/api/project_badges/measure?project=Breus_json-masker&metric=reliability_rating)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Security](https://sonarcloud.io/api/project_badges/measure?project=Breus_json-masker&metric=security_rating)](https://sonarcloud.io/project/overview?id=Breus_json-masker)

JSON masker library which can be used to mask sensitive values inside json using a set of keys (**block-mode**) or,
alternatively, allow only specific keys to be kept unmasked (**allow-mode**).

The library provides a convenient API and its implementation is focused on maximum (time) performance and minimal heap allocations.

No additional third-party runtime dependencies are required to use this library.

## Features

* Mask all primitive values by specifying the keys to mask, by default any `string` is masked as `"***"`, `number` as `"###"` and `boolean` as `"&&&"`
* If value of masked key corresponds to an `object`, all nested fields, including nested arrays and objects) will be masked recursively
* If value of masked key corresponds to an `array`, all values of the array (including nested arrays and objects) will be masked recursively
* Ability to define different masking strategy per type
   - **(default)** mask strings with a different string: `"maskMe": "secret"` -> `"maskMe": "***"`
   - mask _characters_ of a string with a different character: `"maskMe": "secret"` -> `"maskMe": "*****"` (preserves length)
   - **(default)** mask numbers with a string: `"maskMe": 12345` -> `"maskMe": "###"` (changes number type to string)
   - mask numbers with a different number: `"maskMe": 12345` -> `"maskMe": 0` (preserves number type)
   - mask _digits_ of a number with a different digit: `"maskMe": 12345` -> `"maskMe": 88888` (preserves number type and length)
   - **(default)** mask booleans with a string: `"maskMe": true` -> `"maskMe": "&&&"` (changes boolean type to string)
   - mask booleans with a different boolean: `"maskMe": true` -> `"maskMe": false` (preserves boolean type)
* Ability to define masking strategy per key
* Target key **case sensitivity configuration** (default: `false`)
* Use **block-list** (`maskKeys`) or **allow-list** (`allowKeys`) for masking
* Limited support for JsonPath masking in **block-list** (`maskJsonPaths`) and **allow-list** (`allowJsonPaths`) modes
* Masking a valid json will always return a valid json
* The implementation only supports json in UTF-8 character encoding

## Usage examples

`JsonMasker` instance can be created using any of the following factory methods:
```java
// block-mode, default config
var jsonMasker = JsonMasker.getMasker(Set.of("email", "iban"));

// block-mode, default config (using a builder)
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "iban"))
                .build()
);

// block-mode, json path
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskJsonPaths(Set.of("$.email", "$.nested.iban"))
                .build()
);

// allow-mode, default config
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .allowKeys(Set.of("id", "name"))
                .build()
);

// allow-mode, json path
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskJsonPaths(Set.of("$.id", "$.nested.name"))
                .build()
);
```

Using `JsonMaskingConfig` allows customizing the masking behaviour of types, keys or json path or mix keys and json paths.

> [!NOTE]
> Whenever a simple key (`maskKeys(Set.of("email", "iban"))`) is specified, it is going to be masked recursively 
> regardless of the nesting, whereas using a JsonPath (`maskJsonPaths(Set.of("$.email", "$.iban"))`) would only 
> mask those keys on the top level json

after creation the instance can be used to mask a json:
```java
String maskedJson = jsonMasker.mask(json);
```

The `mask` method is thread-safe and it is advised to reuse the `JsonMasker` instance as it pre-processes the
masking (allowed) keys for faster lookup during the actual masking.  

### Default JSON masking

Example of masking fields (block-mode) with a default config

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(Set.of("email", "age", "visaApproved", "iban", "billingAddress"));

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Van Gogh Museum",
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "some-companion-email@example.com",
      "age": 32,
      "visaApproved": true
    }
  ]
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": "###",
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***",
      "***"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "***",
      "age": "###",
      "visaApproved": "&&&"
    }
  ]
}
```

### Allow-list approach

Example showing an allow-list based approach of masking a json.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .allowKeys(Set.of("orderId", "id", "travelPurpose", "successful"))
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Van Gogh Museum",
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "some-companion-email@example.com",
      "age": 32,
      "visaApproved": true
    }
  ]
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": "###",
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***",
      "***"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "***",
      "age": "###",
      "visaApproved": "&&&"
    }
  ]
}
```

### Overriding default masks

The default masks can be overridden for any type.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved", "iban", "billingAddress"))
                .maskStringsWith("[redacted]")
                .maskNumbersWith("[redacted]")
                .maskBooleansWith("[redacted]")
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Van Gogh Museum",
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "some-companion-email@example.com",
      "age": 32,
      "visaApproved": true
    }
  ]
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "[redacted]",
    "age": "[redacted]",
    "visaApproved": "[redacted]"
  },
  "payment": {
    "iban": "[redacted]",
    "successful": true,
    "billingAddress": [
      "[redacted]",
      "[redacted]",
      "[redacted]"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "[redacted]",
      "age": "[redacted]",
      "visaApproved": "[redacted]"
    }
  ]
}
```

### Masking with JsonPath

To have more control over the nesting, JsonPath can be used to specify the keys that needs to be masked (allowed).

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskJsonPaths(Set.of(
                        "$.customerDetails.email",
                        "$.customerDetails.age",
                        "$.customerDetails.visaApproved",
                        "$.payment.iban",
                        "$.payment.billingAddress"
                ))
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Van Gogh Museum",
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "some-companion-email@example.com",
      "age": 32,
      "visaApproved": true
    }
  ]
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": "###",
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***",
      "***"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "some-companion-email@example.com",
      "age": 32,
      "visaApproved": true
    }
  ]
}
```

### Masking with preserving the type

The following configuration might be useful where the value must be masked, but the type needs to be preserved, so that
the resulting json can be parsed again or if the strict json schema is required.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved", "iban", "billingAddress"))
                .maskNumbersWith(0)
                .maskBooleansWith(false)
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Van Gogh Museum",
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "some-companion-email@example.com",
      "age": 32,
      "visaApproved": true
    }
  ]
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": 0,
    "visaApproved": false
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***",
      "***"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "***",
      "age": 0,
      "visaApproved": false
    }
  ]
}
```

### Masking with preserving the length

Example showing masking where the length of the original value (`string` or `number`) is preserved.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved", "iban", "billingAddress"))
                .maskStringCharactersWith("*")
                .maskNumberDigitsWith(8)
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Van Gogh Museum",
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "some-companion-email@example.com",
      "age": 32,
      "visaApproved": true
    }
  ]
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "*******************************",
    "age": 88,
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "**********************",
    "successful": true,
    "billingAddress": [
      "***************",
      "*************",
      "*****************"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "********************************",
      "age": 88,
      "visaApproved": "&&&"
    }
  ]
}
```

### Masking with using a per-key masking configuration

When using a `JsonMaskingConfig` you can also define a per-key masking configuration, which allows to customize the way
certain values are masked.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved"))
                .maskKeys(Set.of("iban"), KeyMaskingConfig.builder()
                        .maskStringCharactersWith("*")
                        .build())
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

> [!NOTE]
> When defining a config for the specific key and value of that key is an `object` or an `array`, the config will apply
> recursively to all nested keys and values, unless the nested key(s) defines its own masking configuration.
>
> If config is attached to a JsonPath it will have a precedence over a regular key.

#### Input

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "some-customer-email@example.com",
    "age": 29,
    "visaApproved": true
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Van Gogh Museum",
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "some-companion-email@example.com",
      "age": 32,
      "visaApproved": true
    }
  ]
}
```

#### Output

```json
{
  "orderId": "789 123 456",
  "customerDetails": {
    "id": 1,
    "travelPurpose": "business",
    "email": "***",
    "age": "###",
    "visaApproved": "&&&"
  },
  "payment": {
    "iban": "**********************",
    "successful": true,
    "billingAddress": [
      "***",
      "***",
      "***"
    ]
  },
  "companions": [
    {
      "id": 2,
      "email": "***",
      "age": "###",
      "visaApproved": "&&&"
    }
  ]
}
```

## Dependencies

* **The library has no third-party runtime dependencies**
* The library only has a single JSR-305 compilation dependency for nullability annotations
* The test/benchmark dependencies for this library are listed in the `build.gradle`

## Performance

The `json-masker` library is optimized for a fast key lookup that scales well with a large key set to mask (or allow).
The input is only scanned once and avoid allocations whenever possible.

### Benchmarks

For benchmarks we compare against couple of baseline benchmarks: counting bytes without masking, using jackson to parse
a json into `JsonNode` and iterate over replacing all the keys and naive regex replace. Generally our implementation 
is ~15-25 times faster than using jackson.

```text
Benchmark                              (characters)  (jsonPath)  (jsonSize)  (maskedKeyProbability)   Mode  Cnt     Score   Error  Units
BaselineBenchmark.countBytes                unicode         N/A         2mb                    0.01  thrpt    2  1525.281          ops/s
BaselineBenchmark.jacksonParseAndMask       unicode         N/A         2mb                    0.01  thrpt    2     3.353          ops/s
BaselineBenchmark.regexReplace              unicode         N/A         2mb                    0.01  thrpt    2     2.684          ops/s
JsonMaskerBenchmark.jsonMaskerBytes         unicode       false         2mb                    0.01  thrpt    2    79.309          ops/s
JsonMaskerBenchmark.jsonMaskerBytes         unicode        true         2mb                    0.01  thrpt    2    80.745          ops/s
JsonMaskerBenchmark.jsonMaskerString        unicode       false         2mb                    0.01  thrpt    2    48.488          ops/s
JsonMaskerBenchmark.jsonMaskerString        unicode        true         2mb                    0.01  thrpt    2    49.345          ops/s
```