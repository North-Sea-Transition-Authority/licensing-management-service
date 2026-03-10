package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@ExtendWith(MockitoExtension.class)
class ContinuationSummarySectionServiceTest {

  @Mock
  private SummarySectionService<LicenceContinuationApplicationDetail> summarySectionService1;

  @Mock
  private SummarySectionService<LicenceContinuationApplicationDetail> summarySectionService2;

  private ContinuationSummarySectionService continuationSummarySectionService;
  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;

  @BeforeEach
  void setUp() {
    licenceContinuationApplicationDetail = new LicenceContinuationApplicationDetail();
  }

  @Test
  void getSummarySections_withSingleSection_returnsSingleSection() {
    var summaryItem = SummaryItem.withCards("Section 1", Collections.emptyList());
    var summarySection = new SummarySection(1, List.of(summaryItem));
    var services = List.of(summarySectionService1);

    continuationSummarySectionService = new ContinuationSummarySectionService(services);

    when(summarySectionService1.getSummarySection(licenceContinuationApplicationDetail, null)).thenReturn(Optional.of(summarySection));

    var result = continuationSummarySectionService.getSummarySections(licenceContinuationApplicationDetail, null);

    assertThat(result).hasSize(1);
    assertThat(result.getFirst().displayOrder()).isEqualTo(1);
  }

  @Test
  void getSummarySections_withMultipleSections_returnsSortedSections() {
    var summaryItem1 = SummaryItem.withCards("Section 1", Collections.emptyList());
    var summarySection1 = new SummarySection(1, List.of(summaryItem1));

    var summaryItem2 = SummaryItem.withCards("Section 2", Collections.emptyList());
    var summarySection2 = new SummarySection(2, List.of(summaryItem2));

    var services = List.of(summarySectionService1, summarySectionService2);
    continuationSummarySectionService = new ContinuationSummarySectionService(services);

    when(summarySectionService1.getSummarySection(licenceContinuationApplicationDetail, null)).thenReturn(Optional.of(summarySection1));
    when(summarySectionService2.getSummarySection(licenceContinuationApplicationDetail, null)).thenReturn(Optional.of(summarySection2));

    var result = continuationSummarySectionService.getSummarySections(licenceContinuationApplicationDetail, null);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).displayOrder()).isEqualTo(1);
    assertThat(result.get(1).displayOrder()).isEqualTo(2);
  }

  @Test
  void getSummarySections_withEmptySections_returnsEmptyList() {
    var services = List.of(summarySectionService1, summarySectionService2);
    continuationSummarySectionService = new ContinuationSummarySectionService(services);

    when(summarySectionService1.getSummarySection(licenceContinuationApplicationDetail, null)).thenReturn(Optional.empty());
    when(summarySectionService2.getSummarySection(licenceContinuationApplicationDetail, null)).thenReturn(Optional.empty());

    var result = continuationSummarySectionService.getSummarySections(licenceContinuationApplicationDetail, null);

    assertThat(result).isEmpty();
  }
}