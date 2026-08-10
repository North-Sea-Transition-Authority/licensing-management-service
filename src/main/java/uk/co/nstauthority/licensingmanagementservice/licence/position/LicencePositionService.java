package uk.co.nstauthority.licensingmanagementservice.licence.position;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil.BLOCK_ORDER;

import java.time.LocalDate;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.transaction.LicenceTransaction;

@Service
public class LicencePositionService {

  private static final Comparator<LicencePosition> CHRONOLOGICAL_ORDER = Comparator
      .comparing(LicencePosition::getPositionDate)
      .thenComparing(LicencePosition::getPositionDateOrder);

  private final LicencePositionRepository licencePositionRepository;
  private final FeatureService featureService;

  public LicencePositionService(
      LicencePositionRepository licencePositionRepository,
      FeatureService featureService
  ) {
    this.licencePositionRepository = licencePositionRepository;
    this.featureService = featureService;
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

    // a new position starts out holding whatever the licence held going into it
    licencePosition.setFeatureIds(findPositionOnLicenceOnOrBefore(licence, positionDate, positionDateOrder)
        .map(previous -> Set.copyOf(previous.getFeatureIds()))
        .orElseGet(Set::of));

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
        .sorted(CHRONOLOGICAL_ORDER)
        .toList();
  }

  @Transactional
  public void setFeatures(
      LicencePosition licencePosition,
      Collection<Feature> features
  ) {
    var featureIds = features.stream()
        .map(feature -> Optional.ofNullable(feature.getId())
            .orElseThrow(() -> new IllegalStateException(
                "Features to be associated with licence position %s have not been persisted before updating the position features"
                    .formatted(licencePosition.getId()))
            ))
        .collect(Collectors.toSet());

    licencePosition.setFeatureIds(featureIds);

    licencePositionRepository.save(licencePosition);
  }

  public List<Feature> getFeatures(LicencePosition licencePosition) {
    return featureService.getFeaturesByIds(licencePosition.getFeatureIds());
  }

  public List<Feature> getBlockFeatures(LicencePosition licencePosition) {
    return getFeatures(licencePosition)
        .stream()
        .filter(LicenceBlockFeatureUtil::isLicenceBlock)
        .sorted(BLOCK_ORDER)
        .toList();
  }

  public List<Feature> getBlockFeaturesOnLicenceOnOrBefore(
      Licence licence,
      LocalDate positionDate,
      int positionDateOrder
  ) {
    return findPositionOnLicenceOnOrBefore(licence, positionDate, positionDateOrder)
        .map(this::getBlockFeatures)
        .orElseGet(List::of);
  }

  private Optional<LicencePosition> findPositionOnLicenceOnOrBefore(
      Licence licence,
      LocalDate positionDate,
      int positionDateOrder
  ) {
    return getExecutedChronologicalLicencePositions(licence)
        .stream()
        .filter(position -> position.getPositionDate().isBefore(positionDate)
            || (position.getPositionDate().isEqual(positionDate) && position.getPositionDateOrder() <= positionDateOrder))
        .max(CHRONOLOGICAL_ORDER);
  }
}
