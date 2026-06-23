package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class LicencePositionService {

  private final LicencePositionRepository licencePositionRepository;

  public LicencePositionService(LicencePositionRepository licencePositionRepository) {
    this.licencePositionRepository = licencePositionRepository;
  }

  public List<LicencePositionTimelineView> getTimelineView(Licence licence) {
    return getChronologicalLicencePositions(licence).reversed().stream()
        .map(licencePosition -> new LicencePositionTimelineView(
            licencePosition.getId(),
            ReverseRouter.route(on(LicencePositionTimelineController.class).renderLicencePosition(
                licence,
                licencePosition.getId()
            )),
            licencePosition.getLicenceTransaction().getRegulatorReference(),
            licencePosition.getFormattedPositionDate()
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

  public LicencePosition getPositionForLicence(Licence licence, UUID licencePositionId) {
    return licencePositionRepository.findByIdAndLicence(licencePositionId, licence)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "licencePosition", licencePositionId));
  }

  public List<LicencePosition> getChronologicalLicencePositions(Licence licence) {
    return licencePositionRepository.findByLicence(licence)
        .stream()
        .sorted(Comparator.comparing(LicencePosition::getPositionDate).thenComparing(LicencePosition::getPositionDateOrder))
        .toList();
  }
}
