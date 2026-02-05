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
 *  05-Feb-2026     Shiven Patel    Initial implementation of custom instrumentation exceptions
 */

package com.motadata.apm;

/**
 * Custom exceptions for the Motadata Custom Instrumentation utility.
 * <p>
 * This class contains nested exception classes used by {@link CustomInstrumentation}
 * to provide descriptive error messages for various failure scenarios.
 */
public class CustomInstrumentationException
{

    private CustomInstrumentationException()
    {
        throw new AssertionError(CustomInstrumentationException.class.getSimpleName() + " is a container class and should not be instantiated");
    }

    /**
     * Exception thrown when an invalid attribute key is provided.
     * <p>
     * This exception is thrown when:
     * - The attribute key is null
     * - The attribute key is empty or contains only whitespace
     * - The attribute key contains invalid character (anything other than alphabets, numbers, and dots)
     */
    public static class InvalidAttributeKeyException extends IllegalArgumentException
    {
        public InvalidAttributeKeyException(String message)
        {
            super(message);
        }

        public InvalidAttributeKeyException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when an invalid attribute value is provided.
     * <p>
     * This exception is thrown when:
     * - The attribute value is null (for non-nullable types)
     * - The Double value is NaN or Infinite
     * - The list is null or empty
     * - The list contains only null or invalid values after filtering
     */
    public static class InvalidAttributeValueException extends IllegalArgumentException
    {
        public InvalidAttributeValueException(String message)
        {
            super(message);
        }

        public InvalidAttributeValueException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }

    /**
     * Exception thrown when an error occurs while setting an attribute on a span.
     * <p>
     * This exception is thrown when:
     * - No active span is available in the current context
     * - An error occurs while retrieving the current span
     * - An error occurs while setting the attribute on the span
     */
    public static class SpanAttributeException extends RuntimeException
    {
        public SpanAttributeException(String message)
        {
            super(message);
        }

        public SpanAttributeException(String message, Throwable cause)
        {
            super(message, cause);
        }
    }
}

