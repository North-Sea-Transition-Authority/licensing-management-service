package uk.co.nstauthority.licensingmanagementservice.fds.error;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class ErrorSummaryItemTest {

  @Test
  void fromMessages() {
    var result = ErrorSummaryItem.fromMessages(List.of("First error", "Second error"), "field-name");

    assertThat(result)
        .usingRecursiveComparison()
        .isEqualTo(List.of(
            new ErrorSummaryItem(0, "field-name", "First error"),
            new ErrorSummaryItem(1, "field-name", "Second error")
        ));
  }
}
