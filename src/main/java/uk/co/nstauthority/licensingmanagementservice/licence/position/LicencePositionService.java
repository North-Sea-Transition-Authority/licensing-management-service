package uk.co.nstauthority.licensingmanagementservice.licence.position;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.CorrectPositionDateController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.ReinstateLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.RemoveExecutedLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.UndoLicencePositionCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.administrator.LicencePositionAdministratorChangeController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.UpdateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ChronologicalPosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.AdministratorChangeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.LicencePositionChangeViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateViewService;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;

@Service
public class LicencePositionService {

  private static final Comparator<TimelineEntry> TIMELINE_ORDER_COMPARATOR =
      Comparator.comparing(TimelineEntry::date).thenComparingInt(TimelineEntry::order).reversed();

  private static final Comparator<ChronologicalPosition> CHRONOLOGICAL_POSITION_COMPARATOR =
      Comparator.comparing(ChronologicalPosition::date).thenComparingInt(ChronologicalPosition::order);

  private final LicencePositionRepository licencePositionRepository;

  public LicencePositionService(LicencePositionRepository licencePositionRepository) {
    this.licencePositionRepository = licencePositionRepository;
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
    licencePosition.setExecuted(true);

    return licencePositionRepository.save(licencePosition);
  }

  public LicencePosition getPositionForLicence(Licence licence, UUID licencePositionId) {
    return licencePositionRepository.findByIdAndLicence(licencePositionId, licence)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "licencePosition", licencePositionId));
  }

  public List<LicencePosition> getExecutedChronologicalLicencePositions(Licence licence) {
    return licencePositionRepository.findByLicence(licence)
        .stream()
        .filter(LicencePosition::isExecuted)
        .sorted(Comparator.comparing(LicencePosition::getPositionDate).thenComparing(LicencePosition::getPositionDateOrder))
        .toList();
  }
}
