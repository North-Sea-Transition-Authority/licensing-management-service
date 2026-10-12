package uk.co.fivium.gisframework.feature;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class LineUtilsTest {

  @Test
  void getEsriJsonsInDisplayOrder() {
    var firstLine = LineTestUtil.newBuilder().withDisplayOrder(1).withEsriJson("first line").build();
    var secondLine = LineTestUtil.newBuilder().withDisplayOrder(2).withEsriJson("second line").build();
    var thirdLine = LineTestUtil.newBuilder().withDisplayOrder(3).withEsriJson("third line").build();

    var result = LineUtils.getEsriJsonsInDisplayOrder(List.of(thirdLine, firstLine, secondLine));

    assertThat(result).isEqualTo(List.of("first line", "second line", "third line"));
  }
}
