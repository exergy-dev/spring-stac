package org.stac.repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.stac.model.AggregationRequest;
import org.stac.model.BulkOperationResult;
import org.stac.model.Collection;
import org.stac.model.Item;
import org.stac.model.ItemCollection;
import org.stac.model.ItemQueryCriteria;
import org.stac.model.SearchCriteria;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Repository interface for STAC API data access operations.
 * <p>
 * This interface defines the contract for all STAC repository implementations,
 * supporting multiple backend stores such as pgSTAC (PostgreSQL) and Elasticsearch.
 * Implementations are selected based on configuration properties.
 * <p>
 * All operations may throw {@link org.stac.exception.RepositoryException} for
 * underlying storage errors and {@link org.stac.exception.NotFoundException}
 * when requested resources do not exist.
 *
 * @see org.stac.repository.pgstac.PgStacRepositoryImpl
 */
public interface StacRepository {

    // ========================================================================
    // Collection Operations
    // ========================================================================

    /**
     * Retrieves all collections with pagination support.
     *
     * @param pageable pagination information (page number, size, sort)
     * @return a page of collections
     */
    Page<Collection> findAllCollections(Pageable pageable);

    /**
     * Retrieves a collection by its unique identifier.
     *
     * @param id the collection identifier
     * @return an Optional containing the collection if found, or empty if not found
     */
    Optional<Collection> findCollectionById(String id);

    /**
     * Saves a collection to the repository.
     * <p>
     * If a collection with the same ID exists, it will be updated.
     * Otherwise, a new collection will be created.
     *
     * @param collection the collection to save
     * @return the saved collection
     * @throws org.stac.exception.RepositoryException if the save operation fails
     */
    Collection saveCollection(Collection collection);

    /**
     * Deletes a collection by its identifier.
     * <p>
     * This operation may also delete all items within the collection,
     * depending on the backend implementation.
     *
     * @param id the collection identifier
     * @throws org.stac.exception.NotFoundException   if the collection does not exist
     * @throws org.stac.exception.RepositoryException if the delete operation fails
     */
    void deleteCollection(String id);

    /**
     * Counts the total number of collections in the repository.
     *
     * @return the total collection count
     */
    long countCollections();

    // ========================================================================
    // Item Operations
    // ========================================================================

    /**
     * Retrieves items within a specific collection with filtering and pagination.
     *
     * @param collectionId the collection identifier
     * @param criteria     optional query criteria for filtering items
     * @param pageable     pagination information (page number, size, sort)
     * @return a page of items matching the criteria
     * @throws org.stac.exception.NotFoundException if the collection does not exist
     */
    Page<Item> findItemsByCollection(String collectionId, ItemQueryCriteria criteria, Pageable pageable);

    /**
     * Retrieves a specific item by collection and item identifiers.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @return an Optional containing the item if found, or empty if not found
     */
    Optional<Item> findItemById(String collectionId, String itemId);

    /**
     * Saves an item to the specified collection.
     * <p>
     * If an item with the same ID exists in the collection, it will be updated.
     * Otherwise, a new item will be created.
     *
     * @param collectionId the collection identifier
     * @param item         the item to save
     * @return the saved item
     * @throws org.stac.exception.NotFoundException   if the collection does not exist
     * @throws org.stac.exception.RepositoryException if the save operation fails
     */
    Item saveItem(String collectionId, Item item);

    /**
     * Deletes an item from the specified collection.
     *
     * @param collectionId the collection identifier
     * @param itemId       the item identifier
     * @throws org.stac.exception.NotFoundException   if the item or collection does not exist
     * @throws org.stac.exception.RepositoryException if the delete operation fails
     */
    void deleteItem(String collectionId, String itemId);

    /**
     * Counts the total number of items in a specific collection.
     *
     * @param collectionId the collection identifier
     * @return the item count for the collection
     * @throws org.stac.exception.NotFoundException if the collection does not exist
     */
    long countItemsByCollection(String collectionId);

    // ========================================================================
    // Search Operations
    // ========================================================================

    /**
     * Performs a cross-collection search based on the provided criteria.
     * <p>
     * Supports spatial (bbox, intersects), temporal (datetime), and attribute
     * filtering (CQL2, query extension), as well as pagination and sorting.
     *
     * @param criteria the search criteria
     * @return an ItemCollection containing matching items and pagination links
     * @throws org.stac.exception.RepositoryException if the search operation fails
     */
    ItemCollection search(SearchCriteria criteria);

    // ========================================================================
    // Aggregation Operations
    // ========================================================================

    /**
     * Performs aggregation queries on STAC items.
     * <p>
     * Supports various aggregation types depending on field type:
     * numeric statistics, term frequencies, date histograms, etc.
     *
     * @param request the aggregation request specifying fields and optional filters
     * @return a map of aggregation results keyed by field name
     * @throws org.stac.exception.RepositoryException if the aggregation operation fails
     */
    Map<String, Object> aggregate(AggregationRequest request);

    // ========================================================================
    // Transaction Support / Bulk Operations
    // ========================================================================

    /**
     * Performs a bulk insert of items into a collection.
     * <p>
     * Items that already exist may cause errors depending on implementation.
     *
     * @param collectionId the collection identifier
     * @param items        the list of items to insert
     * @return the result of the bulk operation including success/failure counts
     * @throws org.stac.exception.NotFoundException   if the collection does not exist
     * @throws org.stac.exception.RepositoryException if the bulk operation fails entirely
     */
    BulkOperationResult bulkInsert(String collectionId, List<Item> items);

    /**
     * Performs a bulk update of items in a collection.
     * <p>
     * Items that do not exist may cause errors depending on implementation.
     *
     * @param collectionId the collection identifier
     * @param items        the list of items to update
     * @return the result of the bulk operation including success/failure counts
     * @throws org.stac.exception.NotFoundException   if the collection does not exist
     * @throws org.stac.exception.RepositoryException if the bulk operation fails entirely
     */
    BulkOperationResult bulkUpdate(String collectionId, List<Item> items);

    /**
     * Performs a bulk delete of items from a collection.
     *
     * @param collectionId the collection identifier
     * @param itemIds      the list of item identifiers to delete
     * @return the result of the bulk operation including success/failure counts
     * @throws org.stac.exception.NotFoundException   if the collection does not exist
     * @throws org.stac.exception.RepositoryException if the bulk operation fails entirely
     */
    BulkOperationResult bulkDelete(String collectionId, List<String> itemIds);
}
