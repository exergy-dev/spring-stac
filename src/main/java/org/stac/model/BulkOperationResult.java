package org.stac.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

/**
 * Result model for bulk operation responses.
 * <p>
 * Provides detailed feedback on bulk insert, update, or delete operations,
 * including success/failure counts and specific error messages.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BulkOperationResult {

    /**
     * Number of operations that completed successfully.
     */
    private int success;

    /**
     * Number of operations that failed.
     */
    private int failed;

    /**
     * List of error messages from failed operations.
     * Each message should identify the item and describe the error.
     */
    @Builder.Default
    private List<String> errors = new ArrayList<>();

    /**
     * List of item IDs affected by the operation.
     * For successful operations, contains the IDs of items that were processed.
     */
    @Builder.Default
    private List<String> items = new ArrayList<>();

    /**
     * Creates a successful result with no failures.
     *
     * @param itemIds the IDs of successfully processed items
     * @return BulkOperationResult with all successes
     */
    public static BulkOperationResult success(List<String> itemIds) {
        return BulkOperationResult.builder()
                .success(itemIds.size())
                .failed(0)
                .items(new ArrayList<>(itemIds))
                .errors(new ArrayList<>())
                .build();
    }

    /**
     * Creates a failed result with no successes.
     *
     * @param errorMessages the error messages describing failures
     * @return BulkOperationResult with all failures
     */
    public static BulkOperationResult failure(List<String> errorMessages) {
        return BulkOperationResult.builder()
                .success(0)
                .failed(errorMessages.size())
                .items(new ArrayList<>())
                .errors(new ArrayList<>(errorMessages))
                .build();
    }

    /**
     * Creates a partial result with both successes and failures.
     *
     * @param successItemIds the IDs of successfully processed items
     * @param errorMessages  the error messages for failed items
     * @return BulkOperationResult with partial success
     */
    public static BulkOperationResult partial(List<String> successItemIds, List<String> errorMessages) {
        return BulkOperationResult.builder()
                .success(successItemIds.size())
                .failed(errorMessages.size())
                .items(new ArrayList<>(successItemIds))
                .errors(new ArrayList<>(errorMessages))
                .build();
    }

    /**
     * Creates an empty result (no operations performed).
     *
     * @return BulkOperationResult with zero counts
     */
    public static BulkOperationResult empty() {
        return BulkOperationResult.builder()
                .success(0)
                .failed(0)
                .items(new ArrayList<>())
                .errors(new ArrayList<>())
                .build();
    }

    /**
     * Checks if all operations were successful.
     *
     * @return true if failed count is zero and success count is positive
     */
    public boolean isFullySuccessful() {
        return failed == 0 && success > 0;
    }

    /**
     * Checks if all operations failed.
     *
     * @return true if success count is zero and failed count is positive
     */
    public boolean isFullyFailed() {
        return success == 0 && failed > 0;
    }

    /**
     * Checks if there was partial success.
     *
     * @return true if both success and failed counts are positive
     */
    public boolean isPartiallySuccessful() {
        return success > 0 && failed > 0;
    }

    /**
     * Gets the total number of operations attempted.
     *
     * @return sum of success and failed counts
     */
    public int getTotal() {
        return success + failed;
    }

    /**
     * Gets the success rate as a percentage.
     *
     * @return percentage of successful operations (0-100)
     */
    public double getSuccessRate() {
        int total = getTotal();
        return total > 0 ? (success * 100.0) / total : 0.0;
    }

    /**
     * Adds a successful item to the result.
     *
     * @param itemId the ID of the successfully processed item
     */
    public void addSuccess(String itemId) {
        this.success++;
        if (this.items == null) {
            this.items = new ArrayList<>();
        }
        this.items.add(itemId);
    }

    /**
     * Adds a failed item to the result.
     *
     * @param errorMessage the error message describing the failure
     */
    public void addFailure(String errorMessage) {
        this.failed++;
        if (this.errors == null) {
            this.errors = new ArrayList<>();
        }
        this.errors.add(errorMessage);
    }

    /**
     * Merges another result into this one.
     * Useful for combining results from parallel operations.
     *
     * @param other the result to merge
     * @return this result with merged counts and messages
     */
    public BulkOperationResult merge(BulkOperationResult other) {
        if (other == null) {
            return this;
        }
        this.success += other.success;
        this.failed += other.failed;
        if (other.items != null) {
            if (this.items == null) {
                this.items = new ArrayList<>();
            }
            this.items.addAll(other.items);
        }
        if (other.errors != null) {
            if (this.errors == null) {
                this.errors = new ArrayList<>();
            }
            this.errors.addAll(other.errors);
        }
        return this;
    }
}
