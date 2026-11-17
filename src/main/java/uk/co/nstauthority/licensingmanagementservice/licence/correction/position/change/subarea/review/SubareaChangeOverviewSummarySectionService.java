package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.SubareaSummaryUtil.toMapSummaryView;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionDisplay;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
class SubareaChangeOverviewSummarySectionService implements SummarySectionService<SubareaChangeSummaryContext> {

  static final String BEFORE = "Before";
  static final String AFTER = "After";
  static final int SECTION_ORDER = 10;

  private final FeatureService featureService;

  SubareaChangeOverviewSummarySectionService(FeatureService featureService) {
    this.featureService = featureService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      SubareaChangeSummaryContext context,
      ServiceUserDetail user
  ) {
    var block = featureService.getFeatureOrThrow(context.blockFeatureId());
    var srsWkid = CoordinateSystemUtils.getWkid(block.getCoordinateSystem());
    var subareasAfter = Stream.concat(
            context.unchangedSubareas().stream(),
            context.transfers().stream().flatMap(transfer -> transfer.outputSubareas().stream())
        )
        .distinct()
        .toList();

    var overview = new SummaryRow(List.of(
        new SummaryColumn(BEFORE, toMapSummaryView(context.originalSubareas(), srsWkid), List.of()),
        new SummaryColumn(AFTER, toMapSummaryView(subareasAfter, srsWkid), List.of())
    ));
    var item = SummaryItem.withRows(null, List.of(overview));
    return Optional.of(new SummarySection(SECTION_ORDER, List.of(item), SummarySectionDisplay.INLINE));
  }
}
