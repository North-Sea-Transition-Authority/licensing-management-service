package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import jakarta.annotation.Nullable;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;

/**
 * The equity rows of a single position change, for showing on the remove/undo confirmation page.
 *
 * @param subareaLabel the subarea the equity was set on, or null when the change is not a subarea equity change
 *                     (it sets equity across the whole licence) or the subarea's block can no longer be found.
 */
public record EquityChangeContext(
    List<SetEquityRow> setEquityRows,
    List<TransferEquityHoldingView> transferEquityRows,
    @Nullable String subareaLabel
) {
}