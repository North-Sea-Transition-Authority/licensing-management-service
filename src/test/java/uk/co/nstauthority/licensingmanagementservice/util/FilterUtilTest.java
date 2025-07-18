package uk.co.nstauthority.licensingmanagementservice.util;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class FilterUtilTest {
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
}