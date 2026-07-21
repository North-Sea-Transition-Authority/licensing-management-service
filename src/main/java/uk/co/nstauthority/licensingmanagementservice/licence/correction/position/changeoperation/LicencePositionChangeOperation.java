package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = LicencePositionAddOperation.class,
        name = LicencePositionChangeOperation.ADD_OPERATION
    )
})
public sealed interface LicencePositionChangeOperation permits LicencePositionAddOperation {

  String ADD_OPERATION = "add-operation";

  String type();

  Integer operationId();

  static LicencePositionAddOperation.Builder newLicencePositionAddOperation() {
    return new LicencePositionAddOperation.Builder();
  }

}
