package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.Month;
import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionKey;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;

class LicencePositionStateViewResolverTest {

  private static final int CURRENT_ADMIN_ID = 100;
  private static final String CURRENT_ADMIN_NAME = "Current Admin Ltd";

  @Test
  void getStateView_resolvesAdministratorAndBeneficialInterests() {
    var currentPositionId = UUID.randomUUID();

    var equityByOrganisationId = new LinkedHashMap<Integer, BigDecimal>();
    equityByOrganisationId.put(1, new BigDecimal("40"));
    equityByOrganisationId.put(2, new BigDecimal("35"));
    equityByOrganisationId.put(3, new BigDecimal("25"));

    var state = new LicencePositionState(CURRENT_ADMIN_ID, equityByOrganisationId);

    var organisationNames = Map.of(
        CURRENT_ADMIN_ID, CURRENT_ADMIN_NAME,
        1, "charlie oil",
        2, "alpha energy",
        3, "Bravo gas"
    );

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        resolvedStatesFor(currentPositionId, state),
        organisationNames
    );

    assertThat(result)
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView(CURRENT_ADMIN_NAME),
            List.of(
                new BeneficialInterestView("alpha energy", new BigDecimal("35")),
                new BeneficialInterestView("Bravo gas", new BigDecimal("25")),
                new BeneficialInterestView("charlie oil", new BigDecimal("40"))
            )
        ));
  }

  @Test
  void getStateView_whenNoStateForPosition_returnsEmptyAdministratorAndNoBeneficialInterests() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        new ResolvedStates(new TreeMap<>(), Map.of()),
        Map.of(CURRENT_ADMIN_ID, CURRENT_ADMIN_NAME)
    );

    assertThat(result).isEqualTo(new LicencePositionStateView(new AdministratorStateView(""), List.of()));
  }

  @Test
  void getStateView_whenAdministratorNameNotFound_returnsEmptyName() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        resolvedStatesFor(currentPositionId, LicencePositionState.EMPTY.withAdministratorId(CURRENT_ADMIN_ID)),
        Map.of()
    );

    assertThat(result.administratorStateView())
        .isEqualTo(new AdministratorStateView(""));
  }

  @Test
  void getStateView_whenBeneficialInterestNameNotFound_displaysNotAvailable() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY.withEquityByOrganisationId(Map.of(5, new BigDecimal("100")));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        resolvedStatesFor(currentPositionId, state),
        Map.of()
    );

    assertThat(result.beneficialInterests())
        .isEqualTo(List.of(new BeneficialInterestView("Not available", new BigDecimal("100"))));
  }

  private static ResolvedStates resolvedStatesFor(UUID positionId, LicencePositionState state) {
    var key = new PositionKey(LocalDate.of(2024, Month.JANUARY, 1), 0);
    var statesByKey = new TreeMap<PositionKey, LicencePositionState>();
    statesByKey.put(key, state);
    return new ResolvedStates(statesByKey, Map.of(positionId, key));
  }
}