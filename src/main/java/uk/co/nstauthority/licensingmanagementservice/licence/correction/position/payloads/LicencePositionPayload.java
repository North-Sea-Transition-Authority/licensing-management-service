package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = CreateLicencePositionPayload.class,
        name = CreateLicencePositionPayload.ADD_POSITION
    )
})
public sealed interface LicencePositionPayload permits CreateLicencePositionPayload {

  String ADD_POSITION = "add-position";

  String type();

  static CreateLicencePositionPayload.Builder newCreateLicencePositionPayload() {
    return new CreateLicencePositionPayload.Builder();
  }
}