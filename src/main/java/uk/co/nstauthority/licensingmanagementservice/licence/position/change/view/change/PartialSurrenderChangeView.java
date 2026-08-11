package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change;

import jakarta.annotation.Nullable;
import java.util.List;

public record PartialSurrenderChangeView(
    @Nullable String surrenderDate,
    List<String> blockLabels,
    @Nullable String changeType
) implements LicencePositionChangeView {
}
