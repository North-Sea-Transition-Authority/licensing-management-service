package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationContext;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.validation.PositionValidationError;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = AdministratorOperation.class,
        name = LicenceOperation.LICENCE_ADMINISTRATOR
    ),
    @JsonSubTypes.Type(
        value = SetEquityOperation.class,
        name = LicenceOperation.SET_EQUITY
    ),
    @JsonSubTypes.Type(
        value = TransferEquityOperation.class,
        name = LicenceOperation.TRANSFER_EQUITY
    ),
    @JsonSubTypes.Type(
        value = PartialSurrenderOperation.class,
        name = LicenceOperation.PARTIAL_SURRENDER
    ),
    @JsonSubTypes.Type(
        value = SubareaOperation.class,
        name = LicenceOperation.SUBAREA
    ),
    @JsonSubTypes.Type(
        value = LicenseeOperation.class,
        name = LicenceOperation.LICENSEE
    ),
    @JsonSubTypes.Type(
        value = SubareaCreateOperation.class,
        name = LicenceOperation.SUBAREA_CREATE
    ),
    @JsonSubTypes.Type(
        value = SubareaEndOperation.class,
        name = LicenceOperation.SUBAREA_END
    ),
    @JsonSubTypes.Type(
        value = BlockCreateOperation.class,
        name = LicenceOperation.BLOCK_CREATE
    ),
    @JsonSubTypes.Type(
        value = BlockRedefinitionOperation.class,
        name = LicenceOperation.BLOCK_REDEFINITION
    ),
    @JsonSubTypes.Type(
        value = BlockEndOperation.class,
        name = LicenceOperation.BLOCK_END
    )
})
public sealed interface LicenceOperation permits HiddenLicenceOperation, VisibleLicenceOperation {

  String LICENCE_ADMINISTRATOR = "licence-administrator";
  String SET_EQUITY = "set-equity";
  String TRANSFER_EQUITY = "transfer-equity";
  String PARTIAL_SURRENDER = "partial-surrender";
  String SUBAREA = "subarea";
  String LICENSEE = "licensee";
  String SUBAREA_CREATE = "subarea-create";
  String SUBAREA_END = "subarea-end";
  String BLOCK_CREATE = "block-create";
  String BLOCK_REDEFINITION = "block-redefinition";
  String BLOCK_END = "block-end";

  String type();

  String displayName();

  UUID id();

  @Nullable
  PositionValidationError validate(PositionValidationContext positionValidationContext);

  default Set<Integer> organisationUnitIds() {
    return Set.of();
  }

  static AdministratorOperation.Builder newAdministratorChange() {
    return new AdministratorOperation.Builder();
  }

  static SetEquityOperation.Builder newSetEquityOperation() {
    return new SetEquityOperation.Builder();
  }

  static TransferEquityOperation.Builder newTransferEquityOperation() {
    return new TransferEquityOperation.Builder();
  }

  static PartialSurrenderOperation.Builder newPartialSurrenderOperation() {
    return new PartialSurrenderOperation.Builder();
  }

  static SubareaOperation.Builder newSubAreaOperation() {
    return new SubareaOperation.Builder();
  }

  static LicenseeOperation.Builder newLicenseeOperation() {
    return new LicenseeOperation.Builder();
  }

  static SubareaCreateOperation.Builder newSubareaCreateOperation() {
    return new SubareaCreateOperation.Builder();
  }

  static SubareaEndOperation.Builder newSubareaEndOperation() {
    return new SubareaEndOperation.Builder();
  }

  static BlockCreateOperation.Builder newBlockCreateOperation() {
    return new BlockCreateOperation.Builder();
  }

  static BlockRedefinitionOperation.Builder newBlockRedefinitionOperation() {
    return new BlockRedefinitionOperation.Builder();
  }

  static BlockEndOperation.Builder newBlockEndOperation() {
    return new BlockEndOperation.Builder();
  }
}
