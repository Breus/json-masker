# 1. Support masking of array values

Date: 2023-11-19

## Status

Accepted

## Context

Currently, strings and numbers inside JSON object can be masked using the json-masker.
However, strings and numbers can also be elements of a (nested) array, in which case they are currently always ignored.
This ADR proposes to add support for masking strings and numbers which are elements of (nested) arrays.

### Solution 1: Support mixed arrays and mask all the string/numbers in the array

Supporting arrays of mixed types (number + strings + arrays + objects) possesses an implementation and performance
challenge: Up to now, the json-masker did not have to keep track of current nesting level. I.e.: it did not matter where 
we found `"maskMe": "a"`, since all such instances are masked.

To bring support for masking of strings and numbers inside arrays, we will have to know exactly which field is being
processed right now, to correctly mask a JSON like this:
```json
{
  "maskMe": [
    "a",
    { "someOtherArray": ["b"]},
    "c"
  ]
}
```

On the other hand, we will have to implement this logic for a future JsonPath masking and going in that direction now 
will simplify that effort. 

Also, this solution will give the most complete support for arrays with the simplest semantics of how the json-masker 
will behave at the cost of the complexity of the implementation.

### Solution 2: Only support arrays with numbers + strings.

We could simply not support arrays with mixed types, considering that those are quite rare (and basically non-existent 
in most strongly typed languages such as Java).
\
However, given that the purpose of this library is to provide high-performance with as correct semantics as 
possible, we only have the following options that would be obvious for the user:
1. Mask all mixed values of an array (Solution 1)
2. Fail if a maskable array contains anything different than string/number

The problem with "failing" is that we need to have an ability to detect mixed arrays:
```json
{
  "maskMe": ["a", 0, {"hello": "hello"}, []]
}
```
In this case it would be fairly easy, as we already know that array is of string + number types, and we could fail at 
the first encounter of `{` or `[`.

But considering the following example:
```json
{
  "maskMe": [{"hello": "hello"}, [], "a", 0]
}
```
we could fail when we see `{` or `[` preemptively, but it would be difficult for the users to configure the library
correctly - you need to be sure that every field you're trying to mask is never an array, and also making it impossible
to mask an object like this:
```json
{
  "maskMe": "genuine secret",
  "nested": {
    "maskMe": [{"hello": "hello"}]
  }
}
```

therefore in order to reliably detect a mixed array, we have to process it fully.


## Decision(s)

Implement [Solution 1](#solution-1-support-mixed-arrays-and-mask-all-the-stringnumbers-in-the-array)

- Masking of arrays will be enabled by default
- Mixed arrays (where elements can be of any JSON value type) are supported,
   but only string and numbers will be masked
- Numeric and string values inside nested arrays will be masked

## Consequences

There will be a new, backward-incompatible version of json-masker which has default support for masking numeric and 
string values inside (mixed-type) arrays.   

From this version onwards, the following listed inputs below result in the following listed outputs:

### Example 1

Given input:
```json
{
"maskMe": ["a", "b"]
}
```
Results in:
```json
{
  "maskMe": ["*", "*"]
}
```
Or with `obfuscationLength(4)`, results in:
```json
{
  "maskMe": ["****", "****"]
}
```

### Example 2 

Given input:
```json
{
  "maskMe": ["a", 123]
}
```
With `maskNumberValuesWith(8)`, results in:

```json
{
  "maskMe": ["*", 888]
}
```

### Example 3

Given input:
```json
{
  "maskMe": ["a", {"field1": "a", "field2": "b"}]
}
```

Results in:
```json
{
  "maskMe": ["*", {"field1": "a", "field2": "b"}]
}
```

### Example 4

Given input:
```json
{
  "maskMe": [[["a"], {"maskMe": "hello"}], "b"]
}
```
Results in: 
```json
{
  "maskMe": [[["*"], {"maskMe": "*****"}], "*"]
}
```


