package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.SubareaBeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureSummary;

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
        buildBeneficialInterests(
            currentState.equityByOrganisationId(),
            currentState.licenseeIds(),
            nameContext,
            positionDate
        ),
        buildOrganisationNameHistories(currentState, nameContext, positionDate),
        buildSubareaBeneficialInterests(
            currentState.blockFeatureIdToSubareaEquity(),
            currentState.licenseeIds(),
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

  private static List<SubareaBeneficialInterestView> buildSubareaBeneficialInterests(
      Map<UUID, Map<SubareaDetails, Map<Integer, BigDecimal>>> blockFeatureIdToSubareaEquity,
      Set<Integer> licenseeIds,
      Map<UUID, FeatureSummary> featuresById,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    return blockFeatureIdToSubareaEquity.entrySet().stream()
        .flatMap(blockEntry -> blockEntry.getValue().entrySet().stream()
            .map(subareaEntry -> {
              var blockName = Optional.ofNullable(featuresById.get(blockEntry.getKey()))
                  .map(FeatureSummary::name)
                  .orElse(null);
              var subareaLabel = subareaEntry.getKey().labelForBlockName(blockName);
              var beneficialInterestViews =
                  buildBeneficialInterests(subareaEntry.getValue(), licenseeIds, nameContext, positionDate);
              return new SubareaBeneficialInterestView(subareaLabel, beneficialInterestViews);
            })
        )
        .sorted(Comparator.comparing(SubareaBeneficialInterestView::subareaLabel, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  /**
   * The name history of the licence administrator/storage operator, licensees and each beneficial interest holder.
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
    distinctOrganisationIds.addAll(currentState.licenseeIds());
    distinctOrganisationIds.addAll(currentState.beneficialInterestHolderIds());

    return distinctOrganisationIds.stream()
        .map(organisationId -> nameContext.getNameHistoryForDate(organisationId, positionDate))
        .filter(OrganisationNameHistoryView::hasNames)
        .sorted()
        .toList();
  }
}
