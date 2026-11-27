package uk.co.nstauthority.licensingmanagementservice.licence.correction.ownedgisartefact;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.GeospatialLicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChange;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;

@Service
public class CorrectionOwnedGisArtefactService {

  private final CorrectionOwnedGisArtefactRepository correctionOwnedGisArtefactRepository;
  private final CommandJourneyService commandJourneyService;
  private final FeatureService featureService;
  private final LicencePositionService licencePositionService;
  private final LicencePositionChangeService licencePositionChangeService;

  CorrectionOwnedGisArtefactService(
      CorrectionOwnedGisArtefactRepository correctionOwnedGisArtefactRepository,
      CommandJourneyService commandJourneyService,
      FeatureService featureService,
      LicencePositionService licencePositionService,
      LicencePositionChangeService licencePositionChangeService
  ) {
    this.correctionOwnedGisArtefactRepository = correctionOwnedGisArtefactRepository;
    this.commandJourneyService = commandJourneyService;
    this.featureService = featureService;
    this.licencePositionService = licencePositionService;
    this.licencePositionChangeService = licencePositionChangeService;
  }

  @Transactional
  public void recordCommandJourneys(LicenceCorrection licenceCorrection, Collection<UUID> commandJourneyIds) {
    createGisOwnedArtefact(licenceCorrection, CorrectionOwnedGisArtefactType.COMMAND_JOURNEY, commandJourneyIds);
  }

  @Transactional
  public void recordFeatures(LicenceCorrection licenceCorrection, Collection<UUID> featureIds) {
    createGisOwnedArtefact(licenceCorrection, CorrectionOwnedGisArtefactType.FEATURE, featureIds);
  }

  @Transactional
  public void deleteJourneysNotIn(LicenceCorrection licenceCorrection, Set<UUID> stillUsedCommandJourneyIds) {
    deleteUnusedJourneys(licenceCorrection, stillUsedCommandJourneyIds);
  }

  @Transactional
  public void deleteUnreferencedFeatures(LicenceCorrection licenceCorrection, Set<UUID> extraCandidateFeatureIds) {
    deleteUnreferencedFeaturesAndRecords(licenceCorrection, extraCandidateFeatureIds);
  }

  @Transactional
  public void cleanUpOnCancel(LicenceCorrection licenceCorrection) {
    deleteUnusedJourneys(licenceCorrection, Set.of());
    deleteUnreferencedFeaturesAndRecords(licenceCorrection, Set.of());
  }

  private void deleteUnusedJourneys(LicenceCorrection licenceCorrection, Set<UUID> stillUsedCommandJourneyIds) {
    var unusedJourneyArtefacts = correctionOwnedGisArtefactRepository
        .findByLicenceCorrectionAndArtefactType(licenceCorrection, CorrectionOwnedGisArtefactType.COMMAND_JOURNEY)
        .stream()
        .filter(artefact -> !stillUsedCommandJourneyIds.contains(artefact.getArtefactId()))
        .toList();

    commandJourneyService.findExistingCommandJourneyIds(
        unusedJourneyArtefacts.stream().map(CorrectionOwnedGisArtefact::getArtefactId).toList()
    ).forEach(commandJourneyService::deleteCommandJourney);

    correctionOwnedGisArtefactRepository.deleteAll(unusedJourneyArtefacts);
  }

  private void deleteUnreferencedFeaturesAndRecords(
      LicenceCorrection licenceCorrection,
      Set<UUID> extraCandidateFeatureIds
  ) {
    var candidateFeatureIds = new HashSet<>(extraCandidateFeatureIds);
    correctionOwnedGisArtefactRepository
        .findByLicenceCorrectionAndArtefactType(licenceCorrection, CorrectionOwnedGisArtefactType.FEATURE)
        .forEach(artefact -> candidateFeatureIds.add(artefact.getArtefactId()));

    if (!candidateFeatureIds.isEmpty()) {
      candidateFeatureIds.removeAll(getReferencedFeatureIds(licenceCorrection.getLicence()));
    }

    commandJourneyService.deleteFeaturesNotHeldByAJourney(featureService.getFeaturesByIds(candidateFeatureIds));

    correctionOwnedGisArtefactRepository.deleteByLicenceCorrection(licenceCorrection);
  }

  private void createGisOwnedArtefact(
      LicenceCorrection licenceCorrection,
      CorrectionOwnedGisArtefactType artefactType,
      Collection<UUID> artefactIds
  ) {
    var recordedIds = correctionOwnedGisArtefactRepository
        .findByLicenceCorrectionAndArtefactType(licenceCorrection, artefactType)
        .stream()
        .map(CorrectionOwnedGisArtefact::getArtefactId)
        .collect(Collectors.toSet());

    var newArtefacts = artefactIds.stream()
        .distinct()
        .filter(artefactId -> !recordedIds.contains(artefactId))
        .map(artefactId -> {
          var artefact = new CorrectionOwnedGisArtefact();
          artefact.setLicenceCorrection(licenceCorrection);
          artefact.setArtefactType(artefactType);
          artefact.setArtefactId(artefactId);
          return artefact;
        })
        .toList();

    correctionOwnedGisArtefactRepository.saveAll(newArtefacts);
  }

  private Set<UUID> getReferencedFeatureIds(Licence licence) {
    var licencePositions = licencePositionService.findByLicence(licence);
    if (licencePositions.isEmpty()) {
      return Set.of();
    }

    return licencePositionChangeService.findByLicencePositionIn(licencePositions).stream()
        .flatMap(change -> LicencePositionChange.operationsOf(change).stream())
        .flatMap(operation -> referencedFeatureIdsOf(operation).stream())
        .collect(Collectors.toSet());
  }

  private static Set<UUID> referencedFeatureIdsOf(LicenceOperation operation) {
    var featureIds = new HashSet<UUID>();
    if (operation instanceof GeospatialLicenceOperation geospatialOperation) {
      featureIds.addAll(geospatialOperation.featureIds());
    }
    if (operation instanceof PartialSurrenderOperation partialSurrenderOperation) {
      featureIds.addAll(partialSurrenderOperation.createdFeatureIds());
    }
    return featureIds;
  }
}
