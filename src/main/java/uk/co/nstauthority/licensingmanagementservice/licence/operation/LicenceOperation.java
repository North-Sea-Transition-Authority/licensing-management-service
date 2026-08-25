package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import jakarta.annotation.Nullable;
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
    )
})
public sealed interface LicenceOperation permits
    AdministratorOperation,
    SetEquityOperation,
    TransferEquityOperation,
    PartialSurrenderOperation {

  String LICENCE_ADMINISTRATOR = "licence-administrator";
  String SET_EQUITY = "set-equity";
  String TRANSFER_EQUITY = "transfer-equity";
  String PARTIAL_SURRENDER = "partial-surrender";

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

  static boolean isEquityOperation(LicenceOperation operation) {
    return operation instanceof SetEquityOperation || operation instanceof TransferEquityOperation;
  }
}
