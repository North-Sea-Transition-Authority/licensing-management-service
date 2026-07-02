package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = AddLicencePositionChange.class,
        name = LicencePositionChangeType.ADD_CHANGE
    )
})
public sealed interface LicencePositionChangeType permits AddLicencePositionChange {

  String ADD_CHANGE = "add-change";

  String type();

  static AddLicencePositionChange.Builder addLicencePositionChange() {
    return new AddLicencePositionChange.Builder();
  }
}

