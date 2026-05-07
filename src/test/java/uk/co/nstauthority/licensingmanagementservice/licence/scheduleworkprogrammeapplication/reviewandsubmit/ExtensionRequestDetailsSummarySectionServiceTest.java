package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.ExtensionRequestDetailsSummarySectionService.EXTENSION_DURATION;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionRequestView;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurpose;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose.SwpApplicationRequestPurposeService;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryKeyValue;

@ExtendWith(MockitoExtension.class)
class ExtensionRequestDetailsSummarySectionServiceTest {

  public static final String EXTENSION_DURATION_DATE = "2 years 2 months 2 days";

  @Mock
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

  @Mock
  private SwpApplicationRequestPurposeService swpApplicationRequestPurposeService;

  @InjectMocks
  private ExtensionRequestDetailsSummarySectionService extensionRequestDetailsSummarySectionService;

  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail;

  @BeforeEach
  void setUp() {
    scheduleWorkProgrammeApplicationDetail = new ScheduleWorkProgrammeApplicationDetail();
  }

  @Test
  void getLicenceSummaryItem_withPhaseExtension() {

    var extensionView = createExtensionView(PhaseType.PHASE_A.getDisplayName());

    when(licenceScheduleExtensionService.getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(extensionView));

    var summaryDataView = getLicenceSummaryData();


    assertThat(summaryDataView.keyValues()).hasSize(1);
    SummaryKeyValue singleKeyValue = summaryDataView.keyValues().getFirst();

    assertThat(singleKeyValue.key()).isEqualTo(EXTENSION_DURATION.formatted(extensionView.displayName()));
    assertThat((String) singleKeyValue.summaryValueData().iterator().next()).isEqualTo(EXTENSION_DURATION_DATE);
  }

  @Test
  void getLicenceSummaryItem_withTermExtension() {
    var extensionView = createExtensionView(TermType.INITIAL.getDisplayName());

    when(licenceScheduleExtensionService.getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(extensionView));

    SummaryDataView summaryDataView = getLicenceSummaryData();

    assertThat(summaryDataView.keyValues()).hasSize(1);
    SummaryKeyValue singleKeyValue = summaryDataView.keyValues().getFirst();

    assertThat(singleKeyValue.key()).isEqualTo(EXTENSION_DURATION.formatted(extensionView.displayName()));
    assertThat((String) singleKeyValue.summaryValueData().iterator().next()).isEqualTo(EXTENSION_DURATION_DATE);
  }

  @Test
  void getLicenceSummaryItem_withMultipleExtensionRequests() {
    var phaseExtensionView = createExtensionView(PhaseType.PHASE_A.getDisplayName());
    var termExtensionView = createExtensionView(TermType.INITIAL.getDisplayName());

    when(licenceScheduleExtensionService.getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of(phaseExtensionView, termExtensionView));

    SummaryDataView summaryDataView = getLicenceSummaryData();

    Map<String, String> summaryData = toSummaryDataMap(summaryDataView);

    Map<String, String> expectedMap = Map.of(
        EXTENSION_DURATION.formatted(phaseExtensionView.displayName()), EXTENSION_DURATION_DATE,
        EXTENSION_DURATION.formatted(termExtensionView.displayName()), EXTENSION_DURATION_DATE
    );

    assertThat(summaryData).containsAllEntriesOf(expectedMap);
  }


  private Map<String, String> toSummaryDataMap(SummaryDataView summaryDataView) {
    return summaryDataView
        .keyValues()
        .stream()
        .collect(toMap(
            SummaryKeyValue::key,
            keyValue -> (String) keyValue.summaryValueData().iterator().next()
        ));
  }

  private LicenceScheduleExtensionRequestView createExtensionView(String displayNameFormat) {
    LicenceScheduleExtensionRequestView licenceScheduleExtensionRequestView = mock(
        LicenceScheduleExtensionRequestView.class);
    when(licenceScheduleExtensionRequestView.displayName()).thenReturn(displayNameFormat);
    when(licenceScheduleExtensionRequestView.duration()).thenReturn(new ThreeFieldDuration(2, 2, 2));
    when(licenceScheduleExtensionRequestView.isRequested()).thenReturn(true);
    return licenceScheduleExtensionRequestView;
  }

  @Test
  void getSummarySection_whenRequestPurposeIsEmpty_returnsEmpty() {
    when(swpApplicationRequestPurposeService.getRequestPurpose(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.empty());

    var result = extensionRequestDetailsSummarySectionService.getSummarySection(
        scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenNeitherExtensionFlagIsSet_returnsEmpty() {
    var requestPurpose = new SwpApplicationRequestPurpose();
    requestPurpose.setExtendPhaseOrTerm(false);
    requestPurpose.setExtendTerm(false);

    when(swpApplicationRequestPurposeService.getRequestPurpose(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(requestPurpose));

    var result = extensionRequestDetailsSummarySectionService.getSummarySection(
        scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).isEmpty();
  }

  @Test
  void getSummarySection_whenExtendPhaseOrTermIsTrue_returnsSummarySection() {
    var requestPurpose = new SwpApplicationRequestPurpose();
    requestPurpose.setExtendPhaseOrTerm(true);

    when(swpApplicationRequestPurposeService.getRequestPurpose(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(requestPurpose));
    when(licenceScheduleExtensionService.getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of());

    var result = extensionRequestDetailsSummarySectionService.getSummarySection(
        scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).isPresent();
  }

  @Test
  void getSummarySection_whenExtendTermIsTrue_returnsSummarySection() {
    var requestPurpose = new SwpApplicationRequestPurpose();
    requestPurpose.setExtendTerm(true);

    when(swpApplicationRequestPurposeService.getRequestPurpose(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(Optional.of(requestPurpose));
    when(licenceScheduleExtensionService.getLicenceScheduleExtensionViews(scheduleWorkProgrammeApplicationDetail))
        .thenReturn(List.of());

    var result = extensionRequestDetailsSummarySectionService.getSummarySection(
        scheduleWorkProgrammeApplicationDetail, null);

    assertThat(result).isPresent();
  }

  private SummaryDataView getLicenceSummaryData() {
    return(SummaryDataView)extensionRequestDetailsSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        ExtensionRequestDetailsSummarySectionService.LICENCE_SECTION_NAME
    ).summaryCards().getFirst().summaryData();
  }
}