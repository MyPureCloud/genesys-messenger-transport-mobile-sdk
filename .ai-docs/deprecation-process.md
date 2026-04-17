# Deprecation Rules

Based on the [Deprecation Process – GBM Mobile](https://genesys-confluence.atlassian.net/wiki/spaces/WMM/pages/1478263154/Deprecation+Process+-+GBM+Mobile).

## When Deprecation Is Required

Any breaking change to the Public API must be deprecated before removal. Breaking changes include:

- Public API removal or signature modification
- Behavior changes that alter contract expectations
- Removal of default values
- Protocol or data format changes
- Upgrading a publicly visible dependency inside the SDK

## Deprecation Annotations

Every `@Deprecated` annotation **must** include:

1. **message** – a clear reason for the deprecation.
2. **replaceWith** – the replacement API, if one exists. Omit only when there is no replacement.
3. A KDoc tag or comment stating **"Deprecated since: \<version\>"**.

```kotlin
/**
 * @deprecated Deprecated since 3.2.0. Use [newMethod] instead.
 */
@Deprecated(
    message = "Use newMethod() instead. Deprecated since 3.2.0.",
    replaceWith = ReplaceWith("newMethod()"),
    level = DeprecationLevel.WARNING
)
fun oldMethod() { }
```

## Reviewer Checklist

When reviewing code that deprecates or removes public API, verify:

- [ ] `@Deprecated` annotation is present with a clear `message`.
- [ ] `replaceWith` is provided when a replacement exists.
- [ ] "Deprecated since: \<version\>" is documented.
- [ ] A migration example or pointer is included when the change is non-trivial.
- [ ] Changelog entry is added for the deprecation or removal.
