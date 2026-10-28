package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Named.named;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContextTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.SubareaEquityPositionRule;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;

class SubareaSetEquityOperationTest {

  private static final UUID BLOCK_FEATURE_ID = UUID.randomUUID();
  private static final UUID SUBAREA_FEATURE_ID = UUID.randomUUID();
  private static final SubareaDetails SUBAREA = new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea A", "A");
  private static final int TRANSFER_TO = 123;

  @Test
  void featureIdsToName_thenReturnsBlockFeatureIdOnly() {
    var operation = new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, TRANSFER_TO, BigDecimal.TEN);

    assertThat(operation.featureIds()).containsExactly(BLOCK_FEATURE_ID);
  }

  @Test
  void organisationUnitIds() {
    var operation = new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, TRANSFER_TO, BigDecimal.TEN);

    var result = operation.organisationUnitIds();

    assertThat(result).containsExactly(TRANSFER_TO);
  }

  @Test
  void applyState() {
    var otherSubarea = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
    var state = LicencePositionState.EMPTY
        .withAdministratorId(1)
        .withStorageOperatorId(2)
        .withSubareaEquity(BLOCK_FEATURE_ID, SUBAREA, 456, new BigDecimal("40"))
        .withSubareaEquity(BLOCK_FEATURE_ID, otherSubarea, 789, new BigDecimal("100"));
    var operation = new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, TRANSFER_TO, new BigDecimal("60"));

    var result = operation.applyState(state);

    var expected = new LicencePositionState(
        1,
        2,
        Set.of(),
        Map.of(),
        Map.of(),
        Map.of(BLOCK_FEATURE_ID, Map.of(
            SUBAREA, Map.of(456, new BigDecimal("40"), TRANSFER_TO, new BigDecimal("60")),
            otherSubarea, Map.of(789, new BigDecimal("100"))
        ))
    );
    assertThat(result).isEqualTo(expected);
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"SEAWARD_PRODUCTION", "LANDWARD_PRODUCTION"})
  void validate_whenProductionLicence_thenReturnsNoError(LicenceType licenceType) {
    var operation = new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, TRANSFER_TO, BigDecimal.TEN);
    var context = PositionValidationContextTestUtil.newBuilder()
        .withLicenceType(licenceType)
        .build();

    var result = operation.validate(context);

    assertThat(result).isNull();
  }

  @ParameterizedTest
  @EnumSource(
      value = LicenceType.class,
      names = {"SEAWARD_PRODUCTION", "LANDWARD_PRODUCTION"},
      mode = EnumSource.Mode.EXCLUDE
  )
  void validate_whenNotProductionLicence_thenReturnsProductionOnlyError(LicenceType licenceType) {
    var operation = new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, TRANSFER_TO, BigDecimal.TEN);
    var context = PositionValidationContextTestUtil.newBuilder()
        .withLicenceType(licenceType)
        .build();

    var result = operation.validate(context);

    var expected = PositionValidationError.forPosition(context, SubareaEquityPositionRule.PRODUCTION_LICENCE_ONLY);
    assertThat(result).isEqualTo(expected);
  }

  @Test
  void doInputsMatchPreviousStateOutputs_whenSubareaHeldOnBlock_thenTrue() {
    var operation = new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, TRANSFER_TO, BigDecimal.TEN);
    var state = LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(SUBAREA));

    var result = operation.doInputsMatchPreviousStateOutputs(state);

    assertThat(result).isTrue();
  }

  @ParameterizedTest
  @MethodSource("statesWithoutSubarea")
  void doInputsMatchPreviousStateOutputs_whenSubareaNotHeldOnBlock_thenFalse(LicencePositionState state) {
    var operation = new SubareaSetEquityOperation(BLOCK_FEATURE_ID, SUBAREA, TRANSFER_TO, BigDecimal.TEN);

    var result = operation.doInputsMatchPreviousStateOutputs(state);

    assertThat(result).isFalse();
  }

  @Test
  void serialise_whenReadBack_thenEqualsOriginal() throws Exception {
    var objectMapper = new ObjectMapper();
    var operation = new SubareaSetEquityOperation(
        BLOCK_FEATURE_ID,
        SUBAREA,
        TRANSFER_TO,
        new BigDecimal("33.3333333333")
    );

    var json = objectMapper.writeValueAsString(operation);
    var result = objectMapper.readValue(json, LicenceOperation.class);

    assertThat(result).isEqualTo(operation);
  }

  @Test
  void deserialise_whenTypeIsSubareaSetEquity_thenReadsSubareaSetEquityOperation() throws Exception {
    var json = """
        {
          "type": "subarea-set-equity",
          "blockFeatureId": "%s",
          "subarea": {"featureId": null, "name": null, "shortName": "LEG"},
          "transferTo": 123,
          "equity": 40.5
        }""".formatted(BLOCK_FEATURE_ID);

    var operation = new ObjectMapper().findAndRegisterModules().readValue(json, LicenceOperation.class);

    var expected = new SubareaSetEquityOperation(
        BLOCK_FEATURE_ID,
        new SubareaDetails(null, null, "LEG"),
        TRANSFER_TO,
        new BigDecimal("40.5")
    );
    assertThat(operation).isEqualTo(expected);
  }

  private static Stream<Arguments> statesWithoutSubarea() {
    var otherSubarea = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
    var renamedSubarea = new SubareaDetails(SUBAREA_FEATURE_ID, "Subarea A renamed", "A");
    return Stream.of(
        Arguments.of(named("block not held", LicencePositionState.EMPTY)),
        Arguments.of(named("only another subarea held",
            LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(otherSubarea)))),
        Arguments.of(named("subarea renamed",
            LicencePositionState.EMPTY.withBlock(BLOCK_FEATURE_ID, List.of(renamedSubarea))))
    );
  }
}