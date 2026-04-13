package uk.co.nstauthority.licensingmanagementservice.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.energyportalapi.generated.types.Address;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceSchedulePhaseTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceScheduleTermTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@ExtendWith(MockitoExtension.class)
class DocumentLinkingServiceTest {

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @Mock
  private LicenceTypeRulesResolver licenceTypeRulesResolver;

  @InjectMocks
  private DocumentLinkingService documentLinkingService;

  private static final Licence LICENCE = LicenceTestUtil.builder().build();
  private static final LicenceScheduleDetail LICENCE_SCHEDULE_DETAIL
      = LicenceScheduleTestUtil.createLicenceScheduleDetail(LicenceScheduleTestUtil.createLicenceSchedule(LICENCE));

  private static final UUID APPLICATION_ID = UUID.randomUUID();

  private static final LicenceContinuationApplicationDetail LICENCE_CONTINUATION_APPLICATION_DETAIL
      = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(LICENCE_SCHEDULE_DETAIL);

  private static final ScheduleWorkProgrammeApplicationDetail SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL
      = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplicationDetail(LICENCE_SCHEDULE_DETAIL) ;

  @Test
  void getContinuationApplicationCompanyNameFromDocumentInstanceDto() {
    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNameById(any()))
        .thenReturn(Optional.of("test name"));

