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
  void searchLicencesWithoutScheduleByReference() {
    var searchTerm = "term";

    var licence = new Licence();
    licence.setId(1);
    licence.setLicenceReference("CS001");

    var licence2 = new Licence();
    licence2.setId(2);
    licence2.setLicenceReference("CS002");

    when(licenceRepository.findAllByLicenceReferenceContainingIgnoreCase(searchTerm)).thenReturn(List.of(licence, licence2));

    var licenceJson1 = new LicenceJson(1, "CS001");
    var licenceJson2 = new LicenceJson(2, "CS002");

    assertThat(licenceInternalApiService.searchLicencesByReference(searchTerm))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson1, licenceJson2));
  }
}