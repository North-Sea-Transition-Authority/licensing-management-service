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
    ),
    @JsonSubTypes.Type(
        value = RemoveChange.class,
        name = LicencePositionChangeType.REMOVE_CHANGE
    )
})
public sealed interface LicencePositionChangeType permits AddChange, UpdateChangeOperations, RemoveChange {

  String ADD_CHANGE = "add-change";
  String UPDATE_CHANGE_OPERATIONS = "update-change-operations";
  String REMOVE_CHANGE = "remove-change";

  String type();

  String changeId();

  static AddChange.Builder addChange() {
    return new AddChange.Builder();
  }

  static UpdateChangeOperations.Builder updateChangeOperations() {
    return new UpdateChangeOperations.Builder();
  }

  static RemoveChange.Builder removeChange() {
    return new RemoveChange.Builder();
  }
}
