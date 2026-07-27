package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = AddChange.class,
        name = LicencePositionChangeType.ADD_CHANGE
    ),
    @JsonSubTypes.Type(
        value = UpdateChangeOperations.class,
        name = LicencePositionChangeType.UPDATE_CHANGE_OPERATIONS
    )
})
public sealed interface LicencePositionChangeType permits AddChange, UpdateChangeOperations {

  String ADD_CHANGE = "add-change";
  String UPDATE_CHANGE_OPERATIONS = "update-change-operations";

  String type();

  static AddChange.Builder addChange() {
    return new AddChange.Builder();
  }

  static UpdateChangeOperations.Builder updateChangeOperations() {
    return new UpdateChangeOperations.Builder();
  }
}
