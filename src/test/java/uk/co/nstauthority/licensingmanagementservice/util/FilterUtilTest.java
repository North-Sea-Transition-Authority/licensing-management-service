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
  void matchesTextInput_Filter_whenBlank_thenTrue(String input) {
    assertTrue(FilterUtil.matchesTextInput("text", input));
  }

  @Test
  void matchesTextInput_Filter_whenMatch_thenTrue() {
    assertTrue(FilterUtil.matchesTextInput("Text on item", "text"));
  }

  @Test
  void matchesTextInput_Filter_whenNoMatch_thenFalse() {
    assertFalse(FilterUtil.matchesTextInput("Text on item", "no match"));
  }

  @ParameterizedTest
  @EmptySource
  @ValueSource(strings = "invalid")
  void matchesEnum_whenNotValidEnum_Filter_thenTrue(String input) {
    assertTrue(FilterUtil.matchesEnum(TestType.class, TestType.TYPE_A, List.of(input)));
  }

  @Test
  void matchesEnum_Filter_whenMatch_thenTrue() {
    assertTrue(FilterUtil.matchesEnum(TestType.class, TestType.TYPE_A, List.of(TestType.TYPE_A.name(), TestType.TYPE_B.name())));
  }

  @Test
  void matchesEnum_Filter_whenNoMatch_thenFalse() {
    assertFalse(FilterUtil.matchesEnum(TestType.class, TestType.TYPE_A, List.of(TestType.TYPE_B.name(), TestType.TYPE_C.name())));
  }

  @Test
  void filterIdList_whenMatchesIsNull_returnsTrueFilter() {
    List<Integer> idList = List.of(1, 2, 3);
    assertTrue(FilterUtil.matchesIdList(idList, null));
  }

  @Test
  void matchesIdList_whenListFilterIsNull_returnsFalse() {
    assertFalse(FilterUtil.matchesIdList(null, 1));
  }

  @Test
  void matchesIdList_whenListFilterIsEmpty_returnsFalse() {
    assertFalse(FilterUtil.matchesIdList(List.of(), 1));
  }

  @Test
  void matchesIdList_whenListContainsMatchingId_returnsTrueFilter() {
    List<Integer> idList = List.of(10, 20, 30);
    assertTrue(FilterUtil.matchesIdList(idList, 20));
  }

  @Test
  void matchesIdList_whenListDoesNotContainMatchingId_returnsFalseFilter() {
    List<Integer> idList = List.of(10, 20, 30);
    assertFalse(FilterUtil.matchesIdList(idList, 40));
  }

  @Test
  void listMatchesIdList_whenFilterIsNull_returnsTrue() {
    assertTrue(FilterUtil.listMatchesIdList(List.of(1, 2, 3), null));
  }

  @Test
  void listMatchesIdList_whenFilterIsEmpty_returnsFalse() {
    assertFalse(FilterUtil.listMatchesIdList(List.of(1, 2, 3), List.of()));
  }

  @Test
  void listMatchesIdList_whenDataListIsNull_returnsFalse() {
    assertFalse(FilterUtil.listMatchesIdList(null, List.of(1, 2, 3)));
  }

  @Test
  void listMatchesIdList_whenDataListIsEmpty_returnsFalse() {
    assertFalse(FilterUtil.listMatchesIdList(List.of(), List.of(1, 2, 3)));
  }

  @Test
  void listMatchesIdList_whenListsHaveOverlap_returnsTrue() {
    assertTrue(FilterUtil.listMatchesIdList(List.of(10, 20, 30), List.of(20, 40)));
  }

  @Test
  void listMatchesIdList_whenListsHaveNoOverlap_returnsFalse() {
    assertFalse(FilterUtil.listMatchesIdList(List.of(10, 20, 30), List.of(40, 50)));
  }
}