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

/*
 *  Change Logs:
 *  Date            Author          Notes
 *  05-Feb-2026     Shiven Patel    Initial implementation of custom instrumentation utility
 */

package com.motadata.apm;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;

import java.util.ArrayList;
import java.util.List;

import com.motadata.apm.CustomInstrumentationException.*;

/**
 * Utility class for setting custom instrumentation attributes on OpenTelemetry spans.
 * <p>
 * This class provides functionality for:
 * - Setting scalar attributes (Boolean, Double, Integer, Long, String) on the current span
 * - Setting list attributes (List of Boolean, Double, Integer, Long, String) on the current span
 * - Validation of attribute keys and values with descriptive error messages
 * <p>
 * All attribute keys are automatically prefixed with "apm." unless already present.
 * This ensures consistent namespacing for APM-related attributes across the application.
 * <p>
 * Thread-safe: All methods operate on the current span context which is thread-local.
 * The class depends only on the OpenTelemetry Span.current() method for span access.
 *
 * @since 1.0.0
 */
public final class CustomInstrumentation
{

    private static final String DEFAULT_PREFIX = "apm.";

    private CustomInstrumentation()
    {
        throw new AssertionError(CustomInstrumentation.class.getSimpleName() + " is a utility class and should not be instantiated");
    }

    /**
     * Prepares and validates the attribute key for use with OpenTelemetry spans.
     * <p>
     * This method performs the following operations:
     * 1. Validates that the key is not null
     * 2. Trims whitespace from the key and validates that the key is not empty after trimming
     * 3. Validates that the key does not contain invalid whitespace characters
     * 4. Converts the key to lowercase for consistency
     * 5. Adds the "apm." prefix if not already present
     *
     * @param key The original attribute key
     * @return The prepared key with "apm." prefix in lowercase
     * @throws InvalidAttributeKeyException if the key is null, empty, or contains invalid characters
     */
    private static String prepareKey(String key)
    {
        if (key == null)
        {
            throw new InvalidAttributeKeyException("Attribute key cannot be null");
        }

        key = key.trim();

        if (key.isEmpty())
        {
            throw new InvalidAttributeKeyException("Attribute key cannot be empty or whitespace only");
        }

        if (!key.matches("[a-zA-Z0-9.]+"))
        {
            throw new InvalidAttributeKeyException("Attribute key contains invalid characters. Only alphabets, numbers, and dots are allowed: '" + key + "'");
        }

        key = key.toLowerCase();

        return key.startsWith(DEFAULT_PREFIX) ? key : DEFAULT_PREFIX + key;
    }

    /**
     * Validates that a value is not null.
     * <p>
     * This method is used to validate attribute values before setting them on a span.
     *
     * @param value    The value to validate
     * @param typeName The type name of the value (used in error messages)
     * @param key      The attribute key (used in error messages)
     * @throws InvalidAttributeValueException if the value is null
     */
    private static void validateValue(Object value, String typeName, String key)
    {
        if (value == null)
        {
            throw new InvalidAttributeValueException(typeName + " value cannot be null for key: " + key);
        }
    }

    /**
     * Validates that a list is not null and not empty.
     * <p>
     * This method is used to validate list attribute values before setting them on a span.
     *
     * @param list     The list to validate
     * @param typeName The type name of the list elements (used in error messages)
     * @param key      The attribute key (used in error messages)
     * @throws InvalidAttributeValueException if the list is null or empty
     */
    private static void validateList(List<?> list, String typeName, String key)
    {
        if (list == null)
        {
            throw new InvalidAttributeValueException(typeName + " list cannot be null for key: " + key);
        }

        if (list.isEmpty())
        {
            throw new InvalidAttributeValueException(typeName + " list cannot be empty for key: " + key);
        }
    }

    /**
     * Filters out null values from a list and returns a new list containing only non-null values.
     * <p>
     * This method is thread-safe as it:
     * - Does not modify the input list
     * - Creates a new ArrayList with initial capacity matching the input size for optimal memory allocation
     * - Uses only local variables (no shared state)
     * - Is stateless and can be safely called from multiple threads
     *
     * @param <T>      The type of elements in the list
     * @param list     The input list to filter (must not be null)
     * @param typeName The type name of the list elements (used in error messages)
     * @param key      The attribute key (used in error messages)
     * @return A new list containing only non-null values from the input list
     * @throws InvalidAttributeValueException if the filtered list is empty (all values were null)
     */
    private static <T> List<T> filterNullValues(List<T> list, String typeName, String key)
    {
        List<T> filtered = new ArrayList<T>(list.size());

        for (T v : list)
        {
            if (v != null)
            {
                filtered.add(v);
            }
        }

        if (filtered.isEmpty())
        {
            throw new InvalidAttributeValueException(typeName + " list contains only null values for key: " + key);
        }

        return filtered;
    }

    /**
     * Filters out null, NaN, and Infinite values from a Double list.
     * <p>
     * This method is thread-safe and optimized for memory efficiency.
     *
     * @param list The input list to filter (must not be null)
     * @param key  The attribute key (used in error messages)
     * @return A new list containing only valid Double values
     * @throws InvalidAttributeValueException if the filtered list is empty
     */
    private static List<Double> filterDoubles(List<Double> list, String key)
    {
        List<Double> filtered = new ArrayList<Double>(list.size());

        for (Double v : list)
        {
            if (v != null && !Double.isNaN(v) && !Double.isInfinite(v))
            {
                filtered.add(v);
            }
        }

        if (filtered.isEmpty())
        {
            throw new InvalidAttributeValueException("Double list contains only invalid values for key: " + key);
        }

        return filtered;
    }

    /**
     * Filters out null values from an Integer list and converts to Long.
     * <p>
     * This method is thread-safe and optimized for memory efficiency.
     * Integer values are converted to Long for OpenTelemetry compatibility.
     *
     * @param list The input list to filter (must not be null)
     * @param key  The attribute key (used in error messages)
     * @return A new list containing Long values converted from non-null Integers
     * @throws InvalidAttributeValueException if the filtered list is empty
     */
    private static List<Long> filterAndConvertIntegers(List<Integer> list, String key)
    {
        List<Long> filtered = new ArrayList<Long>(list.size());

        for (Integer v : list)
        {
            if (v != null)
            {
                filtered.add(v.longValue());
            }
        }

        if (filtered.isEmpty())
        {
            throw new InvalidAttributeValueException("Integer list contains only null values for key: " + key);
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
     * @throws SpanAttributeException if no active span is available or an error occurs
     */
    private static Span getCurrentSpan()
    {
        try
        {
            Span span = Span.current();

            if (span == null)
            {
                throw new SpanAttributeException("No active span available in current context");
            }

            return span;
        }
        catch (Exception exception)
        {
            throw new SpanAttributeException("Failed to retrieve current span", exception);
        }
    }

    /**
     * Sets a boolean attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The boolean value to set (cannot be null)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the value is null
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void set(String key, Boolean value)
    {
        key = prepareKey(key);

        validateValue(value, "Boolean", key);

        getCurrentSpan().setAttribute(key, value);
    }

    /**
     * Sets a double attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     * The value must be a valid finite number (not NaN or Infinite).
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The double value to set (cannot be null, NaN, or Infinite)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the value is null, NaN, or Infinite
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void set(String key, Double value)
    {
        key = prepareKey(key);

        if (value == null || Double.isNaN(value) || Double.isInfinite(value))
        {
            throw new InvalidAttributeValueException("Invalid Double value for key: " + key);
        }

        getCurrentSpan().setAttribute(key, value);
    }

    /**
     * Sets an integer attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     * The integer value is internally converted to a long for OpenTelemetry compatibility.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The integer value to set (cannot be null)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the value is null
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void set(String key, Integer value)
    {
        key = prepareKey(key);

        validateValue(value, "Integer", key);

        getCurrentSpan().setAttribute(key, value.longValue());
    }

    /**
     * Sets a long attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The long value to set (cannot be null)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the value is null
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void set(String key, Long value)
    {
        key = prepareKey(key);

        validateValue(value, "Long", key);

        getCurrentSpan().setAttribute(key, value);
    }

    /**
     * Sets a string attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The string value to set (cannot be null)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the value is null
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void set(String key, String value)
    {
        key = prepareKey(key);

        validateValue(value, "String", key);

        getCurrentSpan().setAttribute(key, value);
    }

    /**
     * Sets a boolean array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     * Null elements in the list are automatically filtered out.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The list of boolean values (cannot be null or empty, must contain at least one non-null value)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the list is null, empty, or contains only null values
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void setBooleanList(String key, List<Boolean> value)
    {
        key = prepareKey(key);

        validateList(value, "Boolean", key);

        List<Boolean> filtered = filterNullValues(value, "Boolean", key);

        getCurrentSpan().setAttribute(AttributeKey.booleanArrayKey(key), filtered);
    }

    /**
     * Sets a double array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     * Null, NaN, and Infinite values in the list are automatically filtered out.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The list of double values (cannot be null or empty, must contain at least one valid value)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the list is null, empty, or contains only invalid values
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void setDoubleList(String key, List<Double> value)
    {
        key = prepareKey(key);

        validateList(value, "Double", key);

        List<Double> filtered = filterDoubles(value, key);

        getCurrentSpan().setAttribute(AttributeKey.doubleArrayKey(key), filtered);
    }

    /**
     * Sets an integer array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     * Null elements in the list are automatically filtered out.
     * Integer values are internally converted to long for OpenTelemetry compatibility.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The list of integer values (cannot be null or empty, must contain at least one non-null value)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the list is null, empty, or contains only null values
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void setIntegerList(String key, List<Integer> value)
    {
        key = prepareKey(key);

        validateList(value, "Integer", key);

        List<Long> filtered = filterAndConvertIntegers(value, key);

        getCurrentSpan().setAttribute(AttributeKey.longArrayKey(key), filtered);
    }

    /**
     * Sets a long array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     * Null elements in the list are automatically filtered out.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The list of long values (cannot be null or empty, must contain at least one non-null value)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the list is null, empty, or contains only null values
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void setLongList(String key, List<Long> value)
    {
        key = prepareKey(key);

        validateList(value, "Long", key);

        List<Long> filtered = filterNullValues(value, "Long", key);

        getCurrentSpan().setAttribute(AttributeKey.longArrayKey(key), filtered);
    }

    /**
     * Sets a string array attribute on the current span.
     * <p>
     * The attribute key will be automatically prefixed with "apm." if not already present.
     * Null elements in the list are automatically filtered out.
     *
     * @param key   The attribute key (will be prefixed with "apm." if needed)
     * @param value The list of string values (cannot be null or empty, must contain at least one non-null value)
     * @throws InvalidAttributeKeyException   if the key is null, empty, or contains invalid characters
     * @throws InvalidAttributeValueException if the list is null, empty, or contains only null values
     * @throws SpanAttributeException         if an error occurs while setting the attribute
     */
    public static void setStringList(String key, List<String> value)
    {
        key = prepareKey(key);

        validateList(value, "String", key);

        List<String> filtered = filterNullValues(value, "String", key);

        getCurrentSpan().setAttribute(AttributeKey.stringArrayKey(key), filtered);
    }
}