package org.stac.exception;

/**
 * Exception thrown when a resource conflict occurs.
 * <p>
 * Used when attempting to create a resource that already exists.
 */
public class ConflictException extends RuntimeException {

    private final String resourceType;
    private final String resourceId;

    /**
     * Creates a new ConflictException with a message.
     *
     * @param message the detail message
     */
    public ConflictException(String message) {
        super(message);
        this.resourceType = null;
        this.resourceId = null;
    }

    /**
     * Creates a new ConflictException for a specific resource.
     *
     * @param resourceType the type of resource (e.g., "Collection", "Item")
     * @param resourceId   the ID of the resource that conflicts
     */
    public ConflictException(String resourceType, String resourceId) {
        super(String.format("%s already exists: %s", resourceType, resourceId));
        this.resourceType = resourceType;
        this.resourceId = resourceId;
    }

    /**
     * Gets the type of resource that caused the conflict.
     *
     * @return the resource type, or null if not specified
     */
    public String getResourceType() {
        return resourceType;
    }

    /**
     * Gets the ID of the resource that caused the conflict.
     *
     * @return the resource ID, or null if not specified
     */
    public String getResourceId() {
        return resourceId;
    }

    /**
     * Creates a ConflictException for a collection.
     *
     * @param collectionId the collection ID
     * @return ConflictException for the collection
     */
    public static ConflictException collection(String collectionId) {
        return new ConflictException("Collection", collectionId);
    }

    /**
     * Creates a ConflictException for an item.
     *
     * @param collectionId the collection ID
     * @param itemId       the item ID
     * @return ConflictException for the item
     */
    public static ConflictException item(String collectionId, String itemId) {
        return new ConflictException("Item",
                String.format("%s in collection %s", itemId, collectionId));
    }
}
