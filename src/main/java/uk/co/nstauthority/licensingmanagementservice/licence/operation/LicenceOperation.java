package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
import java.util.List;
import java.util.UUID;
import java.util.stream.Stream;
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
public sealed interface LicenceOperation permits
    AdministratorOperation,
    SetEquityOperation,
    TransferEquityOperation,
    PartialSurrenderOperation,
    SubareaOperation,
    LicenseeOperation,
    BlockCreateOperation,
    BlockRedefinitionOperation,
    BlockEndOperation {

  String LICENCE_ADMINISTRATOR = "licence-administrator";
  String SET_EQUITY = "set-equity";
  String TRANSFER_EQUITY = "transfer-equity";
  String PARTIAL_SURRENDER = "partial-surrender";
  String SUBAREA = "subarea";
  String LICENSEE = "licensee";
  String BLOCK_CREATE = "block-create";
  String BLOCK_REDEFINITION = "block-redefinition";
  String BLOCK_END = "block-end";

  String type();

  String displayName();

  UUID id();

  @Nullable
  PositionValidationError validate(PositionValidationContext positionValidationContext);

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

  static BlockCreateOperation.Builder newBlockCreateOperation() {
    return new BlockCreateOperation.Builder();
  }

  static BlockRedefinitionOperation.Builder newBlockRedefinitionOperation() {
    return new BlockRedefinitionOperation.Builder();
  }

  static BlockEndOperation.Builder newBlockEndOperation() {
    return new BlockEndOperation.Builder();
  }

  static boolean isEquityOperation(LicenceOperation operation) {
    return operation instanceof SetEquityOperation || operation instanceof TransferEquityOperation;
  }

  static List<UUID> featureIds(LicenceOperation operation) {
    return switch (operation) {
      case PartialSurrenderOperation partialSurrender -> partialSurrender.surrenderedFeatureIds();
      case SubareaOperation subarea -> List.of(subarea.featureId());
      case BlockCreateOperation blockCreate -> blockCreate.featureIds();
      case BlockRedefinitionOperation blockRedefinition ->
          Stream.concat(
                  blockRedefinition.replacedFeatureIds().stream(),
                  blockRedefinition.outputFeatureIds().stream())
              .distinct()
              .toList();
      case BlockEndOperation blockEnd -> blockEnd.endedFeatureIds();
      case AdministratorOperation ignored -> List.of();
      case SetEquityOperation ignored -> List.of();
      case TransferEquityOperation ignored -> List.of();
      case LicenseeOperation ignored -> List.of();
    };
  }

  static List<Integer> organisationIds(LicenceOperation operation) {
    return switch (operation) {
      case AdministratorOperation administratorOperation -> List.of(administratorOperation.operatorId());
      case SetEquityOperation setEquityOperation -> List.of(setEquityOperation.transferTo());
      case TransferEquityOperation transfer -> List.of(transfer.transferFrom(), transfer.transferTo());
      case PartialSurrenderOperation ignored -> List.of();
      case SubareaOperation ignored -> List.of();
      case LicenseeOperation licenseeOperation -> Stream
          .concat(licenseeOperation.licenseesToAdd().stream(), licenseeOperation.licenseesToRemove().stream())
          .toList();
      case BlockCreateOperation ignored -> List.of();
      case BlockRedefinitionOperation ignored -> List.of();
      case BlockEndOperation ignored -> List.of();
    };
  }
}
