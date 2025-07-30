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

@ExtendWith(MockitoExtension.class)
class LicenceInternalApiServiceTest {

  @Mock
  private LicenceRepository licenceRepository;

  @InjectMocks
  private LicenceInternalApiService licenceInternalApiService;

  @Test
  void searchLicencesWithoutScheduleByReference() {
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
  void searchLicencesByReferenceAndType() {
    var searchTerm = "term";
    var licenceType = LicenceType.GAS_STORAGE;

    var licenceReference = "GS001";
    int id = 3;
    var licence3 = buildLicence(id, licenceReference);

    when(licenceRepository.findAllByLicenceReferenceContainingIgnoreCaseAndType(searchTerm, licenceType)).thenReturn(List.of(licence3));

    var licenceJson1 = new LicenceJson(id, licenceReference);

    assertThat(licenceInternalApiService.searchLicencesByReferenceAndType(searchTerm, licenceType))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson1));
  }

  private Licence buildLicence(int id, String licenceReference) {
    var licence = new Licence();
    licence.setId(id);
    licence.setLicenceReference(licenceReference);
    return licence;
  }
}