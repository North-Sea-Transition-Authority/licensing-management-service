package uk.co.nstauthority.licensingmanagementservice.util.enumutil;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;

class EnumValidationUtilTest {

  @Test
  void isValidEnumValue_whenEnumValue_thenTrue() {
    var isValidEnum = EnumValidationUtil.isValidEnumValue(EnumTest.class, EnumTest.ENUM_TEST.name());
    assertTrue(isValidEnum);
  }

  @Test
  void isValidEnumValue_whenNotEnumValue_thenFalse() {
    var isValidEnum = EnumValidationUtil.isValidEnumValue(EnumTest.class, "non-enum value");
    assertFalse(isValidEnum);
  }

  @Test
  void isNotValidEnumValue_whenNotEnumValue_thenTrue() {
    var isNotValidEnum = EnumValidationUtil.isNotValidEnumValue(EnumTest.class, "non-enum value");
    assertTrue(isNotValidEnum);
  }

  @Test
  void isNotValidEnumValue_whenEnumValue_thenFalse() {
    var isNotValidEnum = EnumValidationUtil.isNotValidEnumValue(EnumTest.class, EnumTest.ENUM_TEST.name());
    assertFalse(isNotValidEnum);
  }

  @Test
  void containsInvalidEnumValue_list_whenEnumValue_thenFalse() {
    var isNotValidEnum = EnumValidationUtil.containsInvalidEnumValue(EnumTest.class, List.of(EnumTest.ENUM_TEST.name()));
    assertFalse(isNotValidEnum);
  }

  @Test
  void containsInvalidEnumValue_list_whenNotEnumValue_thenTrue() {
    var isNotValidEnum = EnumValidationUtil.containsInvalidEnumValue(EnumTest.class, List.of("non-enum value"));
    assertTrue(isNotValidEnum);
  }

  @ParameterizedTest
  @NullAndEmptySource
  void containsInvalidEnumValue_listWithInvalidValues_thenTrue(List<String> blankValue) {
    var isNotValidEnum = EnumValidationUtil.containsInvalidEnumValue(
        EnumTest.class,
        blankValue
    );
    assertTrue(isNotValidEnum);
  }

  @Test
  void containsInvalidEnumValue_listWithMultipleValues_whenOneIsNotEnumValue_thenTrue() {
    var isNotValidEnum = EnumValidationUtil.containsInvalidEnumValue(
        EnumTest.class,
        List.of("non-enum value", EnumTest.ENUM_TEST.name())
    );
    assertTrue(isNotValidEnum);
  }

  enum EnumTest {
    ENUM_TEST
  }
}