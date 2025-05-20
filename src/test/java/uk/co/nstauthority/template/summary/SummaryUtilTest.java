package uk.co.nstauthority.template.summary;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.template.util.enumutil.Displayable;

class SummaryUtilTest {

  private static final String STRING_VALUE = "string value";
  private static final Integer INTEGER_VALUE = 2;
  private static final String INTEGER_VALUE_AS_STRING = "2";
  private static final Boolean BOOLEAN_VALUE = false;
  private static final String BOOLEAN_VALUE_AS_STRING = "No";
  private static final LocalDate LOCAL_DATE_VALUE = LocalDate.of(2025, 4, 15);
  private static final String LOCAL_DATE_VALUE_AS_STRING = "15 Apr 2025";
  private static final BigDecimal BIG_DECIMAL_VALUE = new BigDecimal("1234567890.123450000");
  private static final String BIG_DECIMAL_VALUE_AS_STRING = "1234567890.12345";
  private static final DisplayableTestClass DISPLAYABLE_VALUE = new DisplayableTestClass();
  private static final String DISPLAYABLE_VALUE_AS_STRING = "display name";

  @Test
  void formatAsList_Null_ReturnEmptyList() {
    assertThat(SummaryUtil.formatAsList(null))
        .isEmpty();
  }

  @Test
  void formatAsList_String_ReturnListOfStrings() {
    assertThat(SummaryUtil.formatAsList(STRING_VALUE))
        .isEqualTo(List.of(STRING_VALUE));
  }

  @Test
  void formatAsString_String_ReturnString() {
    assertThat(SummaryUtil.formatAsString(STRING_VALUE))
        .isEqualTo(STRING_VALUE);
  }

  @Test
  void formatAsList_Integer_ReturnListOfStrings() {
    assertThat(SummaryUtil.formatAsList(INTEGER_VALUE))
        .isEqualTo(List.of(INTEGER_VALUE_AS_STRING));
  }

  @Test
  void formatAsString_Integer_ReturnString() {
    assertThat(SummaryUtil.formatAsString(INTEGER_VALUE))
        .isEqualTo(INTEGER_VALUE_AS_STRING);
  }

  @Test
  void formatAsList_Boolean_ReturnListOfStrings() {
    assertThat(SummaryUtil.formatAsList(BOOLEAN_VALUE))
        .isEqualTo(List.of(BOOLEAN_VALUE_AS_STRING));
  }

  @Test
  void formatAsString_Boolean_ReturnString() {
    assertThat(SummaryUtil.formatAsString(BOOLEAN_VALUE))
        .isEqualTo(BOOLEAN_VALUE_AS_STRING);
  }

  @Test
  void formatAsList_LocalDate_ReturnListOfStrings() {
    assertThat(SummaryUtil.formatAsList(LOCAL_DATE_VALUE))
        .isEqualTo(List.of(LOCAL_DATE_VALUE_AS_STRING));
  }

  @Test
  void formatAsString_LocalDate_ReturnString() {
    assertThat(SummaryUtil.formatAsString(LOCAL_DATE_VALUE))
        .isEqualTo(LOCAL_DATE_VALUE_AS_STRING);
  }

  @Test
  void formatAsList_BigDecimal_ReturnListOfStrings() {
    assertThat(SummaryUtil.formatAsList(BIG_DECIMAL_VALUE))
        .isEqualTo(List.of(BIG_DECIMAL_VALUE_AS_STRING));
  }

  @Test
  void formatAsString_BigDecimal_ReturnString() {
    assertThat(SummaryUtil.formatAsString(BIG_DECIMAL_VALUE))
        .isEqualTo(BIG_DECIMAL_VALUE_AS_STRING);
  }

  @Test
  void formatAsList_Displayable_ReturnListOfStrings() {
    assertThat(SummaryUtil.formatAsList(DISPLAYABLE_VALUE))
        .isEqualTo(List.of(DISPLAYABLE_VALUE_AS_STRING));
  }

  @Test
  void formatAsString_Displayable_ReturnString() {
    assertThat(SummaryUtil.formatAsString(DISPLAYABLE_VALUE))
        .isEqualTo(DISPLAYABLE_VALUE_AS_STRING);
  }

}

record DisplayableTestClass() implements Displayable {

  @Override
  public String getDisplayName() {
    return "display name";
  }

  @Override
  public String name() {
    return "actual name";
  }
}
