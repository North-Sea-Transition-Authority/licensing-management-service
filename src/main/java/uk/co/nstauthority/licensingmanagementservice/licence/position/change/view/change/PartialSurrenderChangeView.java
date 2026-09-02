package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;

public record PartialSurrenderChangeView(
    @Nullable String surrenderDate,
    List<BlockRow> blockRows,
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  @Override
  public String type() {
    return LicenceOperation.PARTIAL_SURRENDER;
  }

  public record BlockRow(
      String blockLabel,
      @Nullable String surrenderType
  ){}
}
