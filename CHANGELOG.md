# Changelog

All notable changes to this project will be documented in this file.

## [1.0.0] - 2026-02-17

### Added
- **Initial Release** of `motadata-apm-custom-instrumentation-java` library.
- Core `CustomInstrumentation.set()` methods for adding boolean, double, integer, long, and string attributes to OpenTelemetry spans.
- Helper methods for setting *lists* of attributes (e.g., `setBooleanList`, `setStringList`) with automatic null filtering.
- **Automatic Namespacing**: All attributes are enforced with `apm.` prefix.
- **Validation**: Strict validation for key names and non-null values.
- **Relocation**: Shaded `io.opentelemetry` dependencies to prevent version conflicts.
- **Documentation**: Comprehensive README and Javadoc support.
