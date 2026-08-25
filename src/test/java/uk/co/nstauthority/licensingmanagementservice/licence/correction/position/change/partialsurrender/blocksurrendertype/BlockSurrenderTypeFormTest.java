package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.blocksurrendertype;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.PartialSurrenderOperation;

class BlockSurrenderTypeFormTest {

  private static final UUID FEATURE_ID = UUID.randomUUID();
  private static final UUID OTHER_FEATURE_ID = UUID.randomUUID();

  @Test
  void from_whenOperationNull_thenSurrenderTypeNotSet() {
    var form = BlockSurrenderTypeForm.from(null, FEATURE_ID);

    assertThat(form.getSurrenderType()).isNull();
  }

  @Test
  void from_whenOperationHasNoTypeForFeature_thenSurrenderTypeNotSet() {
    var operation = operationWithTypes(Map.of(OTHER_FEATURE_ID, BlockSurrenderType.FULL_SURRENDER));

    var form = BlockSurrenderTypeForm.from(operation, FEATURE_ID);

    assertThat(form.getSurrenderType()).isNull();
  }

  @Test
  void from_whenOperationHasTypeForFeature_thenSurrenderTypeSetToTypeName() {
    var operation = operationWithTypes(Map.of(FEATURE_ID, BlockSurrenderType.PARTIAL_SURRENDER));

    var form = BlockSurrenderTypeForm.from(operation, FEATURE_ID);

    assertThat(form.getSurrenderType()).isEqualTo(BlockSurrenderType.PARTIAL_SURRENDER.name());
  }

  private PartialSurrenderOperation operationWithTypes(Map<UUID, BlockSurrenderType> typesByFeatureId) {
    var blockSurrenders = typesByFeatureId.entrySet().stream()
        .collect(Collectors.toMap(
            Map.Entry::getKey,
            entry -> new PartialSurrenderOperation.SurrenderDetails(entry.getValue(), UUID.randomUUID(), List.of())));

    return LicenceOperation.newPartialSurrenderOperation()
        .withFeatureIds(List.of(FEATURE_ID, OTHER_FEATURE_ID))
        .withSurrenderDetails(blockSurrenders)
        .build();
  }
}
