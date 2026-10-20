package uk.co.nstauthority.licensingmanagementservice.mvc;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
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

  private void givenWebRequest() {
    RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(new MockHttpServletRequest()));
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
