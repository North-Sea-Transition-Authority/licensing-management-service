package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review;

import static uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeOverviewSummarySectionService.AFTER;
import static uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subarea.review.SubareaChangeOverviewSummarySectionService.BEFORE;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.SubareaSummaryUtil.subareaCard;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.SubareaSummaryUtil.subareaDetails;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.SubareaSummaryUtil.subareaHeading;
import static uk.co.nstauthority.licensingmanagementservice.licence.position.feature.SubareaSummaryUtil.toMapSummaryView;

import com.google.common.collect.Comparators;
import com.google.common.collect.Sets;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.Tag;
import uk.co.nstauthority.licensingmanagementservice.fds.tag.TagColour;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.subareaequity.SubareaSetEquityCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaTransfer;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCard;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryCardAction;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryColumn;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryRow;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySection;
import uk.co.nstauthority.licensingmanagementservice.summary.SummarySectionService;

@Service
class SubareaChangesSummarySectionService implements SummarySectionService<SubareaChangeSummaryContext> {

  static final String BENEFICIAL_INTERESTS = "Beneficial interests";
  static final String CHANGE = "Change";
  static final String NOT_ASSIGNED = "Not assigned";
  static final String NO_SUBAREAS_REPLACED = "This change did not replace any subareas.";
  static final String SUBAREAS_REMOVED = "This change removed these subareas.";
  static final Tag LINEAGE_LOST = new Tag("Lineage lost", TagColour.RED);
  static final int SECTION_ORDER = 20;

  private static final Comparator<SubareaDetails> SUBAREA_ORDER =
      Comparator.comparing(SubareaDetails::shortName, String.CASE_INSENSITIVE_ORDER);

  private static final Comparator<Iterable<String>> HEADING_NAMES_ORDER =
      Comparators.lexicographical(String.CASE_INSENSITIVE_ORDER);

