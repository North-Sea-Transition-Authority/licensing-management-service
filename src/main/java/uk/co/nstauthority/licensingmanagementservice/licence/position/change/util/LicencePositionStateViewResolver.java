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
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;

public final class LicencePositionStateViewResolver {

  private LicencePositionStateViewResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  /** Names every organisation in the position's state as it was known on the position date, or today when null. */
  public static LicencePositionStateView getStateView(
      UUID currentLicencePositionId,
      ResolvedStates resolvedStates,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    var currentState = resolvedStates.currentState(currentLicencePositionId);

    return new LicencePositionStateView(
        new AdministratorStateView(nameContext.getNameForDate(currentState.administratorId(), positionDate, "")),
        buildBeneficialInterests(currentState.equityByOrganisationId(), nameContext, positionDate),
        buildOrganisationNameHistories(currentState, nameContext, positionDate)
    );
  }

  private static List<BeneficialInterestView> buildBeneficialInterests(
      Map<Integer, BigDecimal> equityByOrganisationId,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    return equityByOrganisationId.entrySet().stream()
        .map(entry -> new BeneficialInterestView(
            nameContext.getNameForDate(entry.getKey(), positionDate, NOT_AVAILABLE),
            entry.getValue()
        ))
        .sorted(Comparator.comparing(BeneficialInterestView::organisationName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  /**
   * The name history of the licence administrator and each beneficial interest holder. Organisations that were never
   * renamed either side of the position date are left out, so the section stays empty when there is nothing to tell.
   */
  private static List<OrganisationNameHistoryView> buildOrganisationNameHistories(
      LicencePositionState currentState,
      OrganisationNameContext nameContext,
      @Nullable LocalDate positionDate
  ) {
    var distinctOrganisationIds = new HashSet<Integer>();
    Optional.ofNullable(currentState.administratorId()).ifPresent(distinctOrganisationIds::add);
    distinctOrganisationIds.addAll(currentState.equityByOrganisationId().keySet());

    return distinctOrganisationIds.stream()
        .map(organisationId -> nameContext.getNameHistoryForDate(organisationId, positionDate))
        .filter(OrganisationNameHistoryView::hasNames)
        .sorted()
        .toList();
  }
}
