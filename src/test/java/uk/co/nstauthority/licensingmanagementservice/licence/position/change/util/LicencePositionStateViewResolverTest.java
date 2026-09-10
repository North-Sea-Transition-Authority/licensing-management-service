package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods.OrganisationNamePeriod;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionKey;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;

class LicencePositionStateViewResolverTest {

  private static final int CURRENT_ADMIN_ID = 100;
  private static final String CURRENT_ADMIN_NAME = "Current Admin Ltd";
  private static final LocalDate POSITION_DATE = LocalDate.of(2010, Month.JUNE, 1);
  private static final LocalDate LONG_BEFORE_THE_POSITION_DATE = LocalDate.of(1990, Month.JANUARY, 1);

  @Test
  void getStateView_resolvesAdministratorAndBeneficialInterests() {
    var currentPositionId = UUID.randomUUID();

    var equityByOrganisationId = new LinkedHashMap<Integer, BigDecimal>();
    equityByOrganisationId.put(1, new BigDecimal("40"));
    equityByOrganisationId.put(2, new BigDecimal("35"));
    equityByOrganisationId.put(3, new BigDecimal("25"));

    var state = new LicencePositionState(CURRENT_ADMIN_ID, equityByOrganisationId);

    var nameHistories = Map.of(
        CURRENT_ADMIN_ID, heldSince(CURRENT_ADMIN_NAME),
        1, heldSince("charlie oil"),
        2, heldSince("alpha energy"),
        3, heldSince("Bravo gas")
    );

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(nameHistories),
        POSITION_DATE
    );

    assertThat(result)
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView(CURRENT_ADMIN_NAME),
            List.of(
                new BeneficialInterestView("alpha energy", new BigDecimal("35")),
                new BeneficialInterestView("Bravo gas", new BigDecimal("25")),
                new BeneficialInterestView("charlie oil", new BigDecimal("40"))
            ),
            List.of()
        ));
  }

  @Test
  void getStateView_whenNoStateForPosition_returnsEmptyAdministratorAndNoBeneficialInterests() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        new ResolvedStates(new TreeMap<>(), Map.of()),
        OrganisationNameContext.from(Map.of(CURRENT_ADMIN_ID, heldSince(CURRENT_ADMIN_NAME))),
        POSITION_DATE
    );

    assertThat(result).isEqualTo(new LicencePositionStateView(new AdministratorStateView(""), List.of(), List.of()));
  }

  @Test
  void getStateView_whenAdministratorNameNotFound_returnsEmptyName() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        resolvedStatesFor(currentPositionId, LicencePositionState.EMPTY.withAdministratorId(CURRENT_ADMIN_ID)),
        OrganisationNameContext.from(Map.of()),
        POSITION_DATE
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
        OrganisationNameContext.from(Map.of()),
        POSITION_DATE
    );

    assertThat(result.beneficialInterests())
        .isEqualTo(List.of(new BeneficialInterestView("Not available", new BigDecimal("100"))));
  }

  @Test
  void getStateView_whenOrganisationsRenamed_returnsNameHistoriesSortedByOrganisationName() {
    var currentPositionId = UUID.randomUUID();

    var state = new LicencePositionState(CURRENT_ADMIN_ID, Map.of(1, new BigDecimal("100")));

    var nameHistories = Map.of(
        CURRENT_ADMIN_ID, new OrganisationNamePeriods("zeta admin", List.of(
            new OrganisationNamePeriod(
                "Zeta Holdings", LONG_BEFORE_THE_POSITION_DATE, LocalDate.of(2000, Month.MARCH, 3)),
            new OrganisationNamePeriod("zeta admin", LocalDate.of(2000, Month.MARCH, 3), null))),
        1, new OrganisationNamePeriods("Alpha Renewables", List.of(
            new OrganisationNamePeriod("alpha energy", LONG_BEFORE_THE_POSITION_DATE, LocalDate.of(2020, Month.APRIL, 4)),
            new OrganisationNamePeriod("Alpha Renewables", LocalDate.of(2020, Month.APRIL, 4), null)))
    );

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(nameHistories),
        POSITION_DATE
    );

    assertThat(result.organisationNameHistories())
        .isEqualTo(List.of(
            new OrganisationNameHistoryView(
                "alpha energy",
                List.of(),
                List.of(new NameHistoryEntryView("Alpha Renewables", "from 4 April 2020"))),
            new OrganisationNameHistoryView(
                "zeta admin",
                List.of(new NameHistoryEntryView("Zeta Holdings", "to 3 March 2000")),
                List.of())
        ));
  }

  @Test
  void getStateView_whenOrganisationHasNoNamesEitherSideOfThePositionDate_excludesItFromNameHistories() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY.withAdministratorId(CURRENT_ADMIN_ID);

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(CURRENT_ADMIN_ID, heldSince(CURRENT_ADMIN_NAME))),
        POSITION_DATE
    );

    assertThat(result.organisationNameHistories()).isEmpty();
  }

  private static OrganisationNamePeriods heldSince(String name) {
    return new OrganisationNamePeriods(
        name,
        List.of(new OrganisationNamePeriod(name, LONG_BEFORE_THE_POSITION_DATE, null))
    );
  }

  private static ResolvedStates resolvedStatesFor(UUID positionId, LicencePositionState state) {
    var key = new PositionKey(LocalDate.of(2024, Month.JANUARY, 1), 0);
    var statesByKey = new TreeMap<PositionKey, LicencePositionState>();
    statesByKey.put(key, state);
    return new ResolvedStates(statesByKey, Map.of(positionId, key));
  }
}