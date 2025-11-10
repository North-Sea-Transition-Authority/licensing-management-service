package uk.co.nstauthority.licensingmanagementservice.licence.internalapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;

@ExtendWith(MockitoExtension.class)
class LicenceInternalApiServiceTest {

  @Mock
  private LicenceScheduleService licenceScheduleService;

  @InjectMocks
  private LicenceInternalApiService licenceInternalApiService;

  @Test
  void searchLicencesWithSchedulesByReferenceAndType() {
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

    when(licenceScheduleService.searchAllSchedulesByLicenceRefAndType(searchTerm, licenceType)).thenReturn(List.of(licenceSchedule));

    var licenceJson = new LicenceJson(id, licenceReference);

    assertThat(licenceInternalApiService.searchLicencesWithSchedulesByReferenceAndType(searchTerm, licenceType))
        .usingRecursiveComparison()
        .isEqualTo(List.of(licenceJson));
  }
}