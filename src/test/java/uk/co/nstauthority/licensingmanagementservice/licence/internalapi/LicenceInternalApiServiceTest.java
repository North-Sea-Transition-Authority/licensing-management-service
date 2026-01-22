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
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailStatus;

@ExtendWith(MockitoExtension.class)
class LicenceInternalApiServiceTest {

  @Mock
  private LicenceScheduleDetailService licenceScheduleDetailService;

  @InjectMocks
  private LicenceInternalApiService licenceInternalApiService;

  private ServiceUserDetail serviceUserDetail;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

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
  void searchLicencesWithSchedulesByReferenceTypeAndStatus() {
    var searchTerm = "term";
    var licenceType = LicenceType.GAS_STORAGE;

    var licenceReference = "GS001";
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

    assertThat(licenceInternalApiService.searchLicencesWithSchedulesByReferenceTypeAndStatus(searchTerm, List.of(licenceType), LicenceScheduleDetailStatus.ACTIVE, serviceUserDetail))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson));
  }
}