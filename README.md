# Motadata APM Custom Instrumentation for Java

[![Java Version](https://img.shields.io/badge/Java-8%2B-blue.svg)](https://www.oracle.com/java/)
[![OpenTelemetry](https://img.shields.io/badge/OpenTelemetry-Compatible-brightgreen.svg)](https://opentelemetry.io/)
[![License](https://img.shields.io/badge/License-Proprietary-red.svg)](LICENSE)

Lightweight utilities for adding validated, namespaced custom attributes to OpenTelemetry spans in Java. Designed to keep instrumentation safe, consistent, and easy to adopt.

---

## Table of Contents

- [Overview](#overview)
- [Requirements](#requirements)
- [Installation](#installation)
  - [Maven](#maven)
  - [Gradle](#gradle)
- [Quick Start](#quick-start)
- [API at a Glance](#api-at-a-glance)
- [Behavior & Validation](#behavior--validation)
- [Best Practices](#best-practices)
- [Support](#support)
- [License](#license)

---

## Overview

Motadata APM Custom Instrumentation helps you attach business context to traces without risking invalid attributes or inconsistent naming. Keys are automatically namespaced, inputs are validated, and the API is thread-safe across JVMs.

> **Prerequisite:** Instrument your app first with **[Motadata Auto Instrumentation](https://docs.motadata.com/motadata-aiops-docs/apm/apm-in-motadata/)** so the OpenTelemetry context is available.

---

## Requirements

- Java 8+
- Motadata APM agent (auto-instrumented)

---

## Installation

### Maven

Add the following to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Motadata Apache Maven Packages</name>
        <url>https://maven.pkg.github.com/motadata2025/motadata-apm-custom-instrumentation-java</url>
    </repository>
</repositories>

<dependencies>
    <dependency>
        <groupId>motadata-apm</groupId>
        <artifactId>custom-instrumentation</artifactId>
        <version>1.0.0</version>
    </dependency>
</dependencies>
```

Add GitHub Packages credentials in `~/.m2/settings.xml` (Personal Access Token with `read:packages`):

```xml
<settings>
  <servers>
    <server>
      <id>github</id>
      <username>YOUR_GITHUB_USERNAME</username>
      <password>YOUR_GITHUB_PAT</password>
    </server>
  </servers>
</settings>
```

- Create a PAT at https://github.com/settings/tokens with `read:packages`.
- Replace `YOUR_GITHUB_USERNAME` and `YOUR_GITHUB_PAT` and keep `<id>github</id>` consistent with the repository definition above.

### Gradle

Add the following to your `build.gradle`:

```gradle
repositories {
    mavenCentral()
    maven {
        url = uri("https://maven.pkg.github.com/motadata2025/motadata-apm-custom-instrumentation-java")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}

dependencies {
    implementation "motadata-apm:custom-instrumentation:1.0.0"
}
```

Add credentials in `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PAT
```

- Use the same PAT with `read:packages`.
- Keep keys named `gpr.user` and `gpr.key` to match the Gradle configuration above.

---

## Quick Start

```java
import motadata.apm.CustomInstrumentation;
import java.util.Arrays;

try {
    CustomInstrumentation.set("apm.user.id", 12345L);
    CustomInstrumentation.set("apm.user.name", "john.doe");
    CustomInstrumentation.set("apm.request.success", true);
    CustomInstrumentation.setStringList("apm.tags", Arrays.asList("api", "production", "critical"));
} catch (Exception e) {
    System.err.println("Failed to set custom attributes: " + e.getMessage());
}
```

Keys are automatically prefixed with `apm.` when missing, but prefer providing the prefix yourself for consistency. All setters throw `Exception` on invalid input or missing span context, so keep calls wrapped in try/catch.

---

## API at a Glance

### Scalar

| Method | Parameter |
|--------|-----------|
| `set(String key, Boolean value)` | `Boolean` |
| `set(String key, Double value)` | `Double` (finite) |
| `set(String key, Integer value)` | `Integer` |
| `set(String key, Long value)` | `Long` |
| `set(String key, String value)` | `String` |

### Collections

| Method | Parameter |
|--------|-----------|
| `setBooleanList(String key, List<Boolean> values)` | `List<Boolean>` |
| `setDoubleList(String key, List<Double> values)` | `List<Double>` (finite) |
| `setIntegerList(String key, List<Integer> values)` | `List<Integer>` |
| `setLongList(String key, List<Long> values)` | `List<Long>` |
| `setStringList(String key, List<String> values)` | `List<String>` |

---

## Behavior & Validation

- Keys auto-prefix to `apm.` when absent, are lowercased, and are trimmed before validation.
- Keys allow only alphanumeric and dots; whitespace/other symbols are rejected.
- Nulls are removed from lists; lists must retain at least one non-null value.
- Double inputs/drop NaN or Infinity; integer inputs are stored as `long` for OTLP compatibility.
- Thread-safe for concurrent use.
- Throws `Exception` for invalid input or when no active span is present.

Key rules: not null/empty, trimmed, alphanumeric plus dots, lowercase, prefixed `apm.`.  
Value rules: not null, doubles finite (NaN/Infinity discarded), integers coerced to long, lists non-empty after null filtering.

---

## Best Practices

- Use descriptive, hierarchical keys already prefixed with `apm.` (e.g., `apm.order.id`, `apm.order.items`).
- Choose correct types: IDs as `Long`, metrics as `Double`, flags as `Boolean`.
- Wrap calls in try/catch and log errors so observability never breaks business flow.
- Use list setters for collections; let null filtering happen inside the library.
- Keep key naming consistent across services to simplify querying.

---

## Support

- Email: engg@motadata.com
- Issues: GitHub Issues on this repository
- Docs: This README and inline Javadoc

---

## License

**Copyright © 2026 Motadata. All rights reserved.**

Proprietary software; see [LICENSE](LICENSE) for full terms.
