package uk.co.nstauthority.licensingmanagementservice.migration.pears;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;

@ExtendWith(MockitoExtension.class)
class PearsLicenceWritebackEndpointTest {

  @Mock
  private LicenceService licenceService;

  @Mock
  private LicenceWritebackService licenceWritebackService;

  @InjectMocks
  private PearsLicenceWritebackEndpoint pearsLicenceWritebackEndpoint;

  @ParameterizedTest
  @ValueSource(strings = {"P1", "p1", " P1 "})
  void overwriteLicencePositionsFromPears_whenLicenceReferenceGiven_thenOverwritesThatLicence(String licenceReference) {
    var licence = LicenceTestUtil.builder()
        .withLicenceReference("P1")
        .build();

    when(licenceService.findByLicenceReferenceOrThrow("P1")).thenReturn(licence);

    pearsLicenceWritebackEndpoint.overwriteLicencePositionsFromPears(licenceReference);

    verify(licenceWritebackService).overwriteLicencePositionsFromPears(licence);
  }

  @ParameterizedTest
  @NullSource
  @ValueSource(strings = {"", " "})
  void overwriteLicencePositionsFromPears_whenNoLicenceReference_thenThrows(String licenceReference) {
    assertThatThrownBy(() -> pearsLicenceWritebackEndpoint.overwriteLicencePositionsFromPears(licenceReference))
        .isInstanceOf(IllegalArgumentException.class);

    verifyNoInteractions(licenceService, licenceWritebackService);
  }
}
