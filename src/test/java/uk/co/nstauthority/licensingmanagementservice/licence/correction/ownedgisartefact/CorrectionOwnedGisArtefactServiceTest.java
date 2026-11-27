package uk.co.nstauthority.licensingmanagementservice.licence.correction.ownedgisartefact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype.BlockSurrenderType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation.SurrenderDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeService;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.LicencePositionChangeTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;

@ExtendWith(MockitoExtension.class)
class CorrectionOwnedGisArtefactServiceTest {

  private static final LicenceCorrection LICENCE_CORRECTION = LicenceCorrectionTestUtil.newBuilder()
      .withLicence(LicenceTestUtil.builder().build())
      .build();

  @Mock
  private CorrectionOwnedGisArtefactRepository correctionOwnedGisArtefactRepository;

  @Mock
  private CommandJourneyService commandJourneyService;

  @Mock
  private FeatureService featureService;

  @Mock
  private LicencePositionService licencePositionService;

  @Mock
  private LicencePositionChangeService licencePositionChangeService;

  @InjectMocks
  private CorrectionOwnedGisArtefactService correctionOwnedGisArtefactService;

  @Captor
  private ArgumentCaptor<List<CorrectionOwnedGisArtefact>> artefactsCaptor;

  @Test
  void recordCommandJourneys_whenSomeAreAlreadyRecorded_thenOnlyTheNewOnesAreSaved() {
    var recordedJourneyId = UUID.randomUUID();
    var newJourneyId = UUID.randomUUID();

    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.COMMAND_JOURNEY))
        .thenReturn(List.of(artefact(CorrectionOwnedGisArtefactType.COMMAND_JOURNEY, recordedJourneyId)));

    correctionOwnedGisArtefactService.recordCommandJourneys(
        LICENCE_CORRECTION,
        List.of(recordedJourneyId, newJourneyId, newJourneyId)
    );

    verify(correctionOwnedGisArtefactRepository).saveAll(artefactsCaptor.capture());
    assertThat(artefactsCaptor.getValue())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(artefact(CorrectionOwnedGisArtefactType.COMMAND_JOURNEY, newJourneyId));
  }

  @Test
  void recordFeatures() {
    var featureId = UUID.randomUUID();

    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.FEATURE))
        .thenReturn(List.of());

    correctionOwnedGisArtefactService.recordFeatures(LICENCE_CORRECTION, List.of(featureId));

    verify(correctionOwnedGisArtefactRepository).saveAll(artefactsCaptor.capture());
    assertThat(artefactsCaptor.getValue())
        .usingRecursiveFieldByFieldElementComparator()
        .containsExactly(artefact(CorrectionOwnedGisArtefactType.FEATURE, featureId));
  }

  @Test
  void deleteJourneysNotIn_whenSomeJourneysAreStillUsed_thenOnlyTheUnusedOnesThatExistAreDeleted() {
    var stillUsedJourney = artefact(CorrectionOwnedGisArtefactType.COMMAND_JOURNEY, UUID.randomUUID());
    var unusedJourney = artefact(CorrectionOwnedGisArtefactType.COMMAND_JOURNEY, UUID.randomUUID());
    var alreadyDeletedJourney = artefact(CorrectionOwnedGisArtefactType.COMMAND_JOURNEY, UUID.randomUUID());

    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.COMMAND_JOURNEY))
        .thenReturn(List.of(stillUsedJourney, unusedJourney, alreadyDeletedJourney));
    when(commandJourneyService.findExistingCommandJourneyIds(
        List.of(unusedJourney.getArtefactId(), alreadyDeletedJourney.getArtefactId())))
        .thenReturn(Set.of(unusedJourney.getArtefactId()));

    correctionOwnedGisArtefactService.deleteJourneysNotIn(LICENCE_CORRECTION, Set.of(stillUsedJourney.getArtefactId()));

    verify(commandJourneyService).deleteCommandJourney(unusedJourney.getArtefactId());
    verify(commandJourneyService, never()).deleteCommandJourney(stillUsedJourney.getArtefactId());
    verify(commandJourneyService, never()).deleteCommandJourney(alreadyDeletedJourney.getArtefactId());
    verify(correctionOwnedGisArtefactRepository).deleteAll(List.of(unusedJourney, alreadyDeletedJourney));
  }

  @Test
  void deleteUnreferencedFeatures_whenSomeCandidatesAreReferenced_thenOnlyTheUnreferencedOnesAreDeleted() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var subareaBlockId = UUID.randomUUID();
    var surrenderedBlockId = UUID.randomUUID();
    var keptPartId = UUID.randomUUID();
    var executedSurrender = LicenceOperation.newPartialSurrenderOperation()
        .withSurrenderedFeatureIds(List.of(surrenderedBlockId))
        .withSurrenderDetails(Map.of(surrenderedBlockId, new SurrenderDetails(
            BlockSurrenderType.PARTIAL_SURRENDER,
            null,
            List.of(UUID.randomUUID()),
            List.of(keptPartId)
        )))
        .build();
    var subareaOperation = LicenceOperation.newSubAreaOperation().withBlockFeatureId(subareaBlockId).build();
    var liveChanges = List.of(
        LicencePositionChangeTestUtil.newBuilder()
            .withLicencePosition(licencePosition)
            .withOperations(List.of(executedSurrender))
            .build(),
        LicencePositionChangeTestUtil.newBuilder()
            .withLicencePosition(licencePosition)
            .withOperations(List.of(subareaOperation))
            .build()
    );
    var orphanedFeature = FeatureTestUtil.builder().build();
    var orphanedExtraFeature = FeatureTestUtil.builder().build();

    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.FEATURE))
        .thenReturn(List.of(
            artefact(CorrectionOwnedGisArtefactType.FEATURE, subareaBlockId),
            artefact(CorrectionOwnedGisArtefactType.FEATURE, keptPartId),
            artefact(CorrectionOwnedGisArtefactType.FEATURE, orphanedFeature.getId())
        ));
    when(licencePositionService.findByLicence(LICENCE_CORRECTION.getLicence())).thenReturn(List.of(licencePosition));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(licencePosition))).thenReturn(liveChanges);
    when(featureService.getFeaturesByIds(Set.of(orphanedFeature.getId(), orphanedExtraFeature.getId())))
        .thenReturn(List.of(orphanedFeature, orphanedExtraFeature));

    correctionOwnedGisArtefactService.deleteUnreferencedFeatures(
        LICENCE_CORRECTION,
        Set.of(orphanedExtraFeature.getId())
    );

    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of(orphanedFeature, orphanedExtraFeature));
    verify(correctionOwnedGisArtefactRepository).deleteByLicenceCorrection(LICENCE_CORRECTION);
  }

  @Test
  void deleteUnreferencedFeatures_whenTheLicenceHasNoPositions_thenEveryCandidateIsDeleted() {
    var recordedFeature = FeatureTestUtil.builder().build();

    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.FEATURE))
        .thenReturn(List.of(artefact(CorrectionOwnedGisArtefactType.FEATURE, recordedFeature.getId())));
    when(licencePositionService.findByLicence(LICENCE_CORRECTION.getLicence())).thenReturn(List.of());
    when(featureService.getFeaturesByIds(Set.of(recordedFeature.getId()))).thenReturn(List.of(recordedFeature));

    correctionOwnedGisArtefactService.deleteUnreferencedFeatures(LICENCE_CORRECTION, Set.of());

    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of(recordedFeature));
    verify(correctionOwnedGisArtefactRepository).deleteByLicenceCorrection(LICENCE_CORRECTION);
  }

  @Test
  void deleteUnreferencedFeatures_whenEveryCandidateIsReferenced_thenOnlyTheRecordsAreCleared() {
    var licencePosition = LicencePositionTestUtil.newBuilder().build();
    var subareaBlockId = UUID.randomUUID();
    var liveChange = LicencePositionChangeTestUtil.newBuilder()
        .withLicencePosition(licencePosition)
        .withOperations(List.of(LicenceOperation.newSubAreaOperation().withBlockFeatureId(subareaBlockId).build()))
        .build();

    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.FEATURE))
        .thenReturn(List.of(artefact(CorrectionOwnedGisArtefactType.FEATURE, subareaBlockId)));
    when(licencePositionService.findByLicence(LICENCE_CORRECTION.getLicence())).thenReturn(List.of(licencePosition));
    when(licencePositionChangeService.findByLicencePositionIn(List.of(licencePosition))).thenReturn(List.of(liveChange));

    correctionOwnedGisArtefactService.deleteUnreferencedFeatures(LICENCE_CORRECTION, Set.of());

    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of());
    verify(correctionOwnedGisArtefactRepository).deleteByLicenceCorrection(LICENCE_CORRECTION);
  }

  @Test
  void deleteUnreferencedFeatures_whenThereAreNoCandidates_thenLiveChangesAreNotRead() {
    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.FEATURE))
        .thenReturn(List.of());

    correctionOwnedGisArtefactService.deleteUnreferencedFeatures(LICENCE_CORRECTION, Set.of());

    verifyNoInteractions(licencePositionService, licencePositionChangeService);
    verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of());
    verify(correctionOwnedGisArtefactRepository).deleteByLicenceCorrection(LICENCE_CORRECTION);
  }

  @Test
  void cleanUpOnCancel() {
    var journey = artefact(CorrectionOwnedGisArtefactType.COMMAND_JOURNEY, UUID.randomUUID());
    var recordedFeature = FeatureTestUtil.builder().build();

    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.COMMAND_JOURNEY))
        .thenReturn(List.of(journey));
    when(commandJourneyService.findExistingCommandJourneyIds(List.of(journey.getArtefactId())))
        .thenReturn(Set.of(journey.getArtefactId()));
    when(correctionOwnedGisArtefactRepository.findByLicenceCorrectionAndArtefactType(
        LICENCE_CORRECTION, CorrectionOwnedGisArtefactType.FEATURE))
        .thenReturn(List.of(artefact(CorrectionOwnedGisArtefactType.FEATURE, recordedFeature.getId())));
    when(licencePositionService.findByLicence(LICENCE_CORRECTION.getLicence())).thenReturn(List.of());
    when(featureService.getFeaturesByIds(Set.of(recordedFeature.getId()))).thenReturn(List.of(recordedFeature));

    correctionOwnedGisArtefactService.cleanUpOnCancel(LICENCE_CORRECTION);

    var inOrder = inOrder(commandJourneyService, correctionOwnedGisArtefactRepository);
    inOrder.verify(commandJourneyService).deleteCommandJourney(journey.getArtefactId());
    inOrder.verify(commandJourneyService).deleteFeaturesNotHeldByAJourney(List.of(recordedFeature));
    inOrder.verify(correctionOwnedGisArtefactRepository).deleteByLicenceCorrection(LICENCE_CORRECTION);
  }

  private static CorrectionOwnedGisArtefact artefact(CorrectionOwnedGisArtefactType artefactType, UUID artefactId) {
    var artefact = new CorrectionOwnedGisArtefact();
    artefact.setLicenceCorrection(LICENCE_CORRECTION);
    artefact.setArtefactType(artefactType);
    artefact.setArtefactId(artefactId);
    return artefact;
  }
}
