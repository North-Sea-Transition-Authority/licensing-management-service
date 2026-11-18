package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record SubareaChangeView(
    String blockName,
    List<SubareaRow> subareaRows,
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return LicenceOperation.SUBAREA;
  }

  public record SubareaRow(String inputSubarea, Tag tag, String outputSubareas) {
  }
}
