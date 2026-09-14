package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LicenceScheduledJobServiceTest {

  @Mock
  private PearsLicenceRefreshService pearsLicenceRefreshService;

  @InjectMocks
  private LicenceScheduledJobService licenceScheduledJobService;

  @Test
  void retrieveAndSavePearsLicences() {
    licenceScheduledJobService.retrieveAndSavePearsLicences();

    verify(pearsLicenceRefreshService).refreshAllLicences();
  }
}
