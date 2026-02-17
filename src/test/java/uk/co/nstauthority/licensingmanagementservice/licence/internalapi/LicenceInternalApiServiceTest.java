package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;

@ExtendWith(MockitoExtension.class)
class LicenceInternalApiServiceTest {

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @InjectMocks
  private LicenceInternalApiService licenceInternalApiService;

  private ServiceUserDetail serviceUserDetail;

  @BeforeEach
  void setUp() {
    serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build();
    int authorizedUnitId = 99;
    when(applicationAccessService.getOrganisationUnitIds(serviceUserDetail)).thenReturn(Set.of(authorizedUnitId));

    var licenceResponsibleOrganisation = new LicenceResponsibleOrganisation();
    licenceResponsibleOrganisation.setResponsibleOrganisationId(authorizedUnitId);
    when(licenceResponsibleOrganisationService.getAllByLicence(any())).thenReturn(List.of(licenceResponsibleOrganisation));
  }

  @Test
  void searchLicencesWithInProgressSchedulesByReferenceTypeAndStatusForEaaApplication() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(Set.of(
        ScheduleWorkProgrammeApplicationStatus.DRAFT,
        ScheduleWorkProgrammeApplicationStatus.SUBMITTED
    )))
        .thenReturn(List.of());

    var searchTerm = "term";
    var licenceType = LicenceType.CARBON_STORAGE;

    var licenceReference = "CS001";
    int id = 3;
    var licence = LicenceTestUtil.builder()
        .withId(id)
        .withLicenceReference(licenceReference)
        .withLicenceType(licenceType)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    when(licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE)
    ).thenReturn(List.of(licenceScheduleDetail));

    var licenceJson = new LicenceJson(id, licenceReference);

    assertThat(licenceInternalApiService.searchLicencesWithInProgressSchedulesByReferenceTypeAndStatusForEaaApplication(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE,
        serviceUserDetail
    ))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson));
  }

  @Test
  void searchLicencesWithInProgressSchedulesByReferenceTypeAndStatusForEaaApplication_testActiveApplicationFilter() {
    var scheduleAppDetail = new ScheduleWorkProgrammeApplicationDetail();

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(Set.of(
        ScheduleWorkProgrammeApplicationStatus.DRAFT,
        ScheduleWorkProgrammeApplicationStatus.SUBMITTED
    )))
        .thenReturn(List.of(scheduleAppDetail));

    var searchTerm = "term";
    var licenceType = LicenceType.CARBON_STORAGE;

    var licenceReference = "CS001";
    int id = 3;
    var licence = LicenceTestUtil.builder()
        .withId(id)
        .withLicenceReference(licenceReference)
        .withLicenceType(licenceType)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleAppDetail)).thenReturn(licence);

    when(licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE)
    ).thenReturn(List.of(licenceScheduleDetail));

    assertThat(licenceInternalApiService.searchLicencesWithInProgressSchedulesByReferenceTypeAndStatusForEaaApplication(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE,
        serviceUserDetail
    ))
        .isEqualTo(List.of());
  }

  @Test
  void searchLicencesWithInProgressSchedulesByReferenceTypeAndStatusForContinuationApplication() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(Set.of(
        LicenceContinuationApplicationStatus.DRAFT,
        LicenceContinuationApplicationStatus.SUBMITTED
    )))
        .thenReturn(List.of());

    var searchTerm = "term";
    var licenceType = LicenceType.CARBON_STORAGE;

    var licenceReference = "CS001";
    int id = 3;
    var licence = LicenceTestUtil.builder()
        .withId(id)
        .withLicenceReference(licenceReference)
        .withLicenceType(licenceType)
        .build();

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    when(licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE)
    ).thenReturn(List.of(licenceScheduleDetail));

    var licenceJson = new LicenceJson(id, licenceReference);

    assertThat(licenceInternalApiService.searchLicencesWithInProgressSchedulesByReferenceTypeAndStatusForContinuationApplication(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE,
        serviceUserDetail
    ))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson));
  }
}