package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@ExtendWith(MockitoExtension.class)
class LicenceScheduleSummarySectionServiceTest {

  @Mock
  private SummarySectionService<ScheduleWorkProgrammeApplicationDetail> summarySectionService1;

  @Mock
  private SummarySectionService<ScheduleWorkProgrammeApplicationDetail> summarySectionService2;

  private LicenceScheduleSummarySectionService licenceScheduleSummarySectionService;
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
  }

  @Test
  void getSummarySections_withSingleSection_returnsSingleSection() {

    var summaryItem = SummaryItem.withCards("Section 1", Collections.emptyList());
    var summarySection = new SummarySection(1, List.of(summaryItem));
    var services = List.of(summarySectionService1);

    licenceScheduleSummarySectionService = new LicenceScheduleSummarySectionService(services);

    when(summarySectionService1.getSummarySection(scheduleWorkProgrammeApplicationDetail, null))
        .thenReturn(Optional.of(summarySection));

    var result = licenceScheduleSummarySectionService.getSummarySections(scheduleWorkProgrammeApplicationDetail, null);

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
    licenceScheduleSummarySectionService = new LicenceScheduleSummarySectionService(services);

    when(summarySectionService1.getSummarySection(scheduleWorkProgrammeApplicationDetail, null))
        .thenReturn(Optional.of(summarySection1));
    when(summarySectionService2.getSummarySection(scheduleWorkProgrammeApplicationDetail, null))
        .thenReturn(Optional.of(summarySection2));

    var result = licenceScheduleSummarySectionService.getSummarySections(scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).hasSize(2);
    assertThat(result.get(0).displayOrder()).isEqualTo(1);
    assertThat(result.get(1).displayOrder()).isEqualTo(2);
  }

  @Test
  void getSummarySections_withEmptySections_returnsEmptyList() {
    var services = List.of(summarySectionService1, summarySectionService2);
    licenceScheduleSummarySectionService = new LicenceScheduleSummarySectionService(services);

    when(summarySectionService1.getSummarySection(scheduleWorkProgrammeApplicationDetail, null))
        .thenReturn(Optional.empty());
    when(summarySectionService2.getSummarySection(scheduleWorkProgrammeApplicationDetail, null))
        .thenReturn(Optional.empty());

    var result = licenceScheduleSummarySectionService.getSummarySections(scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).isEmpty();
  }
}