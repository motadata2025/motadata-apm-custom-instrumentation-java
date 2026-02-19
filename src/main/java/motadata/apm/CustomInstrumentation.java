/*
 *   Copyright (c) Motadata 2026. All rights reserved.
 *
 *   This source code is the property of Motadata and constitutes
 *   proprietary and confidential information. Unauthorized copying, distribution,
 *   modification, or use of this file, via any medium, is strictly prohibited
 *   unless prior written permission is obtained from Motadata.
 *
 *   Unauthorized access or use of this software may result in legal action
 *   and/or prosecution to the fullest extent of the law.
 *
 *   This software is provided "AS IS," without warranties of any kind, express
 *   or implied, including but not limited to implied warranties of
 *   merchantability or fitness for a particular purpose. In no event shall
 *   Motadata be held liable for any damages arising from the use
 *   of this software.
 *
 *   For inquiries, contact: engg@motadata.com
 *
 */

package motadata.apm;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for setting custom instrumentation attributes on OpenTelemetry
 * spans.
 * <p>
 * This class provides functionality for:
 * <ul>
 *   <li>Setting scalar attributes (Boolean, Double, Integer, Long, String, boolean, double, integer, long) on the current span</li>
 *   <li>Setting list attributes (List of Boolean, Double, Integer, Long, String) on the current span</li>
 *   <li>Validation of attribute keys and values with descriptive error messages</li>
 * </ul>
 * <p>
 * All attribute keys are automatically prefixed with "apm." unless already
 * present.
 * This ensures consistent namespacing for APM-related attributes across the
 * application.
 * <p>
 * Thread-safe: All methods operate on the current span context which is
 * thread-local.
 * The class depends only on the OpenTelemetry Span.current() method for span
 * access.
 *
 * @since 1.0.0
 */
public final class CustomInstrumentation
{

    private static final String DEFAULT_PREFIX = "apm.";

    private static final Pattern KEY_VALIDATION_PATTERN = Pattern.compile("[a-zA-Z0-9.]+");

    private CustomInstrumentation()
    {
        throw new AssertionError("CustomInstrumentation is a utility class and should not be instantiated");
    }

    /**
     * Prepares and validates the attribute key for use with OpenTelemetry spans.
     * <p>
     * This method performs the following operations:
     * <ol>
     *   <li>Validates that the key is not null</li>
     *   <li>Trims whitespace from the key and validates that the key is not empty after trimming</li>
     *   <li>Validates that the key does not contain invalid whitespace characters</li>
     *   <li>Converts the key to lowercase for consistency</li>
     *   <li>Adds the "apm." prefix if not already present</li>
     * </ol>
     *
     * @param key The original attribute key
     * @return The prepared key with "apm." prefix in lowercase
     * @throws Exception if the key is null, empty, or contains
     *                   invalid characters
     */
    private static String prepareKey(String key) throws Exception
    {
        if (key == null)
        {
            throw new Exception("Attribute key cannot be null");
        }

        key = key.trim();

        if (key.isEmpty())
        {
            throw new Exception("Attribute key cannot be empty or whitespace only");
        }

        if (!KEY_VALIDATION_PATTERN.matcher(key).matches())
        {
            throw new Exception("Attribute key contains invalid characters. Only alphabets, numbers, and dots are allowed: '" + key + "'");
        }

        key = key.toLowerCase();

        return key.startsWith(DEFAULT_PREFIX) ? key : DEFAULT_PREFIX + key;
    }

    /**
     * Validates that a value is not null.
     * <p>
     * This method is used to validate attribute values before setting them on a
     * span.
     *
     * @param value The value to validate
     * @param key   The attribute key (used in error messages)
     * @throws Exception if the value is null
     */
    private static void validateValue(Object value, String key) throws Exception
    {
        if (value == null)
        {
            throw new Exception("Attribute value cannot be null for key: " + key);
        }
    }

