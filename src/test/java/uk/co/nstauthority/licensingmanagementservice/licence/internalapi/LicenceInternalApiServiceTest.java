package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@ExtendWith(MockitoExtension.class)
class LicenceInternalApiServiceTest {

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private LicenceService licenceService;

  @InjectMocks
  private LicenceInternalApiService licenceInternalApiService;

  private ServiceUserDetail serviceUserDetail;

  @Test
  void searchLicencesByReferenceAndType() {
    var searchTerm = "term";
    var licenceType = LicenceType.SEAWARD_PRODUCTION;
    var licence = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceReference("P001")
        .build();

    when(licenceService.searchLicencesByReferenceAndTypes(searchTerm, List.of(licenceType))).thenReturn(List.of(licence));

    var licenceJson = new LicenceJson(licence.getId(), licence.getLicenceReference());

    assertThat(licenceInternalApiService.searchLicencesByReferenceAndType(searchTerm, List.of(licenceType)))
        .isEqualTo(List.of(licenceJson));
  }

  @Test
  void searchLicencesWithInProgressSchedulesByReferenceTypeAndStatus() {
    serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build();
    int authorizedUnitId = 99;
    when(applicationAccessService.getOrganisationUnitIds(serviceUserDetail)).thenReturn(Set.of(authorizedUnitId));

    var searchTerm = "term";
    var licenceType = LicenceType.CARBON_STORAGE;

    var licenceReference = "CS001";
    int id = 3;
    var licence = LicenceTestUtil.builder()
        .withId(id)
        .withLicenceReference(licenceReference)
        .withLicenceType(licenceType)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    var licenceResponsibleOrganisation = new LicenceResponsibleOrganisation();
    licenceResponsibleOrganisation.setResponsibleOrganisationId(authorizedUnitId);
    licenceResponsibleOrganisation.setLicence(licence);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(licence))).thenReturn(List.of(licenceResponsibleOrganisation));

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(licenceSchedule);

    when(licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE)
    ).thenReturn(List.of(licenceScheduleDetail));

    var licenceJson = new LicenceJson(id, licenceReference);

    assertThat(licenceInternalApiService.searchLicencesWithInProgressSchedulesByReferenceTypeAndStatus(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE,
        serviceUserDetail
    ))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson));
  }

  @Test
  void searchLicencesWithInProgressSchedulesByReferenceTypeAndStatus_whenLicenceNotExtant_thenNotReturned() {
    serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().withWuaId(1L).build();
    int authorizedUnitId = 99;
    when(applicationAccessService.getOrganisationUnitIds(serviceUserDetail)).thenReturn(Set.of(authorizedUnitId));

    var searchTerm = "term";
    var licenceType = LicenceType.CARBON_STORAGE;

    var extantLicenceReference = "CS001";
    int extantLicenceId = 3;
    var extantLicence = LicenceTestUtil.builder()
        .withId(extantLicenceId)
        .withLicenceReference(extantLicenceReference)
        .withLicenceType(licenceType)
        .withStatus(LicenceStatus.EXTANT)
        .build();

    var revokedLicence = LicenceTestUtil.builder()
        .withId(4)
        .withLicenceReference("CS002")
        .withLicenceType(licenceType)
        .withStatus(LicenceStatus.REVOKED)
        .build();

    var extantLicenceResponsibleOrganisation = new LicenceResponsibleOrganisation();
    extantLicenceResponsibleOrganisation.setResponsibleOrganisationId(authorizedUnitId);
    extantLicenceResponsibleOrganisation.setLicence(extantLicence);
    when(licenceResponsibleOrganisationService.getAllByLicenceIn(List.of(extantLicence)))
        .thenReturn(List.of(extantLicenceResponsibleOrganisation));

    var extantLicenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(extantLicence);
    var extantLicenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(extantLicenceSchedule);

    var revokedLicenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(revokedLicence);
    var revokedLicenceScheduleDetail = LicenceScheduleTestUtil.createLicenceScheduleDetail(revokedLicenceSchedule);

    when(licenceScheduleDetailService.searchByLicenceReferenceLicenceTypeAndStatus(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE)
    ).thenReturn(List.of(extantLicenceScheduleDetail, revokedLicenceScheduleDetail));

    var licenceJson = new LicenceJson(extantLicenceId, extantLicenceReference);

    assertThat(licenceInternalApiService.searchLicencesWithInProgressSchedulesByReferenceTypeAndStatus(
        searchTerm,
        List.of(licenceType),
        LicenceScheduleDetailStatus.ACTIVE,
        serviceUserDetail
    ))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson));
  }
}