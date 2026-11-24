package uk.co.nstauthority.licensingmanagementservice.licence.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;

@ExtendWith(MockitoExtension.class)
class ApplicationUpdateSummaryCardViewTest {

  @Mock
  private ApplicationDetail applicationDetail;

  @Test
  void from_whenDeadlineSet_thenDeadlineFormatted() {
    var deadline = LocalDate.of(2026, 12, 25);
    when(applicationDetail.getUpdateReason()).thenReturn("Missing documents");
    when(applicationDetail.getUpdateDeadlineDate()).thenReturn(deadline);

    var result = ApplicationUpdateSummaryCardView.from(applicationDetail);

    var expected = new ApplicationUpdateSummaryCardView(
        "Missing documents",
        DateFormatUtil.convertToDisplayText(deadline)
    );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void from_whenNoDeadline_thenDeadlineIsNull() {
    when(applicationDetail.getUpdateReason()).thenReturn("Missing documents");
    when(applicationDetail.getUpdateDeadlineDate()).thenReturn(null);

    var result = ApplicationUpdateSummaryCardView.from(applicationDetail);

    var expected = new ApplicationUpdateSummaryCardView("Missing documents", null);
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
