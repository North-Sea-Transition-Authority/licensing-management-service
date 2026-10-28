package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.LicencePositionCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads.LicencePositionPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePosition;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.BlockSubareas;
import uk.co.nstauthority.licensingmanagementservice.licence.position.spatial.LicencePositionSpatialService;

@ExtendWith(MockitoExtension.class)
class SubareaEquityOptionsServiceTest {

  private static final LicenceCorrection CORRECTION = LicenceCorrectionTestUtil.newBuilder().build();
  private static final LicencePosition POSITION = LicencePositionTestUtil.newBuilder().build();
  private static final Feature BLOCK_30_1 = FeatureTestUtil.builder().withFeatureName("30/1").build();
  private static final Feature BLOCK_30_2 = FeatureTestUtil.builder().withFeatureName("30/2").build();

  @Mock
  private LicencePositionSpatialService licencePositionSpatialService;

  @InjectMocks
  private SubareaEquityOptionsService subareaEquityOptionsService;

  @Test
  void getSubareaOptions_whenSubareasOnSeveralBlocks_thenOptionPerSubareaInBlockOrder() {
    var subareaA = new SubareaDetails(UUID.randomUUID(), "Name A", "Short name A");
    var subareaB = new SubareaDetails(UUID.randomUUID(), "Name B", "Short name B");
    var subareaC = new SubareaDetails(UUID.randomUUID(), "Name C", "Short name C");
    var subareaZ = new SubareaDetails(UUID.randomUUID(), null, "Short name Z");
    when(licencePositionSpatialService.getBlockSubareasGoingIntoChange(CORRECTION, POSITION.getId(), null))
        .thenReturn(List.of(
            new BlockSubareas(BLOCK_30_1, List.of(subareaA, subareaB)),
            new BlockSubareas(BLOCK_30_2, List.of(subareaC, subareaZ))
        ));

    var result = subareaEquityOptionsService.getSubareaOptions(CORRECTION, POSITION);

    assertThat(result).containsExactly(
        entry("%s:%s".formatted(BLOCK_30_1.getId(), subareaA.featureId()), "Block 30/1 - Name A"),
        entry("%s:%s".formatted(BLOCK_30_1.getId(), subareaB.featureId()), "Block 30/1 - Name B"),
        entry("%s:%s".formatted(BLOCK_30_2.getId(), subareaC.featureId()), "Block 30/2 - Name C"),
        entry("%s:%s".formatted(BLOCK_30_2.getId(), subareaZ.featureId()), "Block 30/2 - Subarea Short name Z")
    );
  }

  @Test
  void getSubareaOptions_whenSubareasReturnedOutOfOrder_thenOptionsSortedByLabel() {
    var subareaA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
    var subareaB = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
    var subareaC = new SubareaDetails(UUID.randomUUID(), "Subarea C", "C");
    when(licencePositionSpatialService.getBlockSubareasGoingIntoChange(CORRECTION, POSITION.getId(), null))
        .thenReturn(List.of(
            new BlockSubareas(BLOCK_30_2, List.of(subareaC, subareaA)),
            new BlockSubareas(BLOCK_30_1, List.of(subareaB, subareaA))
        ));

    var result = subareaEquityOptionsService.getSubareaOptions(CORRECTION, POSITION);

    assertThat(result).containsExactly(
        entry("%s:%s".formatted(BLOCK_30_1.getId(), subareaA.featureId()), "Block 30/1 - Subarea A"),
        entry("%s:%s".formatted(BLOCK_30_1.getId(), subareaB.featureId()), "Block 30/1 - Subarea B"),
        entry("%s:%s".formatted(BLOCK_30_2.getId(), subareaA.featureId()), "Block 30/2 - Subarea A"),
        entry("%s:%s".formatted(BLOCK_30_2.getId(), subareaC.featureId()), "Block 30/2 - Subarea C"));
  }

  @Test
  void getSubareaOptions_whenLegacySubareaWithoutFeatureOrName_thenKeyedAndLabelledByShortName() {
    var legacySubarea = new SubareaDetails(null, null, "LEG");
    when(licencePositionSpatialService.getBlockSubareasGoingIntoChange(CORRECTION, POSITION.getId(), null))
        .thenReturn(List.of(new BlockSubareas(BLOCK_30_1, List.of(legacySubarea))));

    var result = subareaEquityOptionsService.getSubareaOptions(CORRECTION, POSITION);

    assertThat(result).containsExactly(
        entry("%s:LEG".formatted(BLOCK_30_1.getId()), "Block 30/1 - Subarea LEG"));
  }

