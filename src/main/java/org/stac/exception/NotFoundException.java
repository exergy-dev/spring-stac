package org.stac.exception;

/**
 * Exception thrown when a requested resource is not found.
 * <p>
 * Used for collections and items that do not exist in the repository.
 */
public class NotFoundException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    /**
     * Creates a new NotFoundException with a message.
     *
     * @param message the detail message
     */
    public NotFoundException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * Creates a new NotFoundException with a message and cause.
     *
     * @param message the detail message
     * @param cause   the underlying cause
     */
    public NotFoundException(String message, Throwable cause) {
        super(message, cause);
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * Creates a new NotFoundException for a specific resource.
     *
     * @param resourceType the type of resource (e.g., "Collection", "Item")
     * @param resourceId   the ID of the resource that was not found
     */
    public NotFoundException(String resourceType, String resourceId) {
        super(String.format("%s not found: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Gets the type of resource that was not found.
     *
     * @return the resource type, or null if not specified
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Gets the ID of the resource that was not found.
     *
     * @return the resource ID, or null if not specified
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Creates a NotFoundException for a collection.
     *
     * @param collectionId the collection ID
     * @return NotFoundException for the collection
     */
    public static NotFoundException collection(String collectionId) {
        return new NotFoundException("Collection", collectionId);
    }

    /**
     * Creates a NotFoundException for an item.
     *
     * @param collectionId the collection ID
     * @param itemId       the item ID
     * @return NotFoundException for the item
     */
    public static NotFoundException item(String collectionId, String itemId) {
        return new NotFoundException("Item",
                String.format("%s in collection %s", itemId, collectionId));
    }
}
