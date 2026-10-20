package uk.co.nstauthority.licensingmanagementservice.mvc;

import java.util.HashMap;
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

  protected V get(K key) {
    var requestAttributes = RequestContextHolder.getRequestAttributes();
    if (requestAttributes == null) {
      return load(key);
    }
    return getValuesByKey(requestAttributes).computeIfAbsent(key, this::load);
  }

  @SuppressWarnings("unchecked")
  private Map<K, V> getValuesByKey(RequestAttributes requestAttributes) {
    var valuesByKey = (Map<K, V>) requestAttributes.getAttribute(requestAttributeName, RequestAttributes.SCOPE_REQUEST);
    if (valuesByKey == null) {
      valuesByKey = new HashMap<>();
      requestAttributes.setAttribute(requestAttributeName, valuesByKey, RequestAttributes.SCOPE_REQUEST);
    }
    return valuesByKey;
  }
}
