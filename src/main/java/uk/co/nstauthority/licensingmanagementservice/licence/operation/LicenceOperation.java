package uk.co.nstauthority.licensingmanagementservice.licence.operation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;

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
    )
})
public sealed interface LicenceOperation permits
    AdministratorOperation,
    SetEquityOperation {

  String LICENCE_ADMINISTRATOR = "licence-administrator";
  String SET_EQUITY = "set-equity";

  String type();

  UUID id();

  static AdministratorOperation.Builder newAdministratorChange() {
    return new AdministratorOperation.Builder();
  }

  static SetEquityOperation.Builder newSetEquityOperation() {
    return new SetEquityOperation.Builder();
  }
}
