package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.CreateLicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionRepository;

@Service
public class LicencePositionCorrectionService {

  private final LicencePositionCorrectionRepository licencePositionCorrectionRepository;
  private final LicencePositionRepository licencePositionRepository;

  public LicencePositionCorrectionService(
      LicencePositionCorrectionRepository licencePositionCorrectionRepository,
      LicencePositionRepository licencePositionRepository
  ) {
    this.licencePositionCorrectionRepository = licencePositionCorrectionRepository;
    this.licencePositionRepository = licencePositionRepository;
  }

  @Transactional
  public void addNewPosition(
      LicenceCorrection licenceCorrection,
      LocalDate positionDate,
      String correctionReference
  ) {
    var newLicencePositionId = UUID.randomUUID().toString();
    var newLicenceTransactionId = UUID.randomUUID().toString();
    var effectiveDateOrder = determineEffectiveDateOrder(licenceCorrection, positionDate);

    var payload = LicencePositionPayload.newCreateLicencePositionPayload()
        .withLicencePositionId(newLicencePositionId)
        .withLicenceTransactionId(newLicenceTransactionId)
        .withEffectiveDate(positionDate)
        .withEffectiveDateOrder(effectiveDateOrder)
        .withCorrectionReference(correctionReference)
        .withChanges(List.of())
        .build();

    var licenceCorrectionPosition = new LicencePositionCorrection();
    licenceCorrectionPosition.setLicenceCorrection(licenceCorrection);
    licenceCorrectionPosition.setChangeType(LicencePositionCorrectionChangeType.ADD_POSITION);
    licenceCorrectionPosition.setTargetLicencePosition(null);
    licenceCorrectionPosition.setPayload(payload);

    licencePositionCorrectionRepository.save(licenceCorrectionPosition);
  }

  @Transactional
  public void removeExecutedPosition(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    if (!licencePosition.isExecuted()) {
      throw new IllegalStateException(
          "Cannot remove licence position %s as it is not executed"
              .formatted(licencePosition.getId()));
    }

    if (!canRemovePosition(licenceCorrection, licencePosition)) {
      throw new IllegalStateException(
          "Cannot remove licence position %s as it is already marked for removal in correction %s"
              .formatted(licencePosition.getId(), licenceCorrection.getId()));
    }

    var licencePositionCorrection = new LicencePositionCorrection();
    licencePositionCorrection.setLicenceCorrection(licenceCorrection);
    licencePositionCorrection.setChangeType(LicencePositionCorrectionChangeType.REMOVE_POSITION);
    licencePositionCorrection.setTargetLicencePosition(licencePosition);
    licencePositionCorrectionRepository.save(licencePositionCorrection);
  }

  //todo: positions marked for removalal cannot have:
  // new changes added to them
  // cannot have any changes updated on them
  public boolean canRemovePosition(LicenceCorrection licenceCorrection, LicencePosition licencePosition) {
    return licencePosition.isExecuted()
        && !licencePositionCorrectionRepository
        .existsByLicenceCorrectionAndTargetLicencePositionAndChangeType(
            licenceCorrection,
            licencePosition,
            LicencePositionCorrectionChangeType.REMOVE_POSITION
        );
  }

  public LicencePositionCorrection getPositionCorrectionForCorrection(
      UUID licencePositionCorrectionId,
      LicenceCorrection licenceCorrection
  ) {
    return licencePositionCorrectionRepository
        .findByIdAndLicenceCorrection(licencePositionCorrectionId, licenceCorrection)
        .orElseThrow(() -> new LmsEntityNotFoundException("licencePositionCorrection", licencePositionCorrectionId));
  }

  @Transactional
  public void undoPositionCorrection(LicencePositionCorrection licencePositionCorrection) {
    licencePositionCorrectionRepository.delete(licencePositionCorrection);
  }

  public Set<UUID> getRemovedLicencePositionIds(LicenceCorrection licenceCorrection) {
    return licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(licenceCorrection, LicencePositionCorrectionChangeType.REMOVE_POSITION)
        .stream()
        .map(LicencePositionCorrection::getTargetLicencePosition)
        .map(LicencePosition::getId)
        .collect(Collectors.toSet());
  }


  public List<LicencePositionCorrection> getAddedLicencePositionCorrections(LicenceCorrection licenceCorrection) {
    return licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(licenceCorrection, LicencePositionCorrectionChangeType.ADD_POSITION);
  }

  public boolean isCorrectionReferenceInUse(LicenceCorrection licenceCorrection, String correctionReference) {
    return getAddedLicencePositionCorrections(licenceCorrection)
        .stream()
        .map(LicencePositionCorrection::getPayload)
        .filter(CreateLicencePositionPayload.class::isInstance)
        .map(CreateLicencePositionPayload.class::cast)
        .map(CreateLicencePositionPayload::correctionReference)
        .anyMatch(existingReference -> existingReference.equalsIgnoreCase(correctionReference));
  }

  //TODO - Effective date order is auto-assigned for now. When multiple
  // positions share the same effective date the user should be able to select the order themselves.
  private int determineEffectiveDateOrder(LicenceCorrection licenceCorrection, LocalDate positionDate) {
    var liveMaxOrder = Optional.ofNullable(
        licencePositionRepository.findMaxPositionDateOrder(licenceCorrection.getLicence(), positionDate)
    ).orElse(0);

    var draftMaxOrder = licencePositionCorrectionRepository
        .findByLicenceCorrectionAndChangeType(licenceCorrection, LicencePositionCorrectionChangeType.ADD_POSITION)
        .stream()
        .map(LicencePositionCorrection::getPayload)
        .filter(CreateLicencePositionPayload.class::isInstance)
        .map(CreateLicencePositionPayload.class::cast)
        .filter(payload -> positionDate.equals(payload.effectiveDate()))
        .map(CreateLicencePositionPayload::effectiveDateOrder)
        .max(Integer::compareTo)
        .orElse(0);

    return Math.max(liveMaxOrder, draftMaxOrder) + 1;
  }
}