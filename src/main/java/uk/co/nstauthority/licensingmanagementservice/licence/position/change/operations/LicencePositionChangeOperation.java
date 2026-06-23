package uk.co.nstauthority.licensingmanagementservice.licence.position.change.operations;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = LicencePositionAdministratorChange.class,
        name = LicencePositionChangeOperation.LICENCE_ADMINISTRATOR
    )
})
public sealed interface LicencePositionChangeOperation permits LicencePositionAdministratorChange {

  String LICENCE_ADMINISTRATOR = "licence-administrator";

  String type();

  static LicencePositionAdministratorChange.Builder newAdministratorChange() {
    return new LicencePositionAdministratorChange.Builder();
  }
}
