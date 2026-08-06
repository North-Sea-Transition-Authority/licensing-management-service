package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.payloads;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.changetypes.LicencePositionChangeType;

@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(
        value = CreateLicencePositionPayload.class,
        name = LicencePositionPayload.ADD_POSITION
    ),
    @JsonSubTypes.Type(
        value = UpdateLicencePositionPayload.class,
        name = LicencePositionPayload.UPDATE_POSITION
    )
})
public sealed interface LicencePositionPayload permits CreateLicencePositionPayload, UpdateLicencePositionPayload {

  String ADD_POSITION = "add-position";
  String UPDATE_POSITION = "update-position";

  String type();

  List<LicencePositionChangeType> changes();

  static CreateLicencePositionPayload.Builder newCreateLicencePositionPayload() {
    return new CreateLicencePositionPayload.Builder();
  }

  static UpdateLicencePositionPayload.Builder newUpdateLicencePositionPayload() {
    return new UpdateLicencePositionPayload.Builder();
  }

  static LicencePositionPayload withChanges(LicencePositionPayload payload, List<LicencePositionChangeType> changes) {
    return switch (payload) {
      case CreateLicencePositionPayload create -> newCreateLicencePositionPayload()
          .withLicencePositionId(create.licencePositionId())
          .withLicenceTransactionId(create.licenceTransactionId())
          .withEffectiveDate(create.effectiveDate())
          .withEffectiveDateOrder(create.effectiveDateOrder())
          .withCorrectionReference(create.correctionReference())
          .withChanges(changes)
          .build();
      case UpdateLicencePositionPayload update -> newUpdateLicencePositionPayload()
          .withEffectiveDate(update.effectiveDate())
          .withEffectiveDateOrder(update.effectiveDateOrder())
          .withCorrectionReference(update.correctionReference())
          .withChanges(changes)
          .build();
    };
  }
}