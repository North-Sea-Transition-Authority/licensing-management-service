package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit;

import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.reviewandsubmit.ExtensionRequestDetailsSummarySectionService.EXTENSION_DURATION;

import java.util.List;
import java.util.Map;
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
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryKeyValue;

@ExtendWith(MockitoExtension.class)
class ExtensionRequestDetailsSummarySectionServiceTest {

  public static final String EXTENSION_DURATION_DATE = "2 years 2 months 2 days";

  @Mock
  private LicenceScheduleExtensionService licenceScheduleExtensionService;

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

  private SummaryDataView getLicenceSummaryData() {
    return(SummaryDataView)extensionRequestDetailsSummarySectionService.getLicenceSummaryItem(
        scheduleWorkProgrammeApplicationDetail,
        ExtensionRequestDetailsSummarySectionService.LICENCE_SECTION_NAME
    ).summaryCards().getFirst().summaryData();
  }
}