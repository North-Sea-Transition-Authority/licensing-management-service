package uk.co.nstauthority.licensingmanagementservice.licence.correction;

import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.Collection;
import java.util.Optional;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;

@Service
public class LicenceCorrectionService {

  private final LicenceCorrectionRepository licenceCorrectionRepository;
  private final Clock clock;

  public LicenceCorrectionService(LicenceCorrectionRepository licenceCorrectionRepository, Clock clock) {
    this.licenceCorrectionRepository = licenceCorrectionRepository;
    this.clock = clock;
  }

  @Transactional
  public LicenceCorrection startCorrection(
      Licence licence,
      String correctionReference,
      String reason,
      ServiceUserDetail user
  ) {
    if (hasOpenCorrection(licence)) {
      throw new IllegalStateException(
          "Cannot start correction for licence %s as an open correction already exists".formatted(licence.getId())
      );
    }

    var licenceCorrection = new LicenceCorrection();
    licenceCorrection.setLicence(licence);
    licenceCorrection.setCorrectionReference(correctionReference);
    licenceCorrection.setReason(reason);
    licenceCorrection.setStatus(LicenceCorrectionStatus.IN_PROGRESS);
    licenceCorrection.setAllocatedToWuaId(user.wuaId());
    licenceCorrection.setCreatedInstant(Instant.now(clock));

    try {
      return licenceCorrectionRepository.saveAndFlush(licenceCorrection);
    } catch (DataIntegrityViolationException e) {
      throw new IllegalStateException(
          "Cannot start correction for licence %s as an open correction already exists".formatted(licence.getId()), e
      );
    }
  }

  @Transactional
  public void cancelCorrection(LicenceCorrection licenceCorrection) {
    licenceCorrection.setStatus(LicenceCorrectionStatus.CANCELLED);
    licenceCorrectionRepository.save(licenceCorrection);
  }

  public boolean hasOpenCorrection(Licence licence) {
    return licenceCorrectionRepository.existsByLicenceAndStatus(licence, LicenceCorrectionStatus.IN_PROGRESS);
  }

  public Optional<LicenceCorrection> findByIdAndAllocatedToWuaId(UUID correctionId, ServiceUserDetail user) {
    return licenceCorrectionRepository.findByIdAndAllocatedToWuaId(correctionId, user.wuaId());
  }

  public Collection<LicenceCorrection> getAllInProgressCorrectionsForUser(ServiceUserDetail user) {
    return licenceCorrectionRepository
        .findAllByStatusAndAllocatedToWuaId(LicenceCorrectionStatus.IN_PROGRESS, user.wuaId());
  }

  public LicenceCorrection getInProgressCorrectionOrThrow(Licence licence) {
    return licenceCorrectionRepository.findByLicenceAndStatus(licence, LicenceCorrectionStatus.IN_PROGRESS)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "No in progress correction found for licence %s".formatted(licence.getId())));
  }
}
