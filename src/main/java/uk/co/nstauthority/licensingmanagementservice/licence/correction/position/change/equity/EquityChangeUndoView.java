package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.equity;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.SetEquityRow;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.change.TransferEquityHoldingView;

public record EquityChangeUndoView(
    List<SetEquityRow> setEquityRows,
    List<TransferEquityHoldingView> transferEquityRows
) {
}