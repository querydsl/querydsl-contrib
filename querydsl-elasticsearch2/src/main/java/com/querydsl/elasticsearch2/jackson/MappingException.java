package com.querydsl.elasticsearch2.jackson;

/**
 * Mapping exception for factory purposes.
 *
 * @author Kevin Leturc
 */
public class MappingException extends RuntimeException {

    /**
     * Default constructor.
     *
     * @param message The message.
     * @param cause   The cause.
     */
    public MappingException(String message, Throwable cause) {
        super(message, cause);
    }

}
