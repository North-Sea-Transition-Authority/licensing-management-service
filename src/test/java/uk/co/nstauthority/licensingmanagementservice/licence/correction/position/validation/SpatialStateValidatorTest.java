package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.BlockEndOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaCreateOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionChangeTestUtil;

class SpatialStateValidatorTest {

  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails SUBAREA = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");

  private final SpatialStateValidator spatialStateValidator = new SpatialStateValidator();

  @Test
  void validate_whenInputsHeld_thenNoErrors() {
    var context = PositionValidationContextTestUtil.newBuilder().build();
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(new BlockEndOperation(List.of(BLOCK_FEATURE_ID), List.of(SUBAREA))))
        .build();
    var state = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(SUBAREA));

    var errors = spatialStateValidator.validate(context, change, state);

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenInputsNotHeld_thenErrorForOperation() {
    var context = PositionValidationContextTestUtil.newBuilder().build();
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(new BlockEndOperation(List.of(BLOCK_FEATURE_ID), List.of())))
        .build();

    var errors = spatialStateValidator.validate(context, change, LicencePositionState.EMPTY);

    assertThat(errors).containsExactly(PositionValidationError
        .forOperation(
            context,
            LicenceOperation.BLOCK_END,
            "Blocks ended must only occur on blocks and subareas that exist on that position"
        )
        .withChangeId(change.changeId()));
  }

  @Test
  void validate_whenEarlierOperationInChangeCreatesInputs_thenNoErrors() {
    var context = PositionValidationContextTestUtil.newBuilder().build();
    var change = PositionChangeTestUtil.newBuilder()
        .withOperations(List.of(
            LicenceOperation.newBlockCreateOperation().withFeatureIds(List.of(BLOCK_FEATURE_ID)).build(),
            new SubareaCreateOperation(BLOCK_FEATURE_ID, List.of(SUBAREA))
        ))
        .build();

    var errors = spatialStateValidator.validate(context, change, LicencePositionState.EMPTY);

    assertThat(errors).isEmpty();
  }

  @Test
  void validate_whenOperationNotGeospatial_thenNoErrors() {
    var context = PositionValidationContextTestUtil.newBuilder().build();
    var change = PositionChangeTestUtil.newBuilder()
        .withAdministratorOperation(1)
        .build();

    var errors = spatialStateValidator.validate(context, change, LicencePositionState.EMPTY);

    assertThat(errors).isEmpty();
  }
}
