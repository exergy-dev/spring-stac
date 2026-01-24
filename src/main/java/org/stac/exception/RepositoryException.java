package org.stac.exception;

/**
 * Exception thrown when a repository operation fails.
 * <p>
 * Wraps underlying database or storage exceptions with STAC-specific context.
 */
public class RepositoryException extends RuntimeException {

    private final String operation;

    /**
     * Creates a new RepositoryException with a message.
     *
     * @param message the detail message
     */
    public RepositoryException(String message) {
        super(message);
        this.operation = null;
    }

    /**
     * Creates a new RepositoryException with a message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public RepositoryException(String message, Throwable cause) {
        super(message, cause);
        this.operation = null;
    }

    /**
     * Creates a new RepositoryException for a specific operation.
     *
     * @param operation the operation that failed
     * @param message   the detail message
     * @param cause     the underlying cause
     */
    public RepositoryException(String operation, String message, Throwable cause) {
        super(String.format("Operation '%s' failed: %s", operation, message), cause);
        this.operation = operation;
    }

    /**
     * Gets the operation that failed.
     *
     * @return the operation name, or null if not specified
     */
    public String getOperation() {
        return operation;
    }

    /**
     * Creates a RepositoryException for a search operation.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     * @return RepositoryException for the search operation
     */
    public static RepositoryException search(String message, Throwable cause) {
        return new RepositoryException("search", message, cause);
    }

    /**
     * Creates a RepositoryException for a save operation.
     *
     * @param resourceType the type of resource being saved
     * @param cause        the underlying cause
     * @return RepositoryException for the save operation
     */
    public static RepositoryException save(String resourceType, Throwable cause) {
        return new RepositoryException("save",
                String.format("Failed to save %s", resourceType), cause);
    }

    /**
     * Creates a RepositoryException for a delete operation.
     *
     * @param resourceType the type of resource being deleted
     * @param resourceId   the ID of the resource
     * @param cause        the underlying cause
     * @return RepositoryException for the delete operation
     */
    public static RepositoryException delete(String resourceType, String resourceId, Throwable cause) {
        return new RepositoryException("delete",
                String.format("Failed to delete %s: %s", resourceType, resourceId), cause);
    }

    /**
     * Creates a RepositoryException for a bulk operation.
     *
     * @param operationType the type of bulk operation
     * @param cause         the underlying cause
     * @return RepositoryException for the bulk operation
     */
    public static RepositoryException bulk(String operationType, Throwable cause) {
        return new RepositoryException("bulk_" + operationType,
                String.format("Bulk %s operation failed", operationType), cause);
    }
}
