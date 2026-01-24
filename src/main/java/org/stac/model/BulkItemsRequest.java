package org.stac.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Request model for bulk item operations.
 * <p>
 * Supports bulk insert, upsert, and delete operations on items within a collection.
 * This provides parity with stac-fastapi-pgstac's bulk_items endpoint.
 *
 * @see <a href="https://github.com/stac-utils/stac-fastapi-pgstac">stac-fastapi-pgstac</a>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class BulkItemsRequest {

    /**
     * The list of items to process.
     * Required for insert and upsert operations.
     * For delete operations, only the item IDs are used.
     */
    @Valid
    @NotEmpty(message = "Items list cannot be empty")
    @JsonProperty("items")
    private List<Item> items;

    /**
     * The bulk operation method to perform.
     * <ul>
     *   <li><b>insert</b> - Insert new items; fails if any item already exists</li>
     *   <li><b>upsert</b> - Insert new items or update existing ones (default)</li>
     *   <li><b>delete</b> - Delete items by their IDs</li>
     * </ul>
     */
    @Pattern(regexp = "^(insert|upsert|delete)$", message = "Method must be one of: insert, upsert, delete")
    @JsonProperty("method")
    @Builder.Default
    private String method = "upsert";

    /**
     * Returns whether this is an insert operation.
     *
     * @return true if method is "insert"
     */
    public boolean isInsert() {
        return "insert".equalsIgnoreCase(method);
    }

    /**
     * Returns whether this is an upsert operation.
     *
     * @return true if method is "upsert" or null (default)
     */
    public boolean isUpsert() {
        return method == null || "upsert".equalsIgnoreCase(method);
    }

    /**
     * Returns whether this is a delete operation.
     *
     * @return true if method is "delete"
     */
    public boolean isDelete() {
        return "delete".equalsIgnoreCase(method);
    }
}
