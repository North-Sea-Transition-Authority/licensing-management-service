package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryMode;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.amendjourney.LicenceWorkProgrammeAmendmentSummaryView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeAmendmentSummarySectionServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;

  @InjectMocks
  private WorkProgrammeAmendmentSummarySectionService workProgrammeAmendmentSummarySectionService;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
  }

  @Test
  void getSummaryItem_withMultipleAmendments_returnsCorrectSummaryItem() {

    var workProgrammeAmendmentViews = List.of(
        new LicenceWorkProgrammeAmendmentSummaryView(
            "Amendment 1",
            "Yes",
            "Content changes",
            "Yes",
            "3 years 2 months 1 days",
            LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
            "",
            "",
            true,
            true
        ),
        new LicenceWorkProgrammeAmendmentSummaryView(
            "Amendment 2",
            "No",
            "",
            "Yes",
            "1 years 0 months 0 days",
            LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
            "",
            "",
            true,
            false
        )
    );
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(workProgrammeAmendmentViews);

    var result = workProgrammeAmendmentSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        WorkProgrammeAmendmentSummarySectionService.LICENCE_SECTION_NAME
    );

    assertThat(result.summaryCards()).hasSize(2);
    assertThat(result.displayName()).isEqualTo(WorkProgrammeAmendmentSummarySectionService.LICENCE_SECTION_NAME);
  }

  @Test
  void getSummaryItem_withEmptyAmendments_returnsEmptySummaryItem() {

    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Collections.emptyList());

    var result = workProgrammeAmendmentSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        WorkProgrammeAmendmentSummarySectionService.LICENCE_SECTION_NAME
    );

    assertThat(result.summaryCards().getFirst().summaryCardType()).isEqualTo(SummaryCardType.EMPTY_SUMMARY);
  }

  @Test
  void getSummaryItem_withCompletionDateChangeOnly_returnsCorrectSummaryCard() {

    var workProgrammeAmendmentView = new LicenceWorkProgrammeAmendmentSummaryView(
        "Amendment 1",
        "No",
        "",
        "Yes",
        "2 years 3 months 5 days",
        LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
        "",
        "",
        true,
        false
    );
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(workProgrammeAmendmentView));

    var result = workProgrammeAmendmentSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        WorkProgrammeAmendmentSummarySectionService.LICENCE_SECTION_NAME
    );

    assertThat(result.summaryCards()).hasSize(1);
    assertThat(result.summaryCards().getFirst().displayName()).isEqualTo("Amendment 1");
  }

  @Test
  void getSummaryItem_withContentChangeOnly_returnsCorrectSummaryCard() {

    var workProgrammeAmendmentView = new LicenceWorkProgrammeAmendmentSummaryView(
        "Amendment 1",
        "Yes",
        "Updated work programme content",
        "No",
        "",
        LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
        "",
        "",
        false,
        true
    );
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViewsFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(workProgrammeAmendmentView));

    var result = workProgrammeAmendmentSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        WorkProgrammeAmendmentSummarySectionService.LICENCE_SECTION_NAME
    );

    assertThat(result.summaryCards()).hasSize(1);
    assertThat(result.summaryCards().getFirst().displayName()).isEqualTo("Amendment 1");
  }
}