package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;

public record BlockStateView(
    String blockName,
    String area,
    SummaryMapView blocksMap,
    List<SubareaStateView> subareaStateViews
) {
}
