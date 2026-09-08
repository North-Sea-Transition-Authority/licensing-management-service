package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static uk.co.nstauthority.licensingmanagementservice.licence.position.change.util.LicencePositionChangeUtil.NOT_AVAILABLE;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Stream;
import uk.co.fivium.energyportalapi.generated.types.OrganisationNameHistory;
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

  public static LicencePositionStateView getStateView(
      UUID currentLicencePositionId,
      ResolvedStates resolvedStates,
      Map<Integer, String> organisationNames,
      Map<Integer, List<OrganisationNameHistory>> organisationNameHistories,
      LocalDate positionDate
  ) {
    var currentState = resolvedStates.currentState(currentLicencePositionId);

    return new LicencePositionStateView(
        buildAdministratorState(currentState.administratorId(), organisationNames),
        buildBeneficialInterests(currentState.equityByOrganisationId(), organisationNames),
        buildOrganisationNameHistories(currentState, organisationNames, organisationNameHistories, positionDate)
    );
  }

  private static AdministratorStateView buildAdministratorState(
      @Nullable Integer currentAdministratorId,
      Map<Integer, String> organisationNames
  ) {
    if (currentAdministratorId == null) {
      return new AdministratorStateView("");
    }

    return new AdministratorStateView(organisationNames.getOrDefault(currentAdministratorId, ""));
  }

  private static List<BeneficialInterestView> buildBeneficialInterests(
      Map<Integer, BigDecimal> equityByOrganisationId,
      Map<Integer, String> organisationNames
  ) {
    return equityByOrganisationId.entrySet().stream()
        .map(entry -> new BeneficialInterestView(
            organisationNames.getOrDefault(entry.getKey(), NOT_AVAILABLE),
            entry.getValue()
        ))
        .sorted(Comparator.comparing(BeneficialInterestView::organisationName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  /**
   * Builds the name history for every organisation in the state — the licence administrator and each beneficial
   * interest holder. Organisations that were never renamed either side of the position date are left out so the
   * section stays empty when there is nothing to tell the user.
   */
  private static List<OrganisationNameHistoryView> buildOrganisationNameHistories(
      LicencePositionState currentState,
      Map<Integer, String> organisationNames,
      Map<Integer, List<OrganisationNameHistory>> organisationNameHistories,
      LocalDate positionDate
  ) {
    return stateOrganisationIds(currentState)
        .map(organisationId -> OrganisationNameHistoryUtil.getNameHistoryView(
            organisationNames.getOrDefault(organisationId, NOT_AVAILABLE),
            organisationNameHistories.getOrDefault(organisationId, List.of()),
            positionDate
        ))
        .filter(OrganisationNameHistoryView::hasNames)
        .sorted(Comparator.comparing(OrganisationNameHistoryView::organisationName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }

  public static Stream<Integer> stateOrganisationIds(LicencePositionState currentState) {
    return Stream.concat(
            Stream.ofNullable(currentState.administratorId()),
            currentState.equityByOrganisationId().keySet().stream()
        )
        .filter(Objects::nonNull)
        .distinct();
  }
}
