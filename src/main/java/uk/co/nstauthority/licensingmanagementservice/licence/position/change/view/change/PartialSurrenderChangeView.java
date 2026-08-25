package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.List;

public record PartialSurrenderChangeView(
    @Nullable String surrenderDate,
    List<BlockRow> blockRows,
    @Nullable String changeType,
    ChangeViewUrls urls
) implements LicencePositionChangeView {

  public record BlockRow(
      String blockLabel,
      @Nullable String surrenderType
  ){}
}
