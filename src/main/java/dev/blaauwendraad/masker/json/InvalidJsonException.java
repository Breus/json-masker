package dev.blaauwendraad.masker.json;

/**
 * Calling any of the mask methods of the {@link JsonMasker} can result in two cases:
 *
 * <ol>
 *   <li>In case valid JSON was provided, valid JSON is returned according to the provided masking
 *       configurations
 *   <li>In case invalid JSON was provided, the exception defined in this class is thrown
 * </ol>
 */
public class InvalidJsonException extends RuntimeException {
    public InvalidJsonException(String message, Throwable cause) {
        super(message, cause);
    }
    public InvalidJsonException(String message) {
        super(message);
    }
}