    /**
     * Validates that a list is not null and not empty.
     * <p>
     * This method is used to validate list attribute values before setting them on
     * a span.
     *
     * @param list The list to validate
     * @param key  The attribute key (used in error messages)
     * @throws Exception if the list is null or empty
     */
    private static void validateList(List<?> list, String key) throws Exception
    {
        if (list == null)
        {
            throw new Exception("List cannot be null for key: " + key);
        }

        if (list.isEmpty())
        {
            throw new Exception("List cannot be empty for key: " + key);
        }
    }

    /**
     * Filters out null values from a list and returns a new list containing only
     * non-null values.
     * <p>
     * This method is thread-safe as it:
     * <ul>
     *   <li>Does not modify the input list</li>
     *   <li>Uses Java 8 streams for functional-style filtering</li>
     *   <li>Uses only local variables (no shared state)</li>
     *   <li>Is stateless and can be safely called from multiple threads</li>
     * </ul>
     *
     * @param <T>  The type of elements in the list
     * @param list The input list to filter (must not be null)
     * @param key  The attribute key (used in error messages)
     * @return A new list containing only non-null values from the input list
     * @throws Exception if the filtered list is empty (all values were null)
     */
    private static <T> List<T> filterNullValues(List<T> list, String key) throws Exception
    {
        List<T> filtered = list.stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(() -> new ArrayList<>(list.size())));

        if (filtered.isEmpty())
        {
            throw new Exception("List contains only null values for key: " + key);
        }

        return filtered;
    }

    /**
     * Filters out null, NaN, and Infinite values from a Double list.
     * <p>
     * This method is thread-safe and uses Java 8 streams for functional-style
     * filtering.
     *
     * @param list The input list to filter (must not be null)
     * @param key  The attribute key (used in error messages)
     * @return A new list containing only valid Double values
     * @throws Exception if the filtered list is empty
     */
    private static List<Double> filterDoubles(List<Double> list, String key) throws Exception
    {
        List<Double> filtered = list.stream()
                .filter(v -> v != null && !Double.isNaN(v) && !Double.isInfinite(v))
                .collect(Collectors.toCollection(() -> new ArrayList<>(list.size())));

        if (filtered.isEmpty())
        {
            throw new Exception("List contains only invalid values for key: " + key);
        }

        return filtered;
    }

    /**
     * Converts an Integer list to Long list, filtering out null values.
     * <p>
     * This method is thread-safe and uses Java 8 streams for functional-style
     * filtering and mapping.
     * Integer values are converted to Long for OpenTelemetry compatibility.
     *
     * @param list The input list to convert (must not be null)
     * @param key  The attribute key (used in error messages)
     * @return A new list containing Long values converted from non-null Integers
     * @throws Exception if the filtered list is empty
     */
    private static List<Long> convertIntegers(List<Integer> list, String key) throws Exception
    {
        List<Long> filtered = list.stream()
                .filter(Objects::nonNull)
                .map(Integer::longValue)
                .collect(Collectors.toCollection(() -> new ArrayList<>(list.size())));

        if (filtered.isEmpty())
        {
            throw new Exception("List contains only null values for key: " + key);
        }

        return filtered;
    }

    /**
     * Retrieves the current active span from the OpenTelemetry context.
     * <p>
     * This method safely retrieves the current span and handles any exceptions
     * that might occur during the retrieval process.
     *
     * @return The current active span
     * @throws Exception if no active span is available or an error occurs
     */
    private static Span getCurrentSpan() throws Exception
    {
        try
        {
            Span span = Span.current();

            if (span == null)
            {
                throw new Exception("No active span available in current context");
            }

            return span;
        }
        catch (Exception exception)
        {
            throw new Exception("Failed to retrieve current span", exception);
        }
    }

    /**
     * Sets a boolean attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The boolean value to set (cannot be null)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The value is null</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void set(String key, Boolean value) throws Exception
    {
        key = prepareKey(key);

        validateValue(value, key);

        getCurrentSpan().setAttribute(key, value);
    }

    /**
     * Sets a double attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     * The value must be a valid finite number (not NaN or Infinite).
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The double value to set (cannot be null, NaN, or Infinite)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The value is null, NaN, or Infinite</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void set(String key, Double value) throws Exception
    {
        key = prepareKey(key);

        if (value == null || Double.isNaN(value) || Double.isInfinite(value))
        {
            throw new Exception("Invalid Double value for key: " + key);
        }

        getCurrentSpan().setAttribute(key, value);
    }

    /**
     * Sets an integer attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     * The integer value is internally converted to a long for OpenTelemetry
     * compatibility.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The integer value to set (cannot be null)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The value is null</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void set(String key, Integer value) throws Exception
    {
        key = prepareKey(key);

        validateValue(value, key);

        getCurrentSpan().setAttribute(key, value.longValue());
    }

    /**
     * Sets a long attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The long value to set (cannot be null)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The value is null</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void set(String key, Long value) throws Exception
    {
        key = prepareKey(key);

        validateValue(value, key);

        getCurrentSpan().setAttribute(key, value);
    }

    /**
     * Sets a string attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The string value to set (cannot be null)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The value is null</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void set(String key, String value) throws Exception
    {
        key = prepareKey(key);

        validateValue(value, key);

        getCurrentSpan().setAttribute(key, value);
    }

    /**
     * Sets a boolean array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     * Null elements in the list are automatically filtered out.
     *
     * @param key    The attribute key (will be prefixed with "apm." if needed)
     * @param values The list of boolean values (cannot be null or empty, must
     *               contain at least one non-null value)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The list is null, empty, or contains only null values</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void setBooleanList(String key, List<Boolean> values) throws Exception
    {
        key = prepareKey(key);

        validateList(values, key);

        List<Boolean> filtered = filterNullValues(values, key);

        getCurrentSpan().setAttribute(AttributeKey.booleanArrayKey(key), filtered);
    }

    /**
     * Sets a double array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     * Null, NaN, and Infinite values in the list are automatically filtered out.
     *
     * @param key    The attribute key (will be prefixed with "apm." if needed)
     * @param values The list of double values (cannot be null or empty, must
     *               contain at least one valid value)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The list is null, empty, or contains only invalid values</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void setDoubleList(String key, List<Double> values) throws Exception
    {
        key = prepareKey(key);

        validateList(values, key);

        List<Double> filtered = filterDoubles(values, key);

        getCurrentSpan().setAttribute(AttributeKey.doubleArrayKey(key), filtered);
    }

    /**
     * Sets an integer array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     * Null elements in the list are automatically filtered out.
     * Integer values are internally converted to long for OpenTelemetry
     * compatibility.
     *
     * @param key    The attribute key (will be prefixed with "apm." if needed)
     * @param values The list of integer values (cannot be null or empty, must
     *               contain at least one non-null value)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The list is null, empty, or contains only null values</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void setIntegerList(String key, List<Integer> values) throws Exception
    {
        key = prepareKey(key);

        validateList(values, key);

        List<Long> filtered = convertIntegers(values, key);

        getCurrentSpan().setAttribute(AttributeKey.longArrayKey(key), filtered);
    }

    /**
     * Sets a long array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     * Null elements in the list are automatically filtered out.
     *
     * @param key    The attribute key (will be prefixed with "apm." if needed)
     * @param values The list of long values (cannot be null or empty, must contain
     *               at least one non-null value)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The list is null, empty, or contains only null values</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void setLongList(String key, List<Long> values) throws Exception
    {
        key = prepareKey(key);

        validateList(values, key);

        List<Long> filtered = filterNullValues(values, key);

        getCurrentSpan().setAttribute(AttributeKey.longArrayKey(key), filtered);
    }

    /**
     * Sets a string array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already
     * present.
     * Null elements in the list are automatically filtered out.
     *
     * @param key    The attribute key (will be prefixed with "apm." if needed)
     * @param values The list of string values (cannot be null or empty, must
     *               contain at least one non-null value)
     * @throws Exception if:
     * <ul>
     *   <li>The key is null, empty, or contains invalid characters</li>
     *   <li>The list is null, empty, or contains only null values</li>
     *   <li>An error occurs while setting the attribute</li>
     * </ul>
     */
    public static void setStringList(String key, List<String> values) throws Exception
    {
        key = prepareKey(key);

        validateList(values, key);

        List<String> filtered = filterNullValues(values, key);

        getCurrentSpan().setAttribute(AttributeKey.stringArrayKey(key), filtered);
    }
}