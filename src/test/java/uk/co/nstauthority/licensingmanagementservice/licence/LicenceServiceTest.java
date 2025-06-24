package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LicenceServiceTest {

  @Mock
  private LicenceRepository licenceRepository;

  @InjectMocks
  private LicenceService licenceService;

  @Test
  void saveLicences() {
    var licences = List.of(new Licence());

    licenceService.saveLicences(licences);

    verify(licenceRepository).saveAll(licences);
  }

  @Test
  void getNextLicenceId_noLicences() {
    when(licenceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.empty());

    assertThat(licenceService.getNextLicenceId()).isEqualTo(10000);
  }

  @Test
  void getNextLicenceId_maxIdBelow10000() {
    var licence = new Licence();
    licence.setId(1);

    when(licenceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(licence));

    assertThat(licenceService.getNextLicenceId()).isEqualTo(10000);
  }

  @Test
  void getNextLicenceId_maxIdAbove10000() {
    var licence = new Licence();
    licence.setId(10000);

    when(licenceRepository.findTopByOrderByIdDesc()).thenReturn(Optional.of(licence));

    assertThat(licenceService.getNextLicenceId()).isEqualTo(10001);
  }
}