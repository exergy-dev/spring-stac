package org.stac.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.stac.model.Collection;
import org.stac.model.Item;

import java.util.Iterator;
import java.util.Map;

/**
 * Service for applying JSON Merge Patch operations following RFC 7386.
 * <p>
 * JSON Merge Patch semantics:
 * - Null values in patch = delete property from original
 * - Non-null values = replace or add property
 * - Arrays are replaced entirely (not merged)
 * - Nested objects are recursively merged
 *
 * @see <a href="https://tools.ietf.org/html/rfc7386">RFC 7386 - JSON Merge Patch</a>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class JsonMergePatchService {

    private final ObjectMapper objectMapper;

    /**
     * Applies a JSON Merge Patch to an object.
     *
     * @param original    the original object
     * @param patch       the patch to apply
     * @param targetClass the class of the target object
     * @param <T>         the type of object
     * @return the patched object
     */
    public <T> T applyPatch(T original, JsonNode patch, Class<T> targetClass) {
        if (original == null) {
            throw new IllegalArgumentException("Original object cannot be null");
        }
        if (patch == null || patch.isNull()) {
            return original;
        }

        log.debug("Applying patch to {}", targetClass.getSimpleName());

        // Convert original to JsonNode
        JsonNode originalNode = objectMapper.valueToTree(original);

        // Apply merge patch
        JsonNode mergedNode = mergePatch(originalNode, patch);

        // Convert back to target class
        return objectMapper.convertValue(mergedNode, targetClass);
    }

    /**
     * Applies a JSON Merge Patch to a Collection, preserving immutable fields.
     *
     * @param original the original collection
     * @param patch    the patch to apply
     * @return the patched collection
     */
    public Collection applyCollectionPatch(Collection original, JsonNode patch) {
        if (original == null) {
            throw new IllegalArgumentException("Original collection cannot be null");
        }

        String originalId = original.getId();

        Collection patched = applyPatch(original, patch, Collection.class);

        // Preserve immutable fields
        patched.setId(originalId);
        patched.setType("Collection");

        return patched;
    }

    /**
     * Applies a JSON Merge Patch to an Item, preserving immutable fields.
     *
     * @param original the original item
     * @param patch    the patch to apply
     * @return the patched item
     */
    public Item applyItemPatch(Item original, JsonNode patch) {
        if (original == null) {
            throw new IllegalArgumentException("Original item cannot be null");
        }

        String originalId = original.getId();
        String originalCollection = original.getCollection();

        Item patched = applyPatch(original, patch, Item.class);

        // Preserve immutable fields
        patched.setId(originalId);
        patched.setCollection(originalCollection);
        patched.setType("Feature");

        return patched;
    }

    /**
     * Recursively merges a patch into a target JSON node following RFC 7386.
     *
     * @param target the target node to merge into
     * @param patch  the patch node
     * @return the merged result
     */
    private JsonNode mergePatch(JsonNode target, JsonNode patch) {
        if (patch == null || patch.isNull()) {
            return target;
        }

        if (!patch.isObject()) {
            // Non-object patch replaces target entirely
            return patch;
        }

        // If target is not an object, start with empty object
        ObjectNode result;
        if (target == null || !target.isObject()) {
            result = objectMapper.createObjectNode();
        } else {
            result = target.deepCopy();
        }

        // Iterate through patch fields
        Iterator<Map.Entry<String, JsonNode>> fields = patch.fields();
        while (fields.hasNext()) {
            Map.Entry<String, JsonNode> field = fields.next();
            String fieldName = field.getKey();
            JsonNode patchValue = field.getValue();

            if (patchValue.isNull()) {
                // Null in patch means remove the field
                result.remove(fieldName);
            } else if (patchValue.isObject() && result.has(fieldName) && result.get(fieldName).isObject()) {
                // Recursively merge objects
                JsonNode mergedChild = mergePatch(result.get(fieldName), patchValue);
                result.set(fieldName, mergedChild);
            } else {
                // Replace or add value (arrays are replaced entirely per RFC 7386)
                result.set(fieldName, patchValue.deepCopy());
            }
        }

        return result;
    }
}