    var result = documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceDto);
    assertThat(result).isEqualTo("test name");
    verify(licenceContinuationService).getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID);
  }

  @Test
  void getContinuationApplicationCompanyAddressFromDocumentInstanceDto() {
    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    when(organisationUnitQueryService.getOrganisationUnitAddressById(any()))
        .thenReturn(Optional.of(new Address("test address")));

    var result = documentLinkingService.getApplicationCompanyAddressFromDto(documentInstanceDto);
    assertThat(result).isEqualTo(Optional.of(new Address("test address")));
    verify(licenceContinuationService).getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID);
  }

  @Test
  void getScheduleAmendmentApplicationCompanyNameFromDocumentInstanceDto() {
    when(scheduleWorkProgrammeApplicationService.getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    when(organisationUnitQueryService.getOrganisationUnitNameById(any()))
        .thenReturn(Optional.of("test name"));

    var result = documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceDto);
    assertThat(result).isEqualTo("test name");
    verify(scheduleWorkProgrammeApplicationService).getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID);
  }

  @Test
  void getScheduleAmendmentApplicationCompanyAddressFromDocumentInstanceDto() {
    when(scheduleWorkProgrammeApplicationService.getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    when(organisationUnitQueryService.getOrganisationUnitAddressById(any()))
        .thenReturn(Optional.of(new Address("test address")));

    var result = documentLinkingService.getApplicationCompanyAddressFromDto(documentInstanceDto);
    assertThat(result).isEqualTo(Optional.of(new Address("test address")));
    verify(scheduleWorkProgrammeApplicationService).getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID);
  }

  @Test
  void getContinuationApplicationLicenceReferenceFromDocumentInstanceDto() {
    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(LICENCE_CONTINUATION_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getApplicationLicenceReferenceFromDto(documentInstanceDto);
    assertThat(result).isEqualTo(LICENCE.getLicenceReference());
    verify(licenceContinuationService).getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID);
  }

  @Test
  void getScheduleAmendmentApplicationLicenceReferenceFromDocumentInstanceDto() {
    when(scheduleWorkProgrammeApplicationService.getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(SCHEDULE_WORK_PROGRAMME_APPLICATION_DETAIL);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getApplicationLicenceReferenceFromDto(documentInstanceDto);
    assertThat(result).isEqualTo(LICENCE.getLicenceReference());
    verify(scheduleWorkProgrammeApplicationService).getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID);
  }

  @Test
  void getAmendmentApplicationCompanyFromDocumentInstanceDtoWrongItemType() {
    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType("wrong item type")
        .build();

    assertThatThrownBy(
        () -> documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceDto))
        .isInstanceOf(IllegalArgumentException.class);

    verify(licenceContinuationService, never()).getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(any());
    verify(scheduleWorkProgrammeApplicationService, never()).getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(any());
  }

  @Test
  void getCurrentTermPhaseNameFromDto_continuationApplication_licenceHasNoPhases_returnsTermDisplayName() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var applicationDetail = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(scheduleDetail);

    var currentTerm = LicenceScheduleTermTestUtil.builder().withTermType(TermType.INITIAL).build();

    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(applicationDetail);
    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(scheduleDetail);
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(false);
    when(licenceScheduleService.getCurrentTerm(scheduleDetail)).thenReturn(currentTerm);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getCurrentTermPhaseNameFromDto(documentInstanceDto);

    assertThat(result).isEqualTo(TermType.INITIAL.getDisplayName());
  }

  @Test
  void getCurrentTermPhaseNameFromDto_continuationApplication_licenceHasPhases_returnsPhaseDisplayName() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var applicationDetail = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(scheduleDetail);

    var currentTerm = LicenceScheduleTermTestUtil.builder().withTermType(TermType.INITIAL).build();
    var currentPhase = LicenceSchedulePhaseTestUtil.builder().withPhaseType(PhaseType.PHASE_A).build();

    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(applicationDetail);
    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(scheduleDetail);
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceScheduleService.getCurrentTerm(scheduleDetail)).thenReturn(currentTerm);
    when(licenceScheduleService.getCurrentPhase(currentTerm)).thenReturn(currentPhase);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getCurrentTermPhaseNameFromDto(documentInstanceDto);

    assertThat(result).isEqualTo(PhaseType.PHASE_A.getDisplayName());
  }

  @Test
  void getCurrentTermPhaseNameFromDto_scheduleAmendmentApplication_licenceHasNoPhases_returnsTermDisplayName() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplicationDetail(scheduleDetail);

    var currentTerm = LicenceScheduleTermTestUtil.builder().withTermType(TermType.INITIAL).build();

    when(scheduleWorkProgrammeApplicationService.getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(applicationDetail);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(scheduleDetail);
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(false);
    when(licenceScheduleService.getCurrentTerm(scheduleDetail)).thenReturn(currentTerm);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getCurrentTermPhaseNameFromDto(documentInstanceDto);

    assertThat(result).isEqualTo(TermType.INITIAL.getDisplayName());
  }

  @Test
  void getCurrentTermPhaseNameFromDto_scheduleAmendmentApplication_licenceHasPhases_returnsPhaseDisplayName() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplicationDetail(scheduleDetail);

    var currentTerm = LicenceScheduleTermTestUtil.builder().withTermType(TermType.INITIAL).build();
    var currentPhase = LicenceSchedulePhaseTestUtil.builder().withPhaseType(PhaseType.PHASE_A).build();

    when(scheduleWorkProgrammeApplicationService.getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(applicationDetail);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(scheduleDetail);
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceScheduleService.getCurrentTerm(scheduleDetail)).thenReturn(currentTerm);
    when(licenceScheduleService.getCurrentPhase(currentTerm)).thenReturn(currentPhase);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getCurrentTermPhaseNameFromDto(documentInstanceDto);

    assertThat(result).isEqualTo(PhaseType.PHASE_A.getDisplayName());
  }

  @Test
  void getCurrentTermPhaseNameFromDto_continuationApplication_licenceHasPhases_noCurrentPhase_returnsTermDisplayName() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var applicationDetail = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(scheduleDetail);

    var currentTerm = LicenceScheduleTermTestUtil.builder().withTermType(TermType.INITIAL).build();

    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(applicationDetail);
    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(scheduleDetail);
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceScheduleService.getCurrentTerm(scheduleDetail)).thenReturn(currentTerm);
    when(licenceScheduleService.getCurrentPhase(currentTerm)).thenReturn(null);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getCurrentTermPhaseNameFromDto(documentInstanceDto);

    assertThat(result).isEqualTo(TermType.INITIAL.getDisplayName());
  }

  @Test
  void getCurrentTermPhaseNameFromDto_scheduleAmendmentApplication_licenceHasPhases_noCurrentPhase_returnsTermDisplayName() {
    var licence = LicenceTestUtil.builder().withLicenceType(LicenceType.SEAWARD_PRODUCTION).build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var applicationDetail = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplicationDetail(scheduleDetail);

    var currentTerm = LicenceScheduleTermTestUtil.builder().withTermType(TermType.INITIAL).build();

    when(scheduleWorkProgrammeApplicationService.getLatestScheduleWorkProgrammeDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(applicationDetail);
    when(scheduleWorkProgrammeApplicationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(scheduleDetail);
    when(licenceTypeRulesResolver.hasPhases(LicenceType.SEAWARD_PRODUCTION)).thenReturn(true);
    when(licenceScheduleService.getCurrentTerm(scheduleDetail)).thenReturn(currentTerm);
    when(licenceScheduleService.getCurrentPhase(currentTerm)).thenReturn(null);

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getCurrentTermPhaseNameFromDto(documentInstanceDto);

    assertThat(result).isEqualTo(TermType.INITIAL.getDisplayName());
  }

  @Test
  void getNextTermPhaseStartDateFromDto_continuationApplication_returnsFormattedDate() {
    var licence = LicenceTestUtil.builder().build();
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var scheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);
    var applicationDetail = LicenceContinuationApplicationTestUtil.createLicenceContinuationApplicationDetail(scheduleDetail);
    var nextStartDate = java.time.LocalDate.of(2026, 6, 1);

    when(licenceContinuationService.getLatestLicenceContinuationApplicationDetailByApplicationIdOrThrow(APPLICATION_ID))
        .thenReturn(applicationDetail);
    when(licenceContinuationService.getScheduleDetailFromApplicationDetail(applicationDetail))
        .thenReturn(scheduleDetail);
    when(licenceScheduleService.getNextTermPhaseStartDate(scheduleDetail)).thenReturn(Optional.of(nextStartDate));

    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.CONTINUATION_APPLICATION.name())
        .withItemReference(APPLICATION_ID.toString())
        .build();

    var result = documentLinkingService.getNextTermPhaseStartDateFromDto(documentInstanceDto);

    assertThat(result).isEqualTo("1 Jun 2026");
  }

  @Test
  void getNextTermPhaseStartDateFromDto_scheduleAmendmentApplication_returnsEmpty() {
    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder()
        .withItemType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name())
        .withItemReference(UUID.randomUUID().toString())
        .build();

    var result = documentLinkingService.getNextTermPhaseStartDateFromDto(documentInstanceDto);

    assertThat(result).isEmpty();
  }
}