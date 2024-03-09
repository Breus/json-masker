# 4. Custom value masking strategies

Date: 25-03-2024

## Status

Accepted by:

- [Breus](https://github.com/Breus)
- [gavlyukovskiy](https://github.com/gavlyukovskiy)

## Context

Currently, masking JSON values numbers can be configured in several ways: 

1. A fixed string mask (e.g. `***`, `[REDACTED]`, etc.)
2. A per-character (per-digit) mask in the value (e.g. `#`, `*`, etc.) which results in a length-preserving mask
3. Additionally, for numbers: type-preserving number mask (e.g. `0`, `8`, etc.)

There have been multiple requests so far to support custom masking strategies for values, for example, to be able to 
partially mask values.

Some examples of this are:

- Partially masking an IBAN: `"NL91 FAKE 0417 1643 00"` as `"NL91 ************** 00"`
- Masking every second character of a username of an e-mail address: `"j.johnson@gmail.com"` as `"j*j*h*s*n@gmail.com"`
- Partially masking a PAN while preserving whitespace characters: `"5574 3510 1234 2394"` as `"**** **** **** *394"`
- Partially masking a phone number: `"+31 6 1234 7890"` as `"+31############890"`

The examples above show that for certain masking strategies we need access to the original value which will be masked to
decide how to mask it.

## Decision

Provide a `ValueMasker` interface that allows to configure custom value masking strategies which can access the original
value. Additionally, we provide several out-of-the-box `ValueMasker` implementations for common value masking
strategies for convenience and as a guidance for custom implementations.

For the user, the most convenient interface for custom masking strategy for string values would be to allow injecting a
`Function<String, String>`. However, this would not allow converting a string value into a value of a different type (
e.g. `null`), and the same is true for any other type (a number or a boolean). Additionally, it would be inconvenient to
deal with a `Function<Number, Number>` without knowing the concrete Java type (`Integer`, `Long`, `Double`, etc.) and
also require the `json-masker` to parse the JSON value to the correct Java type, which would impose an unnecessary
performance hit. Finally, parsing a JSON value is not always required for every custom masking strategy. For example,
when a fixed mask is used (e.g. `0`).

This leaves us with two options. Either we provide the raw `byte[]` representing just the value about to be masked, or
provide access to the full `byte[]`, representing while JSON and a pointer referencing just the segment with the value.
Both of the options have their own drawbacks. THe first requires us to allocate a copy of a byte array

The first option adds unnecessary memory allocations to copy the value into its own `byte[]`. The second option is
inherently unsafe because we provide raw access to the input `byte[]` which enables the user to rewrite more than just
the value and requires to be extra careful with the implementation. With the second option, however, the drawback could
be nullified if we provide an abstraction to access the value in a safer manner (`ValueMaskerContext#byteLength`
and `ValueMaskerContext#getByte`).

Therefore, we decided to go with option 2 and the interface for injecting custom masking strategies looks like:

```java
public interface ValueMasker {
    void maskValue(ValueMaskerContext context);
}
```

where the `ValueMaskerContext` will provide access to the full JSON `byte[]` with internally managed offsets and bound
checks for the value being masked. Additionally, this context provides several convenience methods and may serve as
future extension points.

Since certain masking strategies are only applicable for certain JSON value types, the masking function needs to be
typed. For example the e-mail masking mentioned before would only make sense to be applied to JSON strings. Therefore,
we want the user to be able to specify to which JSON type a custom `ValueMasker` is applied.

## Consequences

We provide a new extension API that is operates on `byte` level allowing virtually unlimited custom value masking
strategies. Internally, we re-implemented all existing masking operations to implement this new API.     
