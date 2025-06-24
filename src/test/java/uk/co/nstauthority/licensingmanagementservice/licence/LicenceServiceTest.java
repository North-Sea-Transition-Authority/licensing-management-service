package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.mockito.Mockito.verify;

import java.util.List;
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
}