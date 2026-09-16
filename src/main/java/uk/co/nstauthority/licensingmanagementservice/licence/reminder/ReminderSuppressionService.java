package uk.co.nstauthority.licensingmanagementservice.licence.reminder;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.status.LicenceStatusService;

@Service
public class ReminderSuppressionService {

  private final LicenceStatusService licenceStatusService;

  public ReminderSuppressionService(LicenceStatusService licenceStatusService) {
    this.licenceStatusService = licenceStatusService;
  }

  public Set<Integer> getSuppressedLicenceIds(Collection<Licence> licences) {
    if (licences.isEmpty()) {
      return Set.of();
    }

    var currentStatusesByLicenceId = licenceStatusService.getCurrentStatusesByLicenceId(licences);

    return licences.stream()
        .filter(licence -> currentStatusesByLicenceId.get(licence.getId()) != LicenceStatusType.EXTANT)
        .map(Licence::getId)
        .collect(Collectors.toSet());
  }
}
