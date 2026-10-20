package uk.co.nstauthority.licensingmanagementservice.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

class RequestScopedCacheTest {

  private final CountingCache cache = new CountingCache();

  @BeforeEach
  @AfterEach
  void resetRequestAttributes() {
    RequestContextHolder.resetRequestAttributes();
  }

  @Test
  void get_whenNotInWebRequest_thenLoadsEveryTime() {
    var firstResult = cache.get("a");
    var secondResult = cache.get("a");

    assertThat(firstResult).isEqualTo("loaded a");
    assertThat(secondResult).isEqualTo("loaded a");
    assertThat(cache.loadedKeys).containsExactly("a", "a");
  }

  @Test
  void get_whenInWebRequest_thenLoadsEachKeyOnce() {
    givenWebRequest();

    var firstResult = cache.get("a");
    var secondResult = cache.get("a");
    var otherKeyResult = cache.get("b");

    assertThat(firstResult).isEqualTo("loaded a");
    assertThat(secondResult).isEqualTo("loaded a");
    assertThat(otherKeyResult).isEqualTo("loaded b");
    assertThat(cache.loadedKeys).containsExactly("a", "b");
  }

  @Test
  void get_whenNextWebRequest_thenLoadsAgain() {
    givenWebRequest();
    cache.get("a");
    givenWebRequest();

    cache.get("a");

    assertThat(cache.loadedKeys).containsExactly("a", "a");
  }

  @Test
  void get_whenAnotherCacheUsesTheSameKeyInTheSameRequest_thenEachKeepsItsOwnValue() {
    givenWebRequest();
    var otherCache = new OtherCountingCache();

    var result = cache.get("a");
    var otherResult = otherCache.get("a");

    assertThat(result).isEqualTo("loaded a");
    assertThat(otherResult).isEqualTo("other a");
  }

  @Test
  void getAll_whenNotInWebRequest_thenLoadsAllKeysTogetherEveryTime() {
    var batchCache = new BatchCountingCache();

    var firstResult = batchCache.getAll(List.of("a", "b", "a"));
    var secondResult = batchCache.getAll(List.of("a", "b"));

    assertThat(firstResult).containsExactly(Map.entry("a", "batch a"), Map.entry("b", "batch b"));
    assertThat(secondResult).containsExactly(Map.entry("a", "batch a"), Map.entry("b", "batch b"));
    assertThat(batchCache.loadedBatches).containsExactly(List.of("a", "b"), List.of("a", "b"));
  }

  @Test
  void getAll_whenInWebRequest_thenLoadsOnlyMissingKeysTogether() {
    givenWebRequest();
    var batchCache = new BatchCountingCache();
    batchCache.getAll(List.of("a"));

    var result = batchCache.getAll(List.of("c", "a", "b"));
    var repeatedResult = batchCache.getAll(List.of("b", "c"));

    assertThat(result).containsExactly(
        Map.entry("c", "batch c"),
        Map.entry("a", "batch a"),
        Map.entry("b", "batch b")
    );
    assertThat(repeatedResult).containsExactly(Map.entry("b", "batch b"), Map.entry("c", "batch c"));
    assertThat(batchCache.loadedBatches).containsExactly(List.of("a"), List.of("c", "b"));
  }

  @Test
  void getAll_whenNoKeys_thenLoadsNothing() {
    var batchCache = new BatchCountingCache();

    var result = batchCache.getAll(List.of());

    assertThat(result).isEmpty();
    assertThat(batchCache.loadedBatches).isEmpty();
  }

  @Test
  void getAll_whenLoadAllNotOverridden_thenLoadsEachKey() {
    var result = cache.getAll(List.of("a", "b"));

    assertThat(result).containsExactly(Map.entry("a", "loaded a"), Map.entry("b", "loaded b"));
    assertThat(cache.loadedKeys).containsExactly("a", "b");
  }

  private void givenWebRequest() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
  }

  private static class BatchCountingCache extends RequestScopedCache<String, String> {

    private final List<List<String>> loadedBatches = new ArrayList<>();

    @Override
    protected String load(String key) {
      throw new UnsupportedOperationException("Keys are only loaded in batches");
    }

    @Override
    protected Map<String, String> loadAll(Collection<String> keys) {
      loadedBatches.add(List.copyOf(keys));
      var valuesByKey = new LinkedHashMap<String, String>();
      keys.forEach(key -> valuesByKey.put(key, "batch " + key));
      return valuesByKey;
    }
  }

  private static class CountingCache extends RequestScopedCache<String, String> {

    private final List<String> loadedKeys = new ArrayList<>();

    @Override
    protected String load(String key) {
      loadedKeys.add(key);
      return "loaded " + key;
    }
  }

  private static class OtherCountingCache extends RequestScopedCache<String, String> {

    @Override
    protected String load(String key) {
      return "other " + key;
    }
  }
}
