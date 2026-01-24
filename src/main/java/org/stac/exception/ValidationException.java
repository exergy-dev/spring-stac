package org.stac.exception;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Exception thrown when validation of STAC resources fails.
 * <p>
 * Can contain multiple validation errors for comprehensive error reporting.
 */
public class ValidationException extends RuntimeException {

    private final List<String> errors;
    private final String resourceType;

    /**
     * Creates a new ValidationException with a single error message.
     *
     * @param message the detail message
     */
    public ValidationException(String message) {
        super(message);
        this.errors = List.of(message);
        this.resourceType = null;
    }

    /**
     * Creates a new ValidationException for a specific resource type.
     *
     * @param resourceType the type of resource being validated
     * @param message      the detail message
     */
    public ValidationException(String resourceType, String message) {
        super(String.format("Validation failed for %s: %s", resourceType, message));
        this.errors = List.of(message);
        this.resourceType = resourceType;
    }

    /**
     * Creates a new ValidationException with multiple errors.
     *
     * @param resourceType the type of resource being validated
     * @param errors       the list of validation errors
     */
    public ValidationException(String resourceType, List<String> errors) {
        super(String.format("Validation failed for %s: %s",
                resourceType, String.join("; ", errors)));
        this.errors = new ArrayList<>(errors);
        this.resourceType = resourceType;
    }

    /**
     * Gets the list of validation errors.
     *
     * @return unmodifiable list of error messages
     */
    public List<String> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    /**
     * Gets the type of resource that failed validation.
     *
     * @return the resource type, or null if not specified
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Checks if there are multiple validation errors.
     *
     * @return true if there are more than one error
     */
    public boolean hasMultipleErrors() {
        return errors != null && errors.size() > 1;
    }

    /**
     * Creates a ValidationException for an Item.
     *
     * @param message the validation error message
     * @return ValidationException for Item validation
     */
    public static ValidationException item(String message) {
        return new ValidationException("Item", message);
    }

    /**
     * Creates a ValidationException for an Item with multiple errors.
     *
     * @param errors the list of validation errors
     * @return ValidationException for Item validation
     */
    public static ValidationException item(List<String> errors) {
        return new ValidationException("Item", errors);
    }

    /**
     * Creates a ValidationException for a Collection.
     *
     * @param message the validation error message
     * @return ValidationException for Collection validation
     */
    public static ValidationException collection(String message) {
        return new ValidationException("Collection", message);
    }

    /**
     * Creates a ValidationException for a Collection with multiple errors.
     *
     * @param errors the list of validation errors
     * @return ValidationException for Collection validation
     */
    public static ValidationException collection(List<String> errors) {
        return new ValidationException("Collection", errors);
    }

    /**
     * Creates a ValidationException for a SearchRequest.
     *
     * @param message the validation error message
     * @return ValidationException for SearchRequest validation
     */
    public static ValidationException searchRequest(String message) {
        return new ValidationException("SearchRequest", message);
    }

    /**
     * Creates a ValidationException for a SearchRequest with multiple errors.
     *
     * @param errors the list of validation errors
     * @return ValidationException for SearchRequest validation
     */
    public static ValidationException searchRequest(List<String> errors) {
        return new ValidationException("SearchRequest", errors);
    }
}
