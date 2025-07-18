package uk.co.nstauthority.licensingmanagementservice.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EmptySource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class FilterUtilTest {
  private enum TestType {TYPE_A, TYPE_B, TYPE_C}
  
  @ParameterizedTest
  @NullAndEmptySource
  void filterTextInput_whenBlank_thenTrue(String input) {
    assertTrue(FilterUtil.filterTextInput("text", input));
  }

  @Test
  void filterTextInput_whenMatch_thenTrue() {
    assertTrue(FilterUtil.filterTextInput("Text on item", "text"));
  }

  @Test
  void filterTextInput_whenNoMatch_thenFalse() {
    assertFalse(FilterUtil.filterTextInput("Text on item", "no match"));
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = "invalid")
  void filterEnum_whenNotValidEnum_thenTrue(String input) {
    assertTrue(FilterUtil.filterEnum(TestType.class, TestType.TYPE_A, List.of(input)));
  }

  @Test
  void filterEnum_whenMatch_thenTrue() {
    assertTrue(FilterUtil.filterEnum(TestType.class, TestType.TYPE_A, List.of(TestType.TYPE_A.name(), TestType.TYPE_B.name())));
  }

  @Test
  void filterEnum_whenNoMatch_thenFalse() {
    assertFalse(FilterUtil.filterEnum(TestType.class, TestType.TYPE_A, List.of(TestType.TYPE_B.name(), TestType.TYPE_C.name())));
  }

  @Test
  void filterIdList_whenFilterIsNull_returnsTrue() {
    List<Integer> idList = List.of(1, 2, 3);
    assertTrue(FilterUtil.filterIdList(idList, null));
  }

  @Test
  void filterIdList_whenListIsNull_returnsFalse() {
    assertFalse(FilterUtil.filterIdList(null, 1));
  }

  @Test
  void filterIdList_whenListIsEmpty_returnsFalse() {
    assertFalse(FilterUtil.filterIdList(List.of(), 1));
  }

  @Test
  void filterIdList_whenListContainsMatchingId_returnsTrue() {
    List<Integer> idList = List.of(10, 20, 30);
    assertTrue(FilterUtil.filterIdList(idList, 20));
  }

  @Test
  void filterIdList_whenListDoesNotContainMatchingId_returnsFalse() {
    List<Integer> idList = List.of(10, 20, 30);
    assertFalse(FilterUtil.filterIdList(idList, 40));
  }
}