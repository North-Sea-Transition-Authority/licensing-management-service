package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;

@ExtendWith(MockitoExtension.class)
class LicenceInternalApiServiceTest {

  @Mock
  private LicenceRepository licenceRepository;

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @InjectMocks
  private LicenceInternalApiService licenceInternalApiService;

  @Test
  void searchLicencesByReference() {
    var searchTerm = "term";

    var licence = buildLicence(1, "CS001");
    var licence2 = buildLicence(2, "CS002");

    when(licenceRepository.findAllByLicenceReferenceContainingIgnoreCase(searchTerm)).thenReturn(List.of(licence, licence2));

    var licenceJson1 = new LicenceJson(1, "CS001");
    var licenceJson2 = new LicenceJson(2, "CS002");

    assertThat(licenceInternalApiService.searchLicencesByReference(searchTerm))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson1, licenceJson2));
  }

  @Test
  void searchLicencesWithSchedulesByReferenceAndType() {
    var searchTerm = "term";
    var licenceType = LicenceType.GAS_STORAGE;

    var licenceReference = "GS001";
    int id = 3;
    var licence = buildLicence(id, licenceReference);

    var licenceSchedule = new LicenceSchedule();
    licenceSchedule.setLicence(licence);

    when(licenceScheduleService.searchAllSchedulesByLicenceRefAndType(searchTerm, licenceType)).thenReturn(List.of(licenceSchedule));

    var licenceJson = new LicenceJson(id, licenceReference);

    assertThat(licenceInternalApiService.searchLicencesWithSchedulesByReferenceAndType(searchTerm, licenceType))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson));
  }

  private Licence buildLicence(int id, String licenceReference) {
    var licence = new Licence();
    licence.setId(id);
    licence.setLicenceReference(licenceReference);
    return licence;
  }
}