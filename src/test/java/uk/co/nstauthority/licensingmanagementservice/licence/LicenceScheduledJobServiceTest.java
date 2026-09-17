package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
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
  void retrieveAndSavePearsLicences_delegatesToThePearsLicenceRefreshService() {
    licenceScheduledJobService.retrieveAndSavePearsLicences();

    verify(pearsLicenceRefreshService).refreshAllLicences();
  }

  @Test
  void retrieveAndSavePearsLicences_isLockedSoOnlyOneInstanceRefreshes() throws NoSuchMethodException {
    var schedulerLock = LicenceScheduledJobService.class
        .getMethod("retrieveAndSavePearsLicences")
        .getAnnotation(SchedulerLock.class);

    assertThat(schedulerLock).isNotNull();
    assertThat(schedulerLock.name()).isEqualTo("pearsLicenceRefresh");
    assertThat(schedulerLock.lockAtMostFor()).isEqualTo("PT45M");
    assertThat(schedulerLock.lockAtLeastFor()).isEqualTo("PT1M");
  }
}
