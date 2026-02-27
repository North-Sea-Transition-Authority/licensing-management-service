package uk.co.nstauthority.licensingmanagementservice.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collector;
import java.util.stream.Collectors;

public class StreamUtil {
  private StreamUtil() {
  }

  public static <T, K, U> Collector<T, ?, Map<K, U>> toLinkedHashMap(Function<? super T, ? extends K> keyMapper,
                                                                     Function<? super T, ? extends U> valueMapper) {

    return Collectors.toMap(
        keyMapper,
        valueMapper,
        (u, v) -> {
          throw new IllegalStateException(String.format("Duplicate key %s", u));
        },
        LinkedHashMap::new
    );
  }

  @SafeVarargs
  public static <R> Set<R> unionSets(Set<R>... sets) {
    if (sets == null) {
      return Set.of();
    }

    return Arrays.stream(sets)
        .flatMap(Collection::stream)
        .collect(Collectors.toSet());
  }
}