  private final SubareaSetEquityCorrectionService subareaSetEquityCorrectionService;
  private final FeatureService featureService;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  SubareaChangesSummarySectionService(
      SubareaSetEquityCorrectionService subareaSetEquityCorrectionService,
      FeatureService featureService,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.subareaSetEquityCorrectionService = subareaSetEquityCorrectionService;
    this.featureService = featureService;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  @Override
  public Optional<SummarySection> getSummarySection(
      SubareaChangeSummaryContext context,
      ServiceUserDetail user
  ) {
    var lineageLostTransfers = context instanceof SubareaChangeSummaryContext.Staged
        ? context.transfers().stream()
            .filter(transfer -> subareaSetEquityCorrectionService
                .isLineageLost(context.stateBefore(), context.blockFeatureId(), transfer.replacedSubareas()))
            .collect(Collectors.toSet())
        : Set.<SubareaTransfer>of();
    var transfers = Stream.concat(
        context.transfers().stream(),
        context.unchangedSubareas().stream().map(subarea -> new SubareaTransfer(List.of(subarea), List.of(subarea)))
    ).toList();

    var block = featureService.getFeatureOrThrow(context.blockFeatureId());
    var srsWkid = CoordinateSystemUtils.getWkid(block.getCoordinateSystem());
    var subareas = transfers.stream()
        .flatMap(transfer -> Stream.concat(transfer.replacedSubareas().stream(), transfer.outputSubareas().stream()))
        .toList();
    var featureIdToFeature = featureService.getFeaturesByIds(SubareaDetails.featureIds(subareas)).stream()
        .collect(Collectors.toMap(Feature::getId, Function.identity()));
    var organisationIdToName = organisationUnitQueryService.getOrganisationUnitNamesByIds(
        getBeneficialInterestHolderIds(context)
    );

    // TODO LMS2-367: add the subarea's operator once it is recorded against subareas
    var items = transfers.stream()
        .sorted(Comparator.comparing(SubareaChangesSummarySectionService::getHeadingNames, HEADING_NAMES_ORDER))
        .map(transfer -> {
          var beforeSubareas = sorted(transfer.replacedSubareas());
          var afterSubareas = sorted(transfer.outputSubareas());
          var beforeCards = beforeSubareas.stream()
              .map(subarea -> subareaCard(
                  subarea,
                  context.stateBefore().subareaEquityOf(context.blockFeatureId(), subarea),
                  BENEFICIAL_INTERESTS,
                  featureIdToFeature,
                  organisationIdToName
              ))
              .toList();
          var tags = getTags(context, transfer, lineageLostTransfers);
          var afterCards = afterSubareas.stream()
              .map(subarea -> {
                var equityAfter = getEquityAfter(context, transfer, subarea, lineageLostTransfers);
                var details = subareaDetails(
                    subarea,
                    equityAfter,
                    BENEFICIAL_INTERESTS,
                    featureIdToFeature,
                    organisationIdToName
                );
                if (tags.contains(LINEAGE_LOST) && equityAfter.isEmpty()) {
                  details.addStringValue(BENEFICIAL_INTERESTS, NOT_ASSIGNED);
                }
                return SummaryCard.simpleSummaryCardWithHeading(subareaHeading(subarea), details.build())
                    .withAction(SummaryCardAction.post(CHANGE, context.changeSubareasUrl()));
              })
              .toList();

          return SummaryItem.withRows(
              getHeading(getHeadingNames(transfer)),
              List.of(new SummaryRow(List.of(
                  new SummaryColumn(
                      BEFORE,
                      toMapSummaryView(beforeSubareas, srsWkid),
                      beforeCards,
                      beforeSubareas.isEmpty() ? NO_SUBAREAS_REPLACED : null
                  ),
                  new SummaryColumn(
                      AFTER,
                      toMapSummaryView(afterSubareas, srsWkid),
                      afterCards,
                      afterSubareas.isEmpty() ? SUBAREAS_REMOVED : null
                  )
              )))
          ).withTags(tags);
        })
        .toList();

    return Optional.of(new SummarySection(SECTION_ORDER, items));
  }

  private static List<Tag> getTags(
      SubareaChangeSummaryContext context,
      SubareaTransfer transfer,
      Set<SubareaTransfer> lineageLostTransfers
  ) {
    if (isUnchanged(transfer)) {
      return List.of(SubareaChangeType.UNCHANGED.toTag());
    }

    return switch (context) {
      case SubareaChangeSummaryContext.Staged staged -> {
        var type = getStagedChangeType(transfer, staged.transfers());
        yield type == SubareaChangeType.MERGE && lineageLostTransfers.contains(transfer)
            ? List.of(type.toTag(), LINEAGE_LOST)
            : List.of(type.toTag());
      }
      case SubareaChangeSummaryContext.LiveChange ignored -> List.of(getLiveChangeType(transfer).toTag());
    };
  }

  private static Map<Integer, BigDecimal> getEquityAfter(
      SubareaChangeSummaryContext context,
      SubareaTransfer transfer,
      SubareaDetails outputSubarea,
      Set<SubareaTransfer> lineageLostTransfers
  ) {
    var blockFeatureId = context.blockFeatureId();

    if (isUnchanged(transfer)) {
      return context.stateBefore().subareaEquityOf(blockFeatureId, outputSubarea);
    }

    return switch (context) {
      case SubareaChangeSummaryContext.Staged staged -> {
        var stagedEquity = staged.stateBefore().subareaEquityOf(blockFeatureId, outputSubarea);
        if (!stagedEquity.isEmpty() || lineageLostTransfers.contains(transfer)) {
          yield stagedEquity;
        }
        yield staged.stateBefore().subareaEquityOf(blockFeatureId, transfer.replacedSubareas().getFirst());
      }
      case SubareaChangeSummaryContext.LiveChange liveChange ->
          liveChange.stateAtEnd().subareaEquityOf(blockFeatureId, outputSubarea);
    };
  }

  private static SubareaChangeType getStagedChangeType(
      SubareaTransfer transfer,
      List<SubareaTransfer> transfers
  ) {
    var replacedSubareas = transfer.replacedSubareas();

    if (replacedSubareas.isEmpty()) {
      return SubareaChangeType.SET;
    }

    if (replacedSubareas.size() > 1) {
      return SubareaChangeType.MERGE;
    }

    var isUsedByAnotherTransfer = transfers.stream()
        .filter(otherTransfer -> otherTransfer != transfer)
        .anyMatch(otherTransfer -> otherTransfer.replacedSubareas().contains(replacedSubareas.getFirst()));

    return transfer.outputSubareas().size() > 1 || isUsedByAnotherTransfer
        ? SubareaChangeType.SPLIT
        : SubareaChangeType.RENAME;
  }

  private static SubareaChangeType getLiveChangeType(SubareaTransfer transfer) {
    if (transfer.replacedSubareas().isEmpty()) {
      return SubareaChangeType.SET;
    }

    if (transfer.outputSubareas().isEmpty()) {
      return SubareaChangeType.REMOVE;
    }

    return SubareaChangeType.TRANSFER;
  }

  private static boolean isUnchanged(SubareaTransfer transfer) {
    return !transfer.replacedSubareas().isEmpty() && transfer.replacedSubareas().equals(transfer.outputSubareas());
  }

  private static Set<Integer> getBeneficialInterestHolderIds(SubareaChangeSummaryContext context) {
    return switch (context) {
      case SubareaChangeSummaryContext.Staged staged -> staged.stateBefore().beneficialInterestHolderIds();
      case SubareaChangeSummaryContext.LiveChange liveChange -> Sets.union(
          liveChange.stateBefore().beneficialInterestHolderIds(),
          liveChange.stateAtEnd().beneficialInterestHolderIds()
      );
    };
  }

  private static List<SubareaDetails> sorted(List<SubareaDetails> subareas) {
    return subareas.stream()
        .sorted(SUBAREA_ORDER)
        .toList();
  }

  private static List<String> getHeadingNames(SubareaTransfer transfer) {
    var subareas = transfer.replacedSubareas().isEmpty() ? transfer.outputSubareas() : transfer.replacedSubareas();
    return subareas.stream()
        .map(SubareaDetails::shortName)
        .distinct()
        .sorted(String.CASE_INSENSITIVE_ORDER)
        .toList();
  }

  private static String getHeading(List<String> names) {
    if (names.size() == 1) {
      return "Subarea %s".formatted(names.getFirst());
    }

    return "Subareas %s and %s".formatted(
        String.join(", ", names.subList(0, names.size() - 1)),
        names.getLast()
    );
  }
}
