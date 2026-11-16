package uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state;

import jakarta.annotation.Nullable;
import java.util.List;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;

public record SubareaStateView(
    String subareaName,
    @Nullable String area,
    @Nullable SummaryMapView subareasMap,
    List<BeneficialInterestView> beneficialInterestViews
) {
}
