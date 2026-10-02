package uk.co.nstauthority.licensingmanagementservice.util;

import java.util.List;

public final class ListUtil {

  private ListUtil() {
  }

  public static List<Integer> toIntegers(List<String> values) {
    return values
        .stream()
        .map(Integer::parseInt)
        .toList();
  }

  public static List<String> toStrings(List<Integer> values) {
    return values
        .stream()
        .map(String::valueOf)
        .toList();
  }
}
