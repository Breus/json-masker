<div align="center">

# JSON Masker

### Blazingly fast, zero-dependency JSON masking for Java

[![Maven Central](https://img.shields.io/maven-central/v/dev.blaauwendraad/json-masker?style=flat-square)](https://central.sonatype.com/artifact/dev.blaauwendraad/json-masker)
[![Javadoc](https://javadoc.io/badge2/dev.blaauwendraad/json-masker/javadoc.svg?style=flat-square)](https://javadoc.io/doc/dev.blaauwendraad/json-masker)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square)](https://opensource.org/licenses/MIT)
[![GitHub Workflow Status (with event)](https://img.shields.io/github/actions/workflow/status/Breus/json-masker/build.yml?query=branch%3Amaster&style=flat-square)](https://github.com/Breus/json-masker/actions/workflows/build.yml?query=branch%3Amaster)
[![Sonar Quality Gate](https://img.shields.io/sonar/quality_gate/Breus_json-masker?server=https%3A%2F%2Fsonarcloud.io&style=flat-square)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Coverage](https://img.shields.io/sonar/coverage/Breus_json-masker?server=https%3A%2F%2Fsonarcloud.io&color=appveyor&style=flat-square)](https://sonarcloud.io/project/overview?id=Breus_json-masker)
[![Sonar Tests](https://img.shields.io/sonar/total_tests/Breus_json-masker?server=https%3A%2F%2Fsonarcloud.io&style=flat-square)](https://sonarcloud.io/project/overview?id=Breus_json-masker)

**Protect sensitive data in your JSON logs, API responses, and messages with a library that's up to 11x faster than
Jackson-based alternatives.**

[Getting Started](#getting-started) •
[Features](#features) •
[Performance](#performance) •
[Contributing](CONTRIBUTING.md)

</div>

---

## Quick Example

```java
var jsonMasker = JsonMasker.getMasker(Set.of("email", "password"));

String masked = jsonMasker.mask("""
        {"name": "John", "email": "user@example.com", "password": "secret123"}
        """);

// Result: {"name": "John", "email": "***", "password": "***"}
```

## Why JSON Masker?

| Feature                  |  JSON Masker   | Jackson + Manual |    Regex    |
|--------------------------|:--------------:|:----------------:|:-----------:|
| **Performance**          | 🏎️ 454K ops/s |   🐢 40K ops/s   | 🐌 5K ops/s |
| **Convenient API**       |       ✅        |     ❌ Manual     |  ❌ Manual   |
| **Zero Dependencies**    |       ✅        |        ❌         |      ✅      |
| **JSONPath Support**     |       ✅        |   ⚠️ Extra lib   |      ❌      |
| **Streaming API**        |       ✅        |        ✅         |      ❌      |
| **Preserves Formatting** |       ✅        |        ❌         |      ✅      |
| **Lightweight**          |     ~60 KB     |     ~2.5 MB+     |     N/A     |

## Use Cases

- **📝 Logging**: Safely log JSON payloads without exposing PII, passwords, or API keys
- **🔒 GDPR Compliance**: Mask personal data before storing or transmitting
- **🐛 Debugging**: Share JSON data with support teams while protecting sensitive fields
- **📊 Analytics**: Process JSON data while anonymizing user information
- **🔄 API Proxies**: Sanitize responses before forwarding to third parties

## Table of Contents

- [Getting Started](#getting-started)
- [Features](#features)
- [JDK Compatibility](#jdk-compatibility)
- [Usage Examples](#usage-examples)
- [Performance](#performance)
- [Contributing](#contributing)

---

## Getting Started

Add the dependency to your project:

**Gradle:**

```groovy
implementation("dev.blaauwendraad:json-masker:${version}")
```

**Maven:**

```xml

<dependency>
    <groupId>dev.blaauwendraad</groupId>
    <artifactId>json-masker</artifactId>
    <version>${version}</version>
</dependency>
```

Then start masking:

```java
// Create a reusable, thread-safe masker
var jsonMasker = JsonMasker.getMasker(Set.of("email", "ssn", "creditCard"));

// Mask sensitive data
String masked = jsonMasker.mask(jsonString);
```
> [!TIP]
> Reuse the `JsonMasker` instance: it pre-processes keys for optimal performance!

---

## Features

JSON Masker supports two modes:

1. **Block Mode**: Mask values corresponding to a specified set of keys.
2. **Allow Mode**: Unmask only the values corresponding to specified keys, while masking all others.

### Key Capabilities

**Streaming & In-Memory APIs** - Process large JSON files efficiently or mask strings in memory

**High Performance** - Single-pass scanning with minimal heap allocations to reduce GC pressure

**Flexible Targeting** - Use blocklist, allowlist, or JSONPath expressions to target specific fields

**Customizable Masking** - Configure masking per type, per key, or use custom `ValueMasker` functions

### Supported Masking Strategies

* Mask all primitive values by specifying the keys to mask, by default, any `string` is masked as `"***"`, any `number`
  as `"###"` and any `boolean` as `"&&&"`
* If the value of a targeted key corresponds to an `object`, all nested fields, including nested arrays and objects will
  be masked, recursively
* If the value of a targeted key corresponds to an `array`, all values of the array, including nested arrays and
  objects, will be masked, recursively
* Ability to define a custom masking strategy per value type
    - **(default)** mask strings with a different string: `"maskMe": "secret"` -> `"maskMe": "***"`
    - mask _characters_ of a string with a different character: `"maskMe": "secret"` -> `"maskMe": "*****"` (preserves
      length)
    - **(default)** mask numbers with a string: `"maskMe": 12345` -> `"maskMe": "###"` (changes number type to string)
    - mask numbers with a different number: `"maskMe": 12345` -> `"maskMe": 0` (preserves number type)
    - mask _digits_ of a number with a different digit: `"maskMe": 12345` -> `"maskMe": 88888` (preserves number type
      and length)
    - **(default)** mask booleans with a string: `"maskMe": true` -> `"maskMe": "&&&"` (changes boolean type to string)
    - mask booleans with a different boolean: `"maskMe": true` -> `"maskMe": false` (preserves boolean type)
* Ability to define a custom masking strategy per key
* Ability to configure JSON type preserving masking configurations so the masked JSON can be deserialized back into a
  Java object it was serialized from
* Target key **case sensitivity configuration** (default: `false`)
* Use **blocklist** (`maskKeys`) or **allowlist** (`allowKeys`) for masking
* Support for JSONPath masking in both **blocklist** (`maskJsonPaths`) and **allowlist** (`allowJsonPaths`)
  modes
* Masking a valid JSON will always produce a valid JSON. If the input is not valid JSON, processing is guaranteed to
  complete, but the resulting masked output is undefined

Note: Since [RFC 8259](https://datatracker.ietf.org/doc/html/rfc8259) dictates that JSON exchanges between systems that
are not part of an enclosed system MUST be encoded using UTF-8, the `json-masker` only supports UTF-8 encoding.

## JDK Compatibility

From version 1.1 onwards, `json-masker` became a multi-release JAR (MRJAR) supporting JDK 11 and higher, making JDK 11
the minimum requirement.

## Usage Examples

`JsonMasker` instance can be created using any of the following factory methods:

```java
// block-mode, default masking config
var jsonMasker = JsonMasker.getMasker(Set.of("email", "iban"));

// block-mode, default masking config (using a builder)
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "iban"))
                .build()
);

// block-mode, JSONPath
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskJsonPaths(Set.of("$.email", "$.nested.iban", "$.organization.*.name"))
                .build()
);

// allow-mode, default masking config
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .allowKeys(Set.of("id", "name"))
                .build()
);

// allow-mode, JSONPath
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .allowJsonPaths(Set.of("$.id", "$.clients.*.phone", "$.nested.name"))
                .build()
);
```

Using `JsonMaskingConfig` allows customizing the masking behavior of types, keys or JSONPath or mix keys and JSON
paths.

> [!NOTE]
> Whenever a simple key (`maskKeys(Set.of("email", "iban"))`) is specified, it is going to be masked recursively
> regardless of the nesting, whereas using a JSONPath (`maskJsonPaths(Set.of("$.email", "$.iban"))`) would only
> mask those keys on the top level JSON

After creating the `JsonMasker` instance, it can be used to mask a JSON as following:

```java
String maskedJson = jsonMasker.mask(json);
```

The `mask` method is thread-safe, and it is advised to reuse the `JsonMasker` instance as it pre-processes the
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
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
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
      "***"
    ]
  },
  "companyContact": {
    "email": "***"
  }
}
```

### Allow-list approach

Example showing an allowlist-based approach of masking a JSON.

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
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
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
      "***"
    ]
  },
  "companyContact": {
    "email": "***"
  }
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
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
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
      "[redacted]"
    ]
  },
  "companyContact": {
    "email": "[redacted]"
  }
}
```

### Masking with the streaming API

To mask (potentially) large JSON input, the streaming API can be used.

All features of the JsonMasker work exactly the same for the streaming API and the (default) in-memory API.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "iban"))
                .build()
);

jsonMasker.mask(jsonInputStream, jsonOutputStream);
```

### Masking with JSONPath

To have more control over the nesting, JSONPath can be used to specify the keys that need to be masked (allowed).

The following JSONPath features are not supported:

* Descendant segments.
* Child segments.
* Name selectors.
* Array slice selectors.
* Index selectors.
* Filter selectors.
* Function extensions.
* Escape characters.

The library also imposes a number of additional restrictions:

* Numbers as key names are disallowed.
* JSONPath keys must not have ambiguous segments that share the same path.  
  For example, `$.payment.iban` and `$.payment.*.address` combination is disallowed because segment 2 is ambiguous and
  shares the same path (`$.payment.`).  
  On contrary, `$.payment.iban` and `$.customerDetails.*.address` combination is allowed because segment 2 does not
  share the same path.  
  Also, `$.payment.iban` and `$.payment.customerDetails` combination is allowed because segment 2 is not ambiguous.
* JSONPath must not end with a single leading wildcard. Use `$.a` instead of `$.a.*`.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskJsonPaths(Set.of(
                        "$.customerDetails.email",
                        "$.customerDetails.age",
                        "$.customerDetails.visaApproved",
                        "$.payment.iban",
                        "$.payment.billingAddress",
                        "$.customerDetails.identificationDocuments.*.number"
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
    "visaApproved": true,
    "identificationDocuments": [
      {
        "type": "passport",
        "country": "NL",
        "number": "1234567890"
      },
      {
        "type": "passport",
        "country": "US",
        "number": "E12345678"
      }
    ]
  },
  "payment": {
    "iban": "NL91 FAKE 0417 1643 00",
    "successful": true,
    "billingAddress": [
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
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
    "visaApproved": "&&&",
    "identificationDocuments": [
      {
        "type": "passport",
        "country": "NL",
        "number": "***"
      },
      {
        "type": "passport",
        "country": "US",
        "number": "***"
      }
    ]
  },
  "payment": {
    "iban": "***",
    "successful": true,
    "billingAddress": [
      "***",
      "***"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
}
```

### Masking with preserving the type

The following configuration might be useful where the value must be masked, but the type needs to be preserved so that
the resulting JSON can be parsed again or if the strict JSON schema is required.

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
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
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
      "***"
    ]
  },
  "companyContact": {
    "email": "***"
  }
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
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
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
      "*************",
      "*****************"
    ]
  },
  "companyContact": {
    "email": "*************"
  }
}
```

### Masking with using a per-key masking configuration

When using a `JsonMaskingConfig` you can also define a per-key masking configuration, which allows customizing the way
certain values are masked.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys(Set.of("email", "age", "visaApproved", "billingAddress"))
                .maskKeys("iban", KeyMaskingConfig.builder()
                        .maskStringCharactersWith("*")
                        .build()
                )
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

> [!NOTE]
> When defining a config for the specific key and value of that key is an `object` or an `array`, the config will apply
> recursively to all nested keys and values, unless the nested key(s) defines its own masking configuration.
>
> If config is attached to a JSONPath it has a precedence over a regular key.

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
      "Museumplein 6",
      "1071 DJ Amsterdam"
    ]
  },
  "companyContact": {
    "email": "info@acme.com"
  }
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
      "***"
    ]
  },
  "companyContact": {
    "email": "***"
  }
}
```

### Masking with a `ValueMasker`

In addition to standard options like `maskStringsWith`, `maskNumbersWith` and `maskBooleansWith`, the `ValueMasker` is
a functional interface for low-level value masking, which allows fully customizing the masking process. It can be used
for masking all values, specific JSON value types, or specific keys.

The `ValueMasker` operates on the full value on the byte level, i.e., the value is `byte[]`.

> [!NOTE]
> `ValueMasker` can modify JSON value in any way, but also means that the implementation needs to be careful with
> parsing the value of any JSON type and replacing the correct slice of the value. Otherwise, the masking process could
> produce an
> invalid JSON.

For convenience, a couple out-of-the-box maskers are available in `ValueMaskers` as well as adapters to
`Function<String, String>`.

#### Usage

```java
var jsonMasker = JsonMasker.getMasker(
        JsonMaskingConfig.builder()
                .maskKeys("values")
                .maskStringsWith(ValueMaskers.withRawValueFunction(value -> value.startsWith("\"secret:") ? "\"***\"" : value))
                .maskKeys("email", KeyMaskingConfig.builder()
                        .maskStringsWith(ValueMaskers.email(/* prefix */ 2, /* suffix */ 2, /* keep domain */ true, "***"))
                        .build()
                )
                .build()
);

String maskedJson = jsonMasker.mask(json);
```

#### Input

```json
{
  "values": [
    "not a secret",
    "secret: very much"
  ],
  "email": "agavlyukovskiy@gmail.com"
}
```

#### Output

```json
{
  "values": [
    "not a secret",
    "***"
  ],
  "email": "ag***iy@gmail.com"
}
```

---

## Performance

The `json-masker` library is optimized for a fast key lookup that scales well with a large key set to mask (or allow).
The input is only scanned once, and memory allocations are avoided whenever possible.

### Benchmarks

For benchmarking, we compare the implementation against multiple baseline benchmarks, which are:

- Counting the bytes of the JSON message without doing any other operation
- Using Jackson to parse a JSON message into `JsonNode` and masking it by iterating over and replacing all values
  corresponding to the targeted keys
- A naive regex masking (replacement) implementation.

**Our implementation is ~10–15 times faster than using Jackson** with no runtime dependencies and a convenient API
out-of-the-box.

```text
Benchmark                              (characters)  (jsonPath)  (jsonSize)  (maskedKeyProbability)   Mode  Cnt        Score        Error  Units
BaselineBenchmark.countBytes                unicode         N/A         1kb                     0.1  thrpt    4  2762218.146 ±  41683.232  ops/s
BaselineBenchmark.jacksonParseAndMask       unicode         N/A         1kb                     0.1  thrpt    4    40483.364 ±    785.524  ops/s
BaselineBenchmark.regexReplace              unicode         N/A         1kb                     0.1  thrpt    4     4107.170 ±    162.509  ops/s
JsonMaskerBenchmark.jsonMaskerBytes         unicode       false         1kb                     0.1  thrpt    4   451025.312 ±   4761.981  ops/s
JsonMaskerBenchmark.jsonMaskerBytes         unicode        true         1kb                     0.1  thrpt    4   454335.938 ±   3760.368  ops/s
JsonMaskerBenchmark.jsonMaskerString        unicode       false         1kb                     0.1  thrpt    4   233862.490 ±   3505.727  ops/s
JsonMaskerBenchmark.jsonMaskerString        unicode        true         1kb                     0.1  thrpt    4   218006.202 ±   1130.448  ops/s
```

---

## Contributing

Contributions are welcome! Please read our [Contributing Guide](CONTRIBUTING.md) to get started.

- 🐛 Found a bug? [Open an issue](https://github.com/Breus/json-masker/issues)
- 💡 Have an idea? [Start a discussion](https://github.com/Breus/json-masker/discussions)
- 🔧 Want to contribute? [Submit a PR](https://github.com/Breus/json-masker/pulls)

---

<div align="center">

### ⭐ If this project helps you, consider giving it a star!

</div>

