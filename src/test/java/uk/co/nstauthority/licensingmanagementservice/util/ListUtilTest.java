package uk.co.nstauthority.licensingmanagementservice.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ListUtilTest {

  @Test
  void toIntegers() {
    var inputList = List.of("1", "2", "3", "4");
    var expectedList = List.of(1, 2, 3, 4);

    assertThat(ListUtil.toIntegers(inputList)).isEqualTo(expectedList);
  }

  @Test
  void toStrings() {
    var inputList = List.of(1, 2, 3, 4);
    var expectedList = List.of("1", "2", "3", "4");

    assertThat(ListUtil.toStrings(inputList)).isEqualTo(expectedList);
  }
}