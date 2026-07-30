package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changeoperation;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = LicencePositionAddOperation.class,
        name = LicencePositionChangeOperation.ADD_OPERATION
    ),
    @JsonSubTypes.Type(
        value = LicencePositionUpdateOperation.class,
        name = LicencePositionChangeOperation.UPDATE_OPERATION
    )
})
public sealed interface LicencePositionChangeOperation permits LicencePositionAddOperation, LicencePositionUpdateOperation {

  String ADD_OPERATION = "add-operation";
  String UPDATE_OPERATION = "update-operation";

  String type();

  UUID operationId();

  static LicencePositionAddOperation.Builder newLicencePositionAddOperation() {
    return new LicencePositionAddOperation.Builder();
  }

  static LicencePositionUpdateOperation.Builder newLicencePositionUpdateOperation() {
    return new LicencePositionUpdateOperation.Builder();
  }

}