package uk.co.nstauthority.licensingmanagementservice.licence;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.FeatureFlagService;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleasePhase;

@ExtendWith(MockitoExtension.class)
class LicenceScheduledJobServiceTest {

  @Mock
  private PearsLicenceRefreshService pearsLicenceRefreshService;

  @Mock
  private FeatureFlagService featureFlagService;

  @InjectMocks
  private LicenceScheduledJobService licenceScheduledJobService;

  @Test
  void retrieveAndSavePearsLicences_delegatesToThePearsLicenceRefreshService() {
    licenceScheduledJobService.retrieveAndSavePearsLicences();

    verify(featureFlagService).isEnabled(ReleasePhase.LMS2);
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

  @Test
  void retrieveAndSavePearsLicences_LMS2IsEnabled_doesNotRun() {
    when(featureFlagService.isEnabled(ReleasePhase.LMS2)).thenReturn(true);

    licenceScheduledJobService.retrieveAndSavePearsLicences();

    verifyNoInteractions(pearsLicenceRefreshService);
  }
}
