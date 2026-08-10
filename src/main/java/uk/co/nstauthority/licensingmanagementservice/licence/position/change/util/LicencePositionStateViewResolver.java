package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import jakarta.annotation.Nullable;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;

public final class LicencePositionStateViewResolver {

  private LicencePositionStateViewResolver() {
    throw new IllegalStateException("Utility class should not be instantiated.");
  }

  public static LicencePositionStateView getStateView(
      UUID currentLicencePositionId,
      Map<UUID, LicencePositionState> statesByChronologicalPositionId,
      Map<Integer, String> organisationNames
  ) {
    var state = statesByChronologicalPositionId.getOrDefault(currentLicencePositionId, LicencePositionState.EMPTY);

    return new LicencePositionStateView(
        buildAdministratorState(state.administratorId(), organisationNames),
        buildBeneficialInterests(state.equityByOrganisationId(), organisationNames)
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
            organisationNames.getOrDefault(entry.getKey(), "Not available"),
            entry.getValue()
        ))
        .sorted(Comparator.comparing(BeneficialInterestView::organisationName, String.CASE_INSENSITIVE_ORDER))
        .toList();
  }
}