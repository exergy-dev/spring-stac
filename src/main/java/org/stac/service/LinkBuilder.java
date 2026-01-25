package org.stac.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.stac.model.Collection;
import org.stac.model.Item;
import org.stac.model.ItemCollection;
import org.stac.model.Link;
import org.stac.model.SearchRequest;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for building STAC-compliant links for collections, items, and search results.
 * <p>
 * Consolidates link building logic previously duplicated across StacService methods.
 */
@Service
public class LinkBuilder {

    @Value("${stac.api.root-url:http://localhost:8080}")
    private String rootUrl;

    private static final String APPLICATION_JSON = "application/json";
    private static final String APPLICATION_GEOJSON = "application/geo+json";

    /**
     * Builds standard STAC links for a collection.
     *
     * @param collection the collection to add links to
     * @return the collection with links added
     */
    public Collection addCollectionLinks(Collection collection) {
        List<Link> links = new ArrayList<>();

        if (collection.getLinks() != null) {
            links.addAll(collection.getLinks());
        }

        String collectionUrl = rootUrl + "/collections/" + collection.getId();

        links.add(buildLink(collectionUrl, "self", APPLICATION_JSON, "This collection"));
        links.add(buildLink(rootUrl, "root", APPLICATION_JSON, "Root catalog"));
        links.add(buildLink(rootUrl, "parent", APPLICATION_JSON, "Parent catalog"));
        links.add(buildLink(collectionUrl + "/items", "items", APPLICATION_GEOJSON, "Items in this collection"));

        collection.setLinks(links);
        return collection;
    }

    /**
     * Builds standard STAC links for an item.
     *
     * @param item         the item to add links to
     * @param collectionId the collection ID (may be null, in which case item.getCollection() is used)
     * @return the item with links added
     */
    public Item addItemLinks(Item item, String collectionId) {
        List<Link> links = new ArrayList<>();

        if (item.getLinks() != null) {
            links.addAll(item.getLinks());
        }

        String effectiveCollectionId = collectionId != null ? collectionId : item.getCollection();
        String collectionUrl = rootUrl + "/collections/" + effectiveCollectionId;
        String itemUrl = collectionUrl + "/items/" + item.getId();

        links.add(buildLink(itemUrl, "self", APPLICATION_GEOJSON, "This item"));
        links.add(buildLink(rootUrl, "root", APPLICATION_JSON, "Root catalog"));
        links.add(buildLink(collectionUrl, "parent", APPLICATION_JSON, "Parent collection"));
        links.add(buildLink(collectionUrl, "collection", APPLICATION_JSON, "Collection"));

        item.setLinks(links);
        return item;
    }

    /**
     * Builds links for an item collection (items endpoint response).
     *
     * @param collectionId the collection ID
     * @param page         the page of items
     * @param limit        the page size limit
     * @return list of links for the item collection
     */
    public List<Link> buildItemCollectionLinks(String collectionId, Page<Item> page, int limit) {
        List<Link> links = new ArrayList<>();

        String collectionUrl = rootUrl + "/collections/" + collectionId;
        String itemsUrl = collectionUrl + "/items";

        links.add(buildLink(itemsUrl, "self", APPLICATION_GEOJSON, "Items in this collection"));
        links.add(buildLink(rootUrl, "root", APPLICATION_JSON, "Root catalog"));
        links.add(buildLink(collectionUrl, "collection", APPLICATION_JSON, "Collection"));

        if (page.hasNext()) {
            links.add(buildLink(
                    itemsUrl + "?limit=" + limit + "&offset=" + (page.getNumber() + 1) * limit,
                    "next", APPLICATION_GEOJSON, "Next page"));
        }

        if (page.hasPrevious()) {
            links.add(buildLink(
                    itemsUrl + "?limit=" + limit + "&offset=" + (page.getNumber() - 1) * limit,
                    "prev", APPLICATION_GEOJSON, "Previous page"));
        }

        return links;
    }

    /**
     * Builds links for search results.
     *
     * @param request the search request
     * @param result  the search result
     * @return list of links for the search results
     */
    public List<Link> buildSearchResultLinks(SearchRequest request, ItemCollection result) {
        List<Link> links = new ArrayList<>();

        links.add(buildLink(rootUrl + "/search", "self", APPLICATION_GEOJSON, "This search"));
        links.add(buildLink(rootUrl, "root", APPLICATION_JSON, "Root catalog"));

        // Add next page link if more results are available
        if (hasMoreResults(result)) {
            links.add(Link.builder()
                    .href(rootUrl + "/search")
                    .rel("next")
                    .type(APPLICATION_GEOJSON)
                    .title("Next page")
                    .method("POST")
                    .build());
        }

        return links;
    }

    /**
     * Builds links for the collections endpoint response.
     *
     * @param page  the page of collections
     * @param limit the page size limit
     * @return list of links for the collections response
     */
    public List<Link> buildCollectionsLinks(Page<Collection> page, int limit) {
        List<Link> links = new ArrayList<>();

        links.add(buildLink(rootUrl + "/collections", "self", APPLICATION_JSON, "Collections"));
        links.add(buildLink(rootUrl, "root", APPLICATION_JSON, "Root catalog"));

        if (page.hasNext()) {
            links.add(buildLink(
                    rootUrl + "/collections?limit=" + limit + "&offset=" + (page.getNumber() + 1) * limit,
                    "next", APPLICATION_JSON, "Next page"));
        }

        return links;
    }

    /**
     * Builds links for aggregation results.
     *
     * @return list of links for aggregation response
     */
    public List<Link> buildAggregationLinks() {
        List<Link> links = new ArrayList<>();
        links.add(buildLink(rootUrl + "/aggregate", "self", APPLICATION_JSON, "Aggregation results"));
        links.add(buildLink(rootUrl, "root", APPLICATION_JSON, "Root catalog"));
        return links;
    }

    /**
     * Gets the configured root URL.
     *
     * @return the root URL
     */
    public String getRootUrl() {
        return rootUrl;
    }

    private Link buildLink(String href, String rel, String type, String title) {
        return Link.builder()
                .href(href)
                .rel(rel)
                .type(type)
                .title(title)
                .build();
    }

    private boolean hasMoreResults(ItemCollection result) {
        return result.getContext() != null
                && result.getContext().getMatched() != null
                && result.getContext().getReturned() != null
                && result.getContext().getMatched() > result.getContext().getReturned();
    }
}
