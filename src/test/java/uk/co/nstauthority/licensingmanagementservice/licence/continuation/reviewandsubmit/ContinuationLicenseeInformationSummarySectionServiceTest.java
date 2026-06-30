package uk.co.nstauthority.licensingmanagementservice.licence.continuation.reviewandsubmit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.energyportal.fox.FoxRedirectService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.externalcontributors.ExternalContributorForm;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.externalcontributorjourney.LicenceContinuationExternalContributorService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.ScheduleState;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.summary.ExternalUrlView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class ContinuationLicenseeInformationSummarySectionServiceTest {

  private static final String VIEW_PEARS_LICENCE_URL =
      "https://test.example.com/fox/nsta/LMS_REDIRECT/view-licence?LICENCE_TYPE=P&LICENCE_NO=123";

  private static final String CURRENT_TERM_PHASE_DISPLAY = "Phase A (Initial term)";
  private static final String NEXT_TERM_PHASE_DISPLAY = "Phase B (Initial term)";

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
      .withLicencePrefix("P")
      .withLicenceNumber("123")
      .withLicenceReference("P 123")
      .build();

  private static final Licence LICENCE_MANAGED_BY_LMS = LicenceTestUtil.builder()
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .withLicencePrefix("CS")
      .withLicenceNumber("456")
      .withLicenceReference("CS 456")
      .build();

  private static final LicenceSchedule LICENCE_SCHEDULE =
      LicenceScheduleTestUtil.createLicenceSchedule(LICENCE);

  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL =
      LicenceScheduleTestUtil.createLicenceScheduleDetail(LICENCE_SCHEDULE);

  private static final LicenceSchedule LICENCE_SCHEDULE_MANAGED_BY_LMS =
      LicenceScheduleTestUtil.createLicenceSchedule(LICENCE_MANAGED_BY_LMS);

  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL_MANAGED_BY_LMS =
      LicenceScheduleTestUtil.createLicenceScheduleDetail(LICENCE_SCHEDULE_MANAGED_BY_LMS);

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private FoxRedirectService foxRedirectService;

  @Mock
  private LicenceContinuationExternalContributorService licenceContinuationExternalContributorService;

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @InjectMocks
  private ContinuationLicenseeInformationSummarySectionService continuationLicenseeInformationSummarySectionService;

  @Test
  void getSummarySection_showsTermPhaseUnderLicenceInformationCard() {
    var licenceContinuationApplicationDetail =
        LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);
    licenceContinuationApplicationDetail.setResponsibleOrganisationUnitId(1);

    var externalContributorForm = new ExternalContributorForm();
    externalContributorForm.setAddExternalContributors(true);

    when(foxRedirectService.getViewPearsLicenceUrl(LICENCE)).thenReturn(VIEW_PEARS_LICENCE_URL);
    when(organisationUnitQueryService.getOrganisationUnitNameById(1)).thenReturn(Optional.of("Test Organisation"));
    when(licenceContinuationExternalContributorService.getExternalContributorForm(licenceContinuationApplicationDetail))
        .thenReturn(externalContributorForm);
    stubScheduleState(licenceContinuationApplicationDetail);

    var result = continuationLicenseeInformationSummarySectionService
        .getSummarySection(licenceContinuationApplicationDetail, null).get();

    assertThat(result.displayOrder()).isEqualTo(10);

    var summaryItem = result.summaryItems().getFirst();
    assertThat(summaryItem.displayName()).isEqualTo("General details");
    assertThat(summaryItem.summaryCards()).hasSize(3);

    var licenceSummaryCard = summaryItem.summaryCards().getFirst();
    assertThat(licenceSummaryCard.displayName()).isEqualTo("Licence information");
    assertThat(licenceSummaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Licence reference", "P 123")
            .addExternalUrlValue("View licence", new ExternalUrlView("View licence in PEARS", VIEW_PEARS_LICENCE_URL))
            .addStringValue("Current term/phase", CURRENT_TERM_PHASE_DISPLAY)
            .addStringValue("Next term/phase", NEXT_TERM_PHASE_DISPLAY)
            .build()
    );

    var licenseeInformationSummaryCard = summaryItem.summaryCards().get(1);
    assertThat(licenseeInformationSummaryCard.displayName()).isEqualTo("Licensee information");
    assertThat(licenseeInformationSummaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Who is the licensee for this application?", "Test Organisation")
            .build()
    );

    var externalContributorSummaryCard = summaryItem.summaryCards().get(2);
    assertThat(externalContributorSummaryCard.displayName()).isEqualTo("External contributors");
    assertThat(externalContributorSummaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("External contributors required", true)
            .build()
    );
  }

  @Test
  void getSummarySection_whenNoNextTermPhase_licenceCardOmitsNextTermPhaseRow() {
    var licenceContinuationApplicationDetail =
        LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);
    licenceContinuationApplicationDetail.setResponsibleOrganisationUnitId(1);

    when(foxRedirectService.getViewPearsLicenceUrl(LICENCE)).thenReturn(VIEW_PEARS_LICENCE_URL);
    when(organisationUnitQueryService.getOrganisationUnitNameById(1)).thenReturn(Optional.of("Test Organisation"));
    when(licenceContinuationExternalContributorService.getExternalContributorForm(licenceContinuationApplicationDetail))
        .thenReturn(new ExternalContributorForm());

    var currentTerm = mock(LicenceScheduleTerm.class);
    var scheduleState = new ScheduleState(currentTerm, null, null, null);
    when(licenceContinuationService.resolveScheduleState(licenceContinuationApplicationDetail))
        .thenReturn(scheduleState);
    when(licenceScheduleService.formatTermPhaseDisplay(currentTerm, null)).thenReturn(CURRENT_TERM_PHASE_DISPLAY);
    when(licenceScheduleService.formatTermPhaseDisplay(null, null)).thenReturn(null);

    var result = continuationLicenseeInformationSummarySectionService
        .getSummarySection(licenceContinuationApplicationDetail, null).get();

    var licenceSummaryCard = result.summaryItems().getFirst().summaryCards().getFirst();
    assertThat(licenceSummaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Licence reference", "P 123")
            .addExternalUrlValue("View licence", new ExternalUrlView("View licence in PEARS", VIEW_PEARS_LICENCE_URL))
            .addStringValue("Current term/phase", CURRENT_TERM_PHASE_DISPLAY)
            .build()
    );
  }

  @Test
  void getSummarySection_whenLicenceManagedByLms_licenceCardDoesNotContainViewLicenceLink() {
    var licenceContinuationApplicationDetail =
        LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL_MANAGED_BY_LMS);
    licenceContinuationApplicationDetail.setResponsibleOrganisationUnitId(1);

    when(organisationUnitQueryService.getOrganisationUnitNameById(1)).thenReturn(Optional.of("Test Organisation"));
    when(licenceContinuationExternalContributorService.getExternalContributorForm(licenceContinuationApplicationDetail))
        .thenReturn(new ExternalContributorForm());
    stubScheduleState(licenceContinuationApplicationDetail);

    var result = continuationLicenseeInformationSummarySectionService
        .getSummarySection(licenceContinuationApplicationDetail, null).get();

    var licenceSummaryCard = result.summaryItems().getFirst().summaryCards().getFirst();
    assertThat(licenceSummaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Licence reference", "CS 456")
            .addStringValue("Current term/phase", CURRENT_TERM_PHASE_DISPLAY)
            .addStringValue("Next term/phase", NEXT_TERM_PHASE_DISPLAY)
            .build()
    );
  }

  @Test
  void getSummarySection_whenOrganisationUnitNameNotFound_licenseeCardUsesEmptyString() {
    var licenceContinuationApplicationDetail =
        LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL_MANAGED_BY_LMS);
    licenceContinuationApplicationDetail.setResponsibleOrganisationUnitId(1);

    when(organisationUnitQueryService.getOrganisationUnitNameById(1)).thenReturn(Optional.empty());
    when(licenceContinuationExternalContributorService.getExternalContributorForm(licenceContinuationApplicationDetail))
        .thenReturn(new ExternalContributorForm());
    stubScheduleState(licenceContinuationApplicationDetail);

    var result = continuationLicenseeInformationSummarySectionService
        .getSummarySection(licenceContinuationApplicationDetail, null).get();

    var licenseeInformationSummaryCard = result.summaryItems().getFirst().summaryCards().get(1);
    assertThat(licenseeInformationSummaryCard.summaryData()).isEqualTo(
        SummaryDataView.newBuilder()
            .addStringValue("Who is the licensee for this application?", "")
            .build()
    );
  }

  private void stubScheduleState(LicenceContinuationApplicationDetail detail) {
    var currentTerm = mock(LicenceScheduleTerm.class);
    var currentPhase = mock(LicenceSchedulePhase.class);
    var nextTerm = mock(LicenceScheduleTerm.class);
    var nextPhase = mock(LicenceSchedulePhase.class);
    var scheduleState = new ScheduleState(currentTerm, currentPhase, nextTerm, nextPhase);

    when(licenceContinuationService.resolveScheduleState(detail)).thenReturn(scheduleState);
    when(licenceScheduleService.formatTermPhaseDisplay(currentTerm, currentPhase)).thenReturn(CURRENT_TERM_PHASE_DISPLAY);
    when(licenceScheduleService.formatTermPhaseDisplay(nextTerm, nextPhase)).thenReturn(NEXT_TERM_PHASE_DISPLAY);
  }
}
