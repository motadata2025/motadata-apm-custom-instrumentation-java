# Motadata Custom Instrumentation

A lightweight OpenTelemetry utility library for adding safe, prefixed span attributes to your Java applications.

## Overview

Motadata Custom Instrumentation provides a simple and type-safe way to add custom attributes to OpenTelemetry spans. All attributes are automatically prefixed with `apm.` to ensure consistent namespacing across your application.

## Features

- ✅ **Type-Safe API**: Support for Boolean, Double, Integer, Long, and String attributes
- ✅ **List Support**: Set array attributes for all supported types
- ✅ **Automatic Prefixing**: All keys are automatically prefixed with `apm.`
- ✅ **Validation**: Comprehensive validation with descriptive error messages
- ✅ **Thread-Safe**: All operations are thread-safe
- ✅ **Zero Dependencies**: Only depends on OpenTelemetry API (shaded to avoid conflicts)
- ✅ **Null Filtering**: Automatically filters out null values from lists

## Requirements

- Java 8 or higher
- OpenTelemetry instrumentation in your application

## Installation

### Maven

#### Step 1: Add GitHub Packages Repository

Add the following repository to your `pom.xml`:

```xml
<repositories>
    <repository>
        <id>github</id>
        <name>GitHub Motadata Apache Maven Packages</name>
        <url>https://maven.pkg.github.com/motadata2025/motadata-apm-custom-instrumentation-java</url>
    </repository>
</repositories>
```

#### Step 2: Add Dependency

Add the following dependency to your `pom.xml`:

```xml
<dependency>
    <groupId>com.motadata.apm</groupId>
    <artifactId>motadata-custom-instrumentation</artifactId>
    <version>1.0.0</version>
</dependency>
```

#### Step 3: Configure GitHub Authentication

GitHub Packages requires authentication even for public packages. Add this to your `~/.m2/settings.xml`:

```xml
<settings xmlns="http://maven.apache.org/SETTINGS/1.0.0"
          xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
          xsi:schemaLocation="http://maven.apache.org/SETTINGS/1.0.0
                              http://maven.apache.org/xsd/settings-1.0.0.xsd">
    <servers>
        <server>
            <id>github</id>
            <username>YOUR_GITHUB_USERNAME</username>
            <password>YOUR_GITHUB_PERSONAL_ACCESS_TOKEN</password>
        </server>
    </servers>
</settings>
```

**Note:** The `<id>github</id>` must match the repository ID in your pom.xml. You need a GitHub Personal Access Token with `read:packages` scope.

### Gradle

#### Step 1: Add GitHub Packages Repository

Add the following to your `build.gradle`:

```gradle
repositories {
    maven {
        url = uri("https://maven.pkg.github.com/motadata2025/motadata-apm-custom-instrumentation-java")
        credentials {
            username = project.findProperty("gpr.user") ?: System.getenv("USERNAME")
            password = project.findProperty("gpr.key") ?: System.getenv("TOKEN")
        }
    }
}
```

#### Step 2: Add Dependency

Add the following to your `build.gradle`:

```gradle
implementation 'com.motadata.apm:motadata-custom-instrumentation:1.0.0'
```

#### Step 3: Configure GitHub Authentication

Add your GitHub credentials to `~/.gradle/gradle.properties`:

```properties
gpr.user=YOUR_GITHUB_USERNAME
gpr.key=YOUR_GITHUB_PERSONAL_ACCESS_TOKEN
```

**Note:** You need a GitHub Personal Access Token with `read:packages` scope.

### Alternative: Direct JAR Installation

If you don't want to use GitHub Packages, you can install the JAR directly:

#### Maven Local Installation

```bash
mvn install:install-file \
  -Dfile=motadata-custom-instrumentation-1.0.0-fat.jar \
  -DgroupId=com.motadata.apm \
  -DartifactId=motadata-custom-instrumentation \
  -Dversion=1.0.0 \
  -Dpackaging=jar
```

Then add the dependency normally (no repository configuration needed):

```xml
<dependency>
    <groupId>com.motadata.apm</groupId>
    <artifactId>motadata-custom-instrumentation</artifactId>
    <version>1.0.0</version>
</dependency>
```

## Usage

### Basic Examples

```java
import com.motadata.apm.MotadataCustomInstrumentation;

// Set scalar attributes
MotadataCustomInstrumentation.set("user.id", 12345L);
MotadataCustomInstrumentation.set("user.name", "John Doe");
MotadataCustomInstrumentation.set("user.active", true);
MotadataCustomInstrumentation.set("response.time", 123.45);

// All keys are automatically prefixed with "apm."
// The above will create: apm.user.id, apm.user.name, etc.
```

### List Attributes

```java
import java.util.List;

// Set list attributes
MotadataCustomInstrumentation.setStringList("tags", List.of("api", "production", "critical"));
MotadataCustomInstrumentation.setLongList("user.ids", List.of(1L, 2L, 3L));
MotadataCustomInstrumentation.setBooleanList("flags", List.of(true, false, true));
MotadataCustomInstrumentation.setDoubleList("metrics", List.of(1.5, 2.5, 3.5));
MotadataCustomInstrumentation.setIntegerList("counts", List.of(10, 20, 30));
```

### Advanced Usage

```java
// Keys are case-insensitive and converted to lowercase
MotadataCustomInstrumentation.set("User.Name", "Jane");  // becomes apm.user.name

// Prefix is automatically added if not present
MotadataCustomInstrumentation.set("custom.key", "value");      // becomes apm.custom.key
MotadataCustomInstrumentation.set("apm.custom.key", "value");  // stays apm.custom.key

// Null values in lists are automatically filtered
List<String> tags = Arrays.asList("tag1", null, "tag2");
MotadataCustomInstrumentation.setStringList("tags", tags);  // Only "tag1" and "tag2" are set
```

## Exception Handling

The library throws descriptive exceptions for invalid inputs:

```java
try {
    MotadataCustomInstrumentation.set(null, "value");
} catch (InvalidAttributeKeyException e) {
    // Handle: "Attribute key cannot be null"
}

try {
    MotadataCustomInstrumentation.set("key", (String) null);
} catch (InvalidAttributeValueException e) {
    // Handle: "String value cannot be null for key: apm.key"
}

try {
    MotadataCustomInstrumentation.set("key", Double.NaN);
} catch (InvalidAttributeValueException e) {
    // Handle: "Invalid Double value for key: apm.key"
}
```

## API Reference

### Scalar Methods

- `set(String key, Boolean value)` - Set a boolean attribute
- `set(String key, Double value)` - Set a double attribute (must be finite)
- `set(String key, Integer value)` - Set an integer attribute
- `set(String key, Long value)` - Set a long attribute
- `set(String key, String value)` - Set a string attribute

### List Methods

- `setBooleanList(String key, List<Boolean> value)` - Set a boolean array attribute
- `setDoubleList(String key, List<Double> value)` - Set a double array attribute
- `setIntegerList(String key, List<Integer> value)` - Set an integer array attribute
- `setLongList(String key, List<Long> value)` - Set a long array attribute
- `setStringList(String key, List<String> value)` - Set a string array attribute

## Validation Rules

### Key Validation
- Cannot be null or empty
- Must contain only alphanumeric characters and dots
- Automatically converted to lowercase
- Automatically prefixed with `apm.` if not present

### Value Validation
- Scalar values cannot be null
- Double values cannot be NaN or Infinite
- Lists cannot be null or empty
- Lists must contain at least one valid (non-null) value after filtering

## License

Copyright (c) Motadata 2026. All rights reserved.

This is proprietary software. See LICENSE file for details.

## Contact

For inquiries, contact: engg@motadata.com

## Version History

### 1.0.0 (2026-02-05)
- Initial release
- Support for scalar and list attributes (Boolean, Double, Integer, Long, String)
- Automatic key prefixing and validation
- Thread-safe operations
- Java 8+ compatibility
- Shaded OpenTelemetry dependencies to avoid version conflicts

