package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;

@Service
public class LicencePositionService {

  private final LicencePositionRepository licencePositionRepository;

  public LicencePositionService(LicencePositionRepository licencePositionRepository) {
    this.licencePositionRepository = licencePositionRepository;
  }

  public List<LicencePositionTimelineView> getTimelineView(Licence licence) {
    return licencePositionRepository.findByLicence(licence)
        .stream()
        .sorted(Comparator.comparing(LicencePosition::getPositionDate)
            .thenComparing(LicencePosition::getPositionDateOrder)
            .reversed())
        .map(licencePosition -> new LicencePositionTimelineView(
            licencePosition.getLicenceTransaction().getRegulatorReference(),
            licencePosition.getPositionDate()
        ))
        .toList();
  }

  @Transactional
  public LicencePosition createLicencePosition(
      Licence licence,
      LicenceTransaction transaction,
      LocalDate positionDate
  ) {
    //TODO LMS2-63: Add a lock to licence to serialise concurrent position inserts
    var maxOrder = licencePositionRepository.findMaxPositionDateOrder(licence, positionDate);
    var positionDateOrder = (maxOrder == null) ? 1 : maxOrder + 1;

    LicencePosition licencePosition = new LicencePosition();
    licencePosition.setLicence(licence);
    licencePosition.setLicenceTransaction(transaction);
    licencePosition.setPositionDate(positionDate);
    licencePosition.setPositionDateOrder(positionDateOrder);

    return licencePositionRepository.save(licencePosition);
  }
}
