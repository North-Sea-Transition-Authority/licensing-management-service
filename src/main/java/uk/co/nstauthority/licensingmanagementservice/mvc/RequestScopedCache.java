package uk.co.nstauthority.licensingmanagementservice.mvc;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;

/**
 * Base for caches that keep values for the lifetime of a single HTTP request, so repeated lookups within the request
 * share one load. Within a web request the values are held in the request's attributes. Outside one, such as in
 * scheduled jobs and event listeners, there is no request to hold them, so every lookup loads directly.
 *
 * <p>Only use this for read-only lookups: a request that changes the underlying data would be served the values as
 * they were when first loaded.</p>
 *
 * @param <K> the key values are looked up by
 * @param <V> the value held for each key
 */
public abstract class RequestScopedCache<K, V> {

  private final String requestAttributeName = getClass().getName();

  protected abstract V load(K key);

  /**
   * Returns what a key's value is held under. By default that is the key itself. Override this when the key is an
   * object without value equality, such as an entity, so that two copies of it share one value: return its id.
   */
  protected Object cacheKey(K key) {
    return key;
  }

  /**
   * Loads several keys at once, returning a value for every key. Override this when the source can fetch several keys
   * in one go; by default each key is loaded on its own.
   */
  protected Map<K, V> loadAll(Collection<K> keys) {
    var valuesByKey = new LinkedHashMap<K, V>();
    keys.forEach(key -> valuesByKey.put(key, load(key)));
    return valuesByKey;
  }

  protected V get(K key) {
    var requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      return load(key);
    }
    return getValuesByKey(requestAttributes).computeIfAbsent(cacheKey(key), ignored -> load(key));
  }

  /**
   * Returns the value for each of the given keys, in the order given and without duplicates. Within a web request only
   * the keys not already held are loaded, together in one call to {@link #loadAll}.
   */
  protected Map<K, V> getAll(Collection<K> keys) {
    var distinctKeys = new LinkedHashSet<>(keys);
    var requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      return distinctKeys.isEmpty() ? Map.of() : loadAll(distinctKeys);
    }

    var valuesByKey = getValuesByKey(requestAttributes);
    var missingKeys = distinctKeys.stream()
        .filter(key -> !valuesByKey.containsKey(cacheKey(key)))
        .toList();
    if (!missingKeys.isEmpty()) {
      loadAll(missingKeys).forEach((key, value) -> valuesByKey.put(cacheKey(key), value));
    }

    var requestedValuesByKey = new LinkedHashMap<K, V>();
    distinctKeys.forEach(key -> requestedValuesByKey.put(key, valuesByKey.get(cacheKey(key))));
    return requestedValuesByKey;
  }

  @SuppressWarnings("unchecked")
  private Map<Object, V> getValuesByKey(RequestAttributes requestAttributes) {
    var valuesByKey = (Map<Object, V>) requestAttributes.getAttribute(requestAttributeName, RequestAttributes.SCOPE_REQUEST);
    if (valuesByKey == null) {
      valuesByKey = new HashMap<>();
      requestAttributes.setAttribute(requestAttributeName, valuesByKey, RequestAttributes.SCOPE_REQUEST);
    }
    return valuesByKey;
  }
}
