package uk.co.nstauthority.licensingmanagementservice.licence.status;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;

@Service
public class LicenceStatusService {

  private final LicenceStatusRepository licenceStatusRepository;
  private final Clock clock;

  public LicenceStatusService(LicenceStatusRepository licenceStatusRepository, Clock clock) {
    this.licenceStatusRepository = licenceStatusRepository;
    this.clock = clock;
  }

  @Transactional
  public void recordLicenceStatus(Licence licence, LicenceStatusType status) {
    recordLicenceStatus(licence, status, LocalDate.now(clock));
  }

  @Transactional
  public void recordLicenceStatus(Licence licence, LicenceStatusType status, LocalDate statusDate) {
    var licenceStatus = new LicenceStatus();
    licenceStatus.setLicence(licence);
    licenceStatus.setStatus(status);
    licenceStatus.setStatusDate(statusDate);
    licenceStatusRepository.save(licenceStatus);
  }

  public LicenceStatusType getCurrentStatus(Licence licence) {
    return getLatestLicenceStatus(licence)
        .map(LicenceStatus::getStatus)
        .orElse(null);
  }

  public Optional<LicenceStatus> getLatestLicenceStatus(Licence licence) {
    return licenceStatusRepository.findAllByLicence(licence).stream()
        .max(Comparator.comparing(LicenceStatus::getStatusDate));
  }

  public Map<Integer, LicenceStatusType> getCurrentStatusesByLicenceId(Collection<Licence> licences) {
    var licenceIds = licences.stream()
        .map(Licence::getId)
        .collect(Collectors.toSet());

    return licenceStatusRepository.findAllByLicence_IdIn(licenceIds).stream()
        .collect(Collectors.groupingBy(
            licenceStatus -> licenceStatus.getLicence().getId(),
            Collectors.collectingAndThen(
                Collectors.maxBy(Comparator.comparing(LicenceStatus::getStatusDate)),
                latest -> latest.map(LicenceStatus::getStatus).orElse(null)
            )
        ));
  }
}
