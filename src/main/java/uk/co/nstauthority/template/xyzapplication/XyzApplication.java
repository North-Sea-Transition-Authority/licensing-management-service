package uk.co.nstauthority.template.xyzapplication;

import jakarta.persistence.Id;
import java.util.UUID;
import uk.co.nstauthority.template.endpointvalidation.PathVariableEntity;
import uk.co.nstauthority.template.fds.searchselector.SearchSelectable;

@PathVariableEntity(pathVariableName = XyzApplication.XYZ_APPLICATION_ID_PARAM_NAME)
public final class XyzApplication implements SearchSelectable {

  public static final String XYZ_APPLICATION_ID_PARAM_NAME = "applicationId";

  @Id
  private UUID id;
  private String reference;
  private String type;
  private XyzApplicationStatus status;

  public XyzApplication() {
  }

  public XyzApplication(UUID id,
                        String reference,
                        String type,
                        XyzApplicationStatus status
  ) {
    this.id = id;
    this.reference = reference;
    this.type = type;
    this.status = status;
  }

  @Override
  public String getSelectionId() {
    return id.toString();
  }

  @Override
  public String getSelectionText() {
    return reference;
  }

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public String getReference() {
    return reference;
  }

  public void setReference(String reference) {
    this.reference = reference;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public XyzApplicationStatus getStatus() {
    return status;
  }

  public void setStatus(XyzApplicationStatus status) {
    this.status = status;
  }
}
