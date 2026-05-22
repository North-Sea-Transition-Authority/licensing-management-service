package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.WorkProgrammeAmendmentSummarySectionService.COMPLETION_DATE_CHANGE_REQUESTED;
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.WorkProgrammeAmendmentSummarySectionService.WORK_PROGRAMME_CONTENT_CHANGE_REQUESTED;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardType;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryKeyValue;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeAmendmentSummarySectionServiceTest {

  @Mock
  private LicenceWorkProgrammeAmendmentSummaryService licenceWorkProgrammeAmendmentSummaryService;

  @Mock
  private SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

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
            true,
            false
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
            false,
            false
        )
    );
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViews(scheduleWorkProgrammeApplicationDetail))
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
        .getWorkProgrammeAmendmentSummaryViews(scheduleWorkProgrammeApplicationDetail))
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
        false,
        false
    );
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViews(scheduleWorkProgrammeApplicationDetail))
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
        true,
        false
    );
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(workProgrammeAmendmentView));

    var result = workProgrammeAmendmentSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        WorkProgrammeAmendmentSummarySectionService.LICENCE_SECTION_NAME
    );

    assertThat(result.summaryCards()).hasSize(1);
    assertThat(result.summaryCards().getFirst().displayName()).isEqualTo("Amendment 1");
  }

  @Test
  void getSummaryItem_whenCompletionDateChangeRequestedDisplayIsPopulated_includesCompletionDateChangeRequested() {

    var workProgrammeAmendmentView = new LicenceWorkProgrammeAmendmentSummaryView(
        "Amendment 1",
        "No",
        "",
        "Yes",
        "",
        LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
        "",
        "",
        false,
        false,
        false
    );
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(workProgrammeAmendmentView));

    var summaryData = getLicenceSummaryDataMap();

    assertThat(summaryData).containsEntry(COMPLETION_DATE_CHANGE_REQUESTED, "Yes");
  }

  @Test
  void getSummaryItem_whenCompletionDateChangeRequestedDisplayIsEmpty_omitsCompletionDateChangeRequested() {

    var workProgrammeAmendmentView = new LicenceWorkProgrammeAmendmentSummaryView(
        "Amendment 1",
        "No",
        "",
        "",
        "",
        LicenceWorkProgrammeAmendmentSummaryMode.VIEW,
        "",
        "",
        false,
        false,
        false
    );
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(workProgrammeAmendmentView));

    var summaryData = getLicenceSummaryDataMap();

    assertThat(summaryData)
        .doesNotContainKey(COMPLETION_DATE_CHANGE_REQUESTED)
        .containsEntry(WORK_PROGRAMME_CONTENT_CHANGE_REQUESTED, "No");
  }

  private Map<String, String> getLicenceSummaryDataMap() {
    var summaryData = (SummaryDataView) workProgrammeAmendmentSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        WorkProgrammeAmendmentSummarySectionService.LICENCE_SECTION_NAME
    ).summaryCards().getFirst().summaryData();

    return summaryData
        .keyValues()
        .stream()
        .collect(toMap(
            SummaryKeyValue::key,
            keyValue -> (String) keyValue.summaryValueData().iterator().next()
        ));
  }

  @Test
  void getSummarySection_whenRequestPurposeIsEmpty_returnsEmpty() {
    when(swpApplicationRequestPurposeService.getRequestPurpose(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.empty());

    var result = workProgrammeAmendmentSummarySectionService.getSummarySection(
        scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenAmendWorkProgrammeIsFalse_returnsEmpty() {
    var requestPurpose = new SwpApplicationRequestPurpose();
    requestPurpose.setAmendWorkProgramme(false);

    when(swpApplicationRequestPurposeService.getRequestPurpose(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(requestPurpose));

    var result = workProgrammeAmendmentSummarySectionService.getSummarySection(
        scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenAmendWorkProgrammeIsTrue_returnsSummarySection() {
    var requestPurpose = new SwpApplicationRequestPurpose();
    requestPurpose.setAmendWorkProgramme(true);

    when(swpApplicationRequestPurposeService.getRequestPurpose(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(requestPurpose));
    when(licenceWorkProgrammeAmendmentSummaryService
        .getWorkProgrammeAmendmentSummaryViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Collections.emptyList());

    var result = workProgrammeAmendmentSummarySectionService.getSummarySection(
        scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).isPresent();
  }
}
