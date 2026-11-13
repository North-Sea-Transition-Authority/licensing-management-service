package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit;

import static uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderBlockSummarySectionService.AFTER;
import static uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderBlockSummarySectionService.AREA;
import static uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.partialsurrender.reviewandsubmit.PartialSurrenderBlockSummarySectionService.BEFORE;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionDisplay;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
public class PartialSurrenderLicenceAreaSummarySectionService implements SummarySectionService<PartialSurrenderSummaryContext> {

  static final String LICENCE_AREA = "Licence area";
  static final int SECTION_ORDER = 15;

  private final FeatureService featureService;

  PartialSurrenderLicenceAreaSummarySectionService(FeatureService featureService) {
    this.featureService = featureService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(PartialSurrenderSummaryContext context, ServiceUserDetail user) {
    var stateBefore = context.stateBefore();
    var blocksBefore = featureService.getFeaturesByIds(stateBefore.blockFeatureIds());
    if (blocksBefore.isEmpty()) {
      return Optional.empty();
    }

    var srsWkid = CoordinateSystemUtils.getWkid(blocksBefore.getFirst().getCoordinateSystem());
    var cardHeading = "Licence %s".formatted(getLicenceReference(context));
    var columns = new ArrayList<SummaryColumn>();
    columns.add(getColumn(BEFORE, cardHeading, blocksBefore, srsWkid));

    var surrender = context.surrender();
    if (surrender.takesEffect()) {
      var blocksAfter = featureService.getFeaturesByIds(surrender.applyState(stateBefore).blockFeatureIds());
      columns.add(getColumn(AFTER, cardHeading, blocksAfter, srsWkid));
    }

    var item = SummaryItem.withRows(LICENCE_AREA, List.of(new SummaryRow(columns)));
    return Optional.of(new SummarySection(SECTION_ORDER, List.of(item), SummarySectionDisplay.INLINE));
  }

  private String getLicenceReference(PartialSurrenderSummaryContext context) {
    var correction = switch (context) {
      case PartialSurrenderSummaryContext.Staged staged -> staged.licencePositionCorrection().getLicenceCorrection();
      case PartialSurrenderSummaryContext.LiveChange liveChange -> liveChange.correction();
    };
    return correction.getLicence().getLicenceReference();
  }

  private SummaryColumn getColumn(String heading, String cardHeading, List<Feature> blocks, int srsWkid) {
    var map = blocks.isEmpty() ? null : new SummaryMapView(
        blocks.stream().sorted(LicenceBlockFeatureUtil.BLOCK_ORDER).map(Feature::getId).toList(),
        srsWkid
    );
    var card = SummaryCard.simpleSummaryCardWithHeading(
        cardHeading,
        SummaryDataView.newStringKeyValue(
            AREA,
            LicenceBlockFeatureUtil.formatArea(LicenceBlockFeatureUtil.getTotalArea(blocks))
        )
    );

    return new SummaryColumn(heading, map, List.of(card));
  }
}