  @Test
  void getSubareaOptions_whenTwoSubareasOnBlockShareKey_thenFirstSubareaKept() {
    var featureId = UUID.randomUUID();
    var subarea = new SubareaDetails(featureId, "Subarea A", "A");
    var renamedSubarea = new SubareaDetails(featureId, "Subarea A renamed", "A");
    when(licencePositionSpatialService.getBlockSubareasGoingIntoChange(CORRECTION, POSITION.getId(), null))
        .thenReturn(List.of(new BlockSubareas(BLOCK_30_1, List.of(subarea, renamedSubarea))));

    var result = subareaEquityOptionsService.getSubareaOptions(CORRECTION, POSITION);

    assertThat(result).containsExactly(
        entry("%s:%s".formatted(BLOCK_30_1.getId(), featureId), "Block 30/1 - Subarea A"));
  }

  @Test
  void getSubareaOptions_whenAddedPosition_thenOptionsForSubareasOnAddedPosition() {
    var addedPositionId = UUID.randomUUID();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(CORRECTION)
        .withPayload(LicencePositionPayload.newCreateLicencePositionPayload()
            .withLicencePositionId(addedPositionId.toString())
            .build())
        .build();
    var subarea = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
    when(licencePositionSpatialService.getBlockSubareasGoingIntoChange(CORRECTION, addedPositionId, null))
        .thenReturn(List.of(new BlockSubareas(BLOCK_30_1, List.of(subarea))));

    var result = subareaEquityOptionsService.getSubareaOptions(positionCorrection);

    assertThat(result).containsExactly(
        entry("%s:%s".formatted(BLOCK_30_1.getId(), subarea.featureId()), "Block 30/1 - Subarea A"));
  }

  @Test
  void getSubareaOptionOrThrow_whenKeyMatchesSubareaOnLaterBlock_thenReturnsThatBlockAndSubarea() {
    var subareaA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
    var subareaC = new SubareaDetails(UUID.randomUUID(), "Subarea C", "C");
    when(licencePositionSpatialService.getBlockSubareasGoingIntoChange(CORRECTION, POSITION.getId(), null))
        .thenReturn(List.of(
            new BlockSubareas(BLOCK_30_1, List.of(subareaA)),
            new BlockSubareas(BLOCK_30_2, List.of(subareaC))
        ));

    var result = subareaEquityOptionsService.getSubareaOptionOrThrow(
        CORRECTION, POSITION, "%s:%s".formatted(BLOCK_30_2.getId(), subareaC.featureId()));

    assertThat(result).isEqualTo(new SubareaOption(BLOCK_30_2, subareaC));
  }

  @Test
  void getSubareaOptionOrThrow_whenAddedPositionAndLegacyKey_thenReturnsLegacySubarea() {
    var addedPositionId = UUID.randomUUID();
    var positionCorrection = LicencePositionCorrectionTestUtil.newBuilder()
        .withLicenceCorrection(CORRECTION)
        .withPayload(LicencePositionPayload.newCreateLicencePositionPayload()
            .withLicencePositionId(addedPositionId.toString())
            .build())
        .build();
    var legacySubarea = new SubareaDetails(null, null, "LEG");
    when(licencePositionSpatialService.getBlockSubareasGoingIntoChange(CORRECTION, addedPositionId, null))
        .thenReturn(List.of(new BlockSubareas(BLOCK_30_1, List.of(legacySubarea))));

    var result = subareaEquityOptionsService.getSubareaOptionOrThrow(
        positionCorrection, "%s:LEG".formatted(BLOCK_30_1.getId()));

    assertThat(result).isEqualTo(new SubareaOption(BLOCK_30_1, legacySubarea));
  }

  @Test
  void getSubareaOptionOrThrow_whenNoSubareaMatchesKey_thenThrowsNotFound() {
    when(licencePositionSpatialService.getBlockSubareasGoingIntoChange(CORRECTION, POSITION.getId(), null))
        .thenReturn(List.of(new BlockSubareas(
            BLOCK_30_1,
            List.of(new SubareaDetails(UUID.randomUUID(), "Subarea A", "A"))
        )));
    var unknownKey = "%s:%s".formatted(BLOCK_30_1.getId(), UUID.randomUUID());

    assertThatThrownBy(() -> subareaEquityOptionsService.getSubareaOptionOrThrow(CORRECTION, POSITION, unknownKey))
        .isInstanceOf(LmsEntityNotFoundException.class);
  }
}
