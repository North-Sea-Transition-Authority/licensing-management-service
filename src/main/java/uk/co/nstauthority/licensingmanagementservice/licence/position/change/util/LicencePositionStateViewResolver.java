package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BlockStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.SubareaStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureSummary;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.LicenceBlockFeatureUtil;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryMapView;

public final class LicencePositionStateViewResolver {

  private LicencePositionStateViewResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static LicencePositionStateView getStateView(
      UUID currentLicencePositionId,
      LicenceType licenceType,
      ResolvedStates resolvedStates,
      OrganisationNameContext nameContext,
      Map<UUID, FeatureSummary> featuresById,
      @Nullable LocalDate positionDate
  ) {
    var currentState = resolvedStates.currentState(currentLicencePositionId);

    return new LicencePositionStateView(
        new AdministratorStateView(nameContext.getNameForDate(currentState.administratorId(), positionDate, "")),
        new LicenseeStateView(buildLicensees(currentState, licenceType, nameContext, positionDate)),
        nameContext.getNameForDate(currentState.storageOperatorId(), positionDate, ""),
        nameContext.getNameForDate(currentState.explorationOperatorId(), positionDate, ""),
        buildBeneficialInterests(
            currentState.equityByOrganisationId(),
            currentState.licenseeIds(),
            nameContext,
            positionDate
        ),
        buildOrganisationNameHistories(currentState, nameContext, positionDate),
        buildBlocks(
            currentState,
            featuresById,
            nameContext,
            positionDate
        )
    );
  }

  private static List<LicenseeView> buildLicensees(
      LicencePositionState currentState,
      LicenceType licenceType,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    var beneficialInterestHolderIds = currentState.beneficialInterestHolderIds();
    return currentState.licenseeIds()
        .stream()
        .map(licenseeId -> new LicenseeView(
            nameContext.getNameForDate(licenseeId, positionDate, NOT_AVAILABLE),
            getLicenseeMarker(licenseeId, licenceType, beneficialInterestHolderIds)
        ))
        .sorted(Comparator.comparing(LicenseeView::organisationName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  @Nullable
  private static OrganisationMarker getLicenseeMarker(
      Integer licenseeId,
      LicenceType licenceType,
      Set<Integer> beneficialInterestHolderIds
  ) {
    if (licenceType.hasBeneficialInterests() && !beneficialInterestHolderIds.contains(licenseeId)) {
      return OrganisationMarker.NO_BENEFICIAL_INTEREST;
    }
    return null;
  }

  private static List<BeneficialInterestView> buildBeneficialInterests(
      Map<Integer, BigDecimal> equityByOrganisationId,
      Set<Integer> licenseeIds,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    return equityByOrganisationId.entrySet().stream()
        .map(entry -> new BeneficialInterestView(
            nameContext.getNameForDate(entry.getKey(), positionDate, NOT_AVAILABLE),
            entry.getValue(),
            licenseeIds.contains(entry.getKey()) ? null : OrganisationMarker.NOT_A_LICENSEE
        ))
        .sorted(Comparator.comparing(BeneficialInterestView::organisationName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  private static List<BlockStateView> buildBlocks(
      LicencePositionState currentState,
      Map<UUID, FeatureSummary> featuresById,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    var blockFeatureIds = currentState.blockFeatureIds().stream()
        .filter(featuresById::containsKey)
        .sorted(Comparator.comparing(featuresById::get, FeatureSummary.BLOCK_ORDER))
        .toList();

    return blockFeatureIds.stream()
        .map(blockId -> {
          var block = featuresById.get(blockId);
          return new BlockStateView(
              "Block %s".formatted(block.name()),
              LicenceBlockFeatureUtil.formatArea(block.area()),
              new SummaryMapView(blockFeatureIds, block.srsWkid(), blockId),
              buildSubareas(currentState, blockId, featuresById, nameContext, positionDate)
          );
        })
        .toList();
  }

  private static List<SubareaStateView> buildSubareas(
      LicencePositionState currentState,
      UUID blockId,
      Map<UUID, FeatureSummary> featuresById,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    var subareas = currentState.subareasOf(blockId);
    var subareaFeatureIds = subareas.stream()
        .map(SubareaDetails::featureId)
        .filter(Objects::nonNull)
        .toList();

    return subareas.stream()
        .sorted(Comparator.comparing(SubareaDetails::shortName))
        .map(subarea -> {
          var feature = Optional.ofNullable(subarea.featureId()).map(featuresById::get);
          return new SubareaStateView(
              subarea.displayName(),
              feature.map(summary -> LicenceBlockFeatureUtil.formatArea(summary.area())).orElse(null),
              feature.map(summary -> new SummaryMapView(subareaFeatureIds, summary.srsWkid(), subarea.featureId())).orElse(null),
              buildBeneficialInterests(
                  currentState.subareaEquityOf(blockId, subarea),
                  currentState.licenseeIds(),
                  nameContext,
                  positionDate
              )
          );
        })
        .toList();
  }

  /**
   * The name history of the licence administrator/storage operator and exploration operator,
   * licensees and each beneficial interest holder.
   * Organisations that were never renamed either side of the position date are left out,
   * so the section stays empty when there is nothing to tell.
   */
  private static List<OrganisationNameHistoryView> buildOrganisationNameHistories(
      LicencePositionState currentState,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    var distinctOrganisationIds = new HashSet<Integer>();
    Optional.ofNullable(currentState.administratorId()).ifPresent(distinctOrganisationIds::add);
    Optional.ofNullable(currentState.storageOperatorId()).ifPresent(distinctOrganisationIds::add);
    Optional.ofNullable(currentState.explorationOperatorId()).ifPresent(distinctOrganisationIds::add);
    distinctOrganisationIds.addAll(currentState.licenseeIds());
    distinctOrganisationIds.addAll(currentState.beneficialInterestHolderIds());

    return distinctOrganisationIds.stream()
        .map(organisationId -> nameContext.getNameHistoryForDate(organisationId, positionDate))
        .filter(OrganisationNameHistoryView::hasNames)
        .sorted()
        .toList();
  }
}
