package uk.co.nstauthority.licensingmanagementservice.licence;

import net.javacrumbs.shedlock.spring.annotation.SchedulerLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LicenceScheduledJobService {

  private static final Logger LOGGER = LoggerFactory.getLogger(LicenceScheduledJobService.class);

  private final PearsLicenceRefreshService pearsLicenceRefreshService;

  public LicenceScheduledJobService(PearsLicenceRefreshService pearsLicenceRefreshService) {
    this.pearsLicenceRefreshService = pearsLicenceRefreshService;
  }

  @Scheduled(fixedRateString = "PT1H", initialDelayString = "PT1H")
  @SchedulerLock(name = "pearsLicenceRefresh", lockAtMostFor = "PT45M", lockAtLeastFor = "PT1M")
  public void retrieveAndSavePearsLicences() {

    LOGGER.info("Starting update of PEARS licences and responsible organisations");

    pearsLicenceRefreshService.refreshAllLicences();

    LOGGER.info("Completed updating PEARS licences and responsible organisations");
  }

}
