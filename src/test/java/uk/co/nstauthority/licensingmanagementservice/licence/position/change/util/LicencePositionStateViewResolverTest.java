package uk.co.nstauthority.licensingmanagementservice.licence.position.change.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Month;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationNamePeriods.OrganisationNamePeriod;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.SubareaDetails;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.LicencePositionState;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.PositionKey;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.ResolvedStates;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.AdministratorStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.BeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicencePositionStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeStateView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.LicenseeView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.NameHistoryEntryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.OrganisationNameHistoryView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.view.state.SubareaBeneficialInterestView;
import uk.co.nstauthority.licensingmanagementservice.licence.position.feature.FeatureSummary;

class LicencePositionStateViewResolverTest {

  private static final int CURRENT_ADMIN_ID = 100;
  private static final String CURRENT_ADMIN_NAME = "Current Admin Ltd";
  private static final int CURRENT_STORAGE_OPERATOR_ID = 200;
  private static final String CURRENT_STORAGE_OPERATOR_NAME = "Current Storage Operator Ltd";
  private static final LocalDate POSITION_DATE = LocalDate.of(2010, Month.JUNE, 1);
  private static final LocalDate LONG_BEFORE_THE_POSITION_DATE = LocalDate.of(1990, Month.JANUARY, 1);

  @Test
  void getStateView_resolvesState_withoutNameHistories() {
    var currentPositionId = UUID.randomUUID();

    var equityByOrganisationId = new LinkedHashMap<Integer, BigDecimal>();
    equityByOrganisationId.put(1, new BigDecimal("40"));
    equityByOrganisationId.put(2, new BigDecimal("35"));
    equityByOrganisationId.put(3, new BigDecimal("25"));

    var state = new LicencePositionState(
        CURRENT_ADMIN_ID,
        CURRENT_STORAGE_OPERATOR_ID,
        Set.of(11, 12),
        equityByOrganisationId,
        Map.of(),
        Map.of()
    );

    var nameHistories = Map.of(
        CURRENT_ADMIN_ID, heldSince(CURRENT_ADMIN_NAME),
        CURRENT_STORAGE_OPERATOR_ID, heldSince(CURRENT_STORAGE_OPERATOR_NAME),
        1, heldSince("charlie oil"),
        2, heldSince("alpha energy"),
        3, heldSince("Bravo gas"),
        11, heldSince("delta diff"),
        12, heldSince("echo location")
    );

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(nameHistories),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result)
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView(CURRENT_ADMIN_NAME),
            new LicenseeStateView(List.of(
                new LicenseeView("delta diff", OrganisationMarker.NO_BENEFICIAL_INTEREST),
                new LicenseeView("echo location", OrganisationMarker.NO_BENEFICIAL_INTEREST))),
            CURRENT_STORAGE_OPERATOR_NAME,
            List.of(
                new BeneficialInterestView("alpha energy", new BigDecimal("35"), OrganisationMarker.NOT_A_LICENSEE),
                new BeneficialInterestView("Bravo gas", new BigDecimal("25"), OrganisationMarker.NOT_A_LICENSEE),
                new BeneficialInterestView("charlie oil", new BigDecimal("40"), OrganisationMarker.NOT_A_LICENSEE)
            ),
            List.of(),
            List.of()
        ));
  }

  @Test
  void getStateView_whenNoStateForPosition_returnsEmptyAdministratorAndNoBeneficialInterests() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        new ResolvedStates(new TreeMap<>(), Map.of()),
        OrganisationNameContext.from(Map.of(CURRENT_ADMIN_ID, heldSince(CURRENT_ADMIN_NAME))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result).isEqualTo(new LicencePositionStateView(new AdministratorStateView(""),
        new LicenseeStateView(List.of()),
        "",
        List.of(),
        List.of(),
        List.of()));
  }

  @Test
  void getStateView_whenLicenseeNameNotFound_displaysNotAvailable() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY.withLicenseeIds(List.of(1, 2), List.of());

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.GAS_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("organisationOne"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.licenseeStateView())
        .isEqualTo(new LicenseeStateView(List.of(
            new LicenseeView("Not available", null),
            new LicenseeView("organisationOne", null))));
  }

  @ParameterizedTest
  @EnumSource(value = LicenceType.class, names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION"})
  void getStateView_whenNoBeneficialInterestsRecorded_thenEveryLicenseeMarkedNoBeneficialInterest(
      LicenceType licenceType
  ) {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY.withLicenseeIds(List.of(1, 2), List.of());

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        licenceType,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"), 2, heldSince("bravo gas"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.licenseeStateView())
        .isEqualTo(new LicenseeStateView(List.of(
            new LicenseeView("alpha energy", OrganisationMarker.NO_BENEFICIAL_INTEREST),
            new LicenseeView("bravo gas", OrganisationMarker.NO_BENEFICIAL_INTEREST))));
  }

  @ParameterizedTest
  @EnumSource(
      value = LicenceType.class,
      mode = EnumSource.Mode.EXCLUDE,
      names = {"CARBON_STORAGE", "LANDWARD_PRODUCTION", "SEAWARD_PRODUCTION"}
  )
  void getStateView_whenLicenceTypeHasNoBeneficialInterests_thenLicenseesNotMarked(LicenceType licenceType) {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY.withLicenseeIds(List.of(1), List.of());

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        licenceType,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.licenseeStateView())
        .isEqualTo(new LicenseeStateView(List.of(new LicenseeView("alpha energy", null))));
  }

  @Test
  void getStateView_whenCarbonStorage_thenMarksLicenseesWithoutLicenceLevelEquity() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY
        .withLicenseeIds(List.of(1, 2), List.of())
        .withEquityByOrganisationId(Map.of(1, new BigDecimal("100")));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"), 2, heldSince("bravo gas"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.licenseeStateView())
        .isEqualTo(new LicenseeStateView(List.of(
            new LicenseeView("alpha energy", null),
            new LicenseeView("bravo gas", OrganisationMarker.NO_BENEFICIAL_INTEREST))));
  }

  @Test
  void getStateView_whenProduction_thenMarksLicenseesWithoutSubareaEquity() {
    var currentPositionId = UUID.randomUUID();
    var blockId = UUID.randomUUID();
    var subarea = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");

    var state = LicencePositionState.EMPTY
        .withLicenseeIds(List.of(1, 2), List.of())
        .withSubareaEquity(blockId, subarea, 1, new BigDecimal("100"));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.SEAWARD_PRODUCTION,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"), 2, heldSince("bravo gas"))),
        Map.of(blockId, new FeatureSummary("21/1", BigDecimal.ONE)),
        POSITION_DATE
    );

    assertThat(result.licenseeStateView())
        .isEqualTo(new LicenseeStateView(List.of(
            new LicenseeView("alpha energy", null),
            new LicenseeView("bravo gas", OrganisationMarker.NO_BENEFICIAL_INTEREST))));
  }

  @Test
  void getStateView_whenLicenseeHoldsOnlyZeroEquity_thenNotMarked() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY
        .withLicenseeIds(List.of(1), List.of())
        .withEquityByOrganisationId(Map.of(1, BigDecimal.ZERO));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.licenseeStateView())
        .isEqualTo(new LicenseeStateView(List.of(
            new LicenseeView("alpha energy", null))));
  }

  @Test
  void getStateView_whenStorageOperatorNameNotFound_returnsEmptyName() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY.withStorageOperatorId(1);

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of()),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.storageOperatorName())
        .isEqualTo("");
  }

  @Test
  void getStateView_whenAdministratorNameNotFound_returnsEmptyName() {
    var currentPositionId = UUID.randomUUID();

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.SEAWARD_PRODUCTION,
        resolvedStatesFor(currentPositionId, LicencePositionState.EMPTY.withAdministratorId(CURRENT_ADMIN_ID)),
        OrganisationNameContext.from(Map.of()),
        Map.of(),
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
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of()),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.beneficialInterests())
        .isEqualTo(List.of(
            new BeneficialInterestView("Not available", new BigDecimal("100"), OrganisationMarker.NOT_A_LICENSEE)));
  }

  @Test
  void getStateView_whenBeneficialInterestHolderIsNotALicensee_thenMarkedNotALicensee() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY
        .withLicenseeIds(List.of(2), List.of())
        .withEquityByOrganisationId(Map.of(1, new BigDecimal("100")));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"), 2, heldSince("bravo gas"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.beneficialInterests())
        .isEqualTo(List.of(
            new BeneficialInterestView("alpha energy", new BigDecimal("100"), OrganisationMarker.NOT_A_LICENSEE)));
  }

  @Test
  void getStateView_whenBeneficialInterestHolderIsALicensee_thenNotMarked() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY
        .withLicenseeIds(List.of(1), List.of())
        .withEquityByOrganisationId(Map.of(1, new BigDecimal("100")));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.beneficialInterests())
        .isEqualTo(List.of(new BeneficialInterestView("alpha energy", new BigDecimal("100"), null)));
  }

  @Test
  void getStateView_whenNonLicenseeHoldsZeroEquity_thenMarkedNotALicensee() {
    var currentPositionId = UUID.randomUUID();

    var state = LicencePositionState.EMPTY.withEquityByOrganisationId(Map.of(1, BigDecimal.ZERO));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.beneficialInterests())
        .isEqualTo(List.of(
            new BeneficialInterestView("alpha energy", BigDecimal.ZERO, OrganisationMarker.NOT_A_LICENSEE)));
  }

  @Test
  void getStateView_whenOrganisationsRenamed_returnsNameHistoriesSortedByOrganisationName() {
    var currentPositionId = UUID.randomUUID();

    var state = new LicencePositionState(
        CURRENT_ADMIN_ID, CURRENT_STORAGE_OPERATOR_ID, Set.of(11), Map.of(1, new BigDecimal("100")), Map.of(), Map.of());

    var nameHistories = Map.of(
        CURRENT_ADMIN_ID, new OrganisationNamePeriods("zeta admin", List.of(
            new OrganisationNamePeriod(
                "Zeta Holdings", LONG_BEFORE_THE_POSITION_DATE, LocalDate.of(2000, Month.MARCH, 3)),
            new OrganisationNamePeriod("zeta admin", LocalDate.of(2000, Month.MARCH, 3), null))),
        CURRENT_STORAGE_OPERATOR_ID, new OrganisationNamePeriods("delta operator", List.of(
            new OrganisationNamePeriod(
                "delta holdings", LONG_BEFORE_THE_POSITION_DATE, LocalDate.of(2000, Month.MARCH, 3)),
            new OrganisationNamePeriod("delta operator", LocalDate.of(2000, Month.MARCH, 3), null))),
        1, new OrganisationNamePeriods("Alpha Renewables", List.of(
            new OrganisationNamePeriod("alpha energy", LONG_BEFORE_THE_POSITION_DATE, LocalDate.of(2020, Month.APRIL, 4)),
            new OrganisationNamePeriod("Alpha Renewables", LocalDate.of(2020, Month.APRIL, 4), null))),
        11, new OrganisationNamePeriods("beta pharmaceuticals", List.of(
            new OrganisationNamePeriod("beta financials", LONG_BEFORE_THE_POSITION_DATE, LocalDate.of(2020, Month.MAY, 5)),
            new OrganisationNamePeriod("beta pharmaceuticals", LocalDate.of(2020, Month.MAY, 5), null)))
    );

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.CARBON_STORAGE,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(nameHistories),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.organisationNameHistories())
        .isEqualTo(List.of(
            new OrganisationNameHistoryView(
                "alpha energy",
                List.of(),
                List.of(new NameHistoryEntryView("Alpha Renewables", "from 4 April 2020"))),
            new OrganisationNameHistoryView(
                "beta financials",
                List.of(),
                List.of(new NameHistoryEntryView("beta pharmaceuticals", "from 5 May 2020"))),
            new OrganisationNameHistoryView(
                "delta operator",
                List.of(new NameHistoryEntryView("delta holdings", "to 3 March 2000")),
                List.of()),
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
        LicenceType.SEAWARD_PRODUCTION,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(CURRENT_ADMIN_ID, heldSince(CURRENT_ADMIN_NAME))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.organisationNameHistories()).isEmpty();
  }

  @Test
  void getStateView_whenSubareaBeneficialInterests_returnsOneEntryPerSubareaSortedByLabel() {
    var currentPositionId = UUID.randomUUID();
    var firstBlockId = UUID.randomUUID();
    var secondBlockId = UUID.randomUUID();
    var subareaOnSecondBlock = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");
    var namedSubareaOnFirstBlock = new SubareaDetails(UUID.randomUUID(), "Subarea B", "B");
    var legacySubareaOnFirstBlock = new SubareaDetails(null, null, "a");

    var state = LicencePositionState.EMPTY
        .withSubareaEquity(secondBlockId, subareaOnSecondBlock, 1, new BigDecimal("60"))
        .withSubareaEquity(secondBlockId, subareaOnSecondBlock, 2, new BigDecimal("40"))
        .withSubareaEquity(firstBlockId, namedSubareaOnFirstBlock, 3, new BigDecimal("100"))
        .withSubareaEquity(firstBlockId, legacySubareaOnFirstBlock, 1, new BigDecimal("100"));

    var nameHistories = Map.of(
        1, heldSince("charlie oil"),
        2, heldSince("alpha energy"),
        3, heldSince("Bravo gas")
    );

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.SEAWARD_PRODUCTION,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(nameHistories),
        Map.of(
            firstBlockId, new FeatureSummary("21/1", BigDecimal.ONE),
            secondBlockId, new FeatureSummary("21/2", BigDecimal.ONE)),
        POSITION_DATE
    );

    assertThat(result)
        .isEqualTo(new LicencePositionStateView(
            new AdministratorStateView(""),
            new LicenseeStateView(List.of()),
            "",
            List.of(),
            List.of(),
            List.of(
                new SubareaBeneficialInterestView(
                    "Block 21/1 - Subarea a",
                    List.of(
                        new BeneficialInterestView("charlie oil", new BigDecimal("100"), OrganisationMarker.NOT_A_LICENSEE))),
                new SubareaBeneficialInterestView(
                    "Block 21/1 - Subarea B",
                    List.of(
                        new BeneficialInterestView("Bravo gas", new BigDecimal("100"), OrganisationMarker.NOT_A_LICENSEE))),
                new SubareaBeneficialInterestView(
                    "Block 21/2 - Subarea A",
                    List.of(
                        new BeneficialInterestView("alpha energy", new BigDecimal("40"), OrganisationMarker.NOT_A_LICENSEE),
                        new BeneficialInterestView("charlie oil", new BigDecimal("60"), OrganisationMarker.NOT_A_LICENSEE)))
            )
        ));
  }

  @Test
  void getStateView_whenBlockNameNotFound_labelsSubareaWithUnknownBlock() {
    var currentPositionId = UUID.randomUUID();
    var subarea = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");

    var state = LicencePositionState.EMPTY.withSubareaEquity(UUID.randomUUID(), subarea, 1, new BigDecimal("100"));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.SEAWARD_PRODUCTION,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"))),
        Map.of(),
        POSITION_DATE
    );

    assertThat(result.subareaBeneficialInterests())
        .isEqualTo(List.of(new SubareaBeneficialInterestView(
            "Unknown block - Subarea A",
            List.of(new BeneficialInterestView("alpha energy", new BigDecimal("100"), OrganisationMarker.NOT_A_LICENSEE)))));
  }

  @Test
  void getStateView_whenSubareaBeneficialInterestHolderIsNotALicensee_thenMarkedNotALicensee() {
    var currentPositionId = UUID.randomUUID();
    var blockId = UUID.randomUUID();
    var subarea = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");

    var state = LicencePositionState.EMPTY
        .withLicenseeIds(List.of(1), List.of())
        .withSubareaEquity(blockId, subarea, 1, new BigDecimal("60"))
        .withSubareaEquity(blockId, subarea, 2, new BigDecimal("40"));

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.SEAWARD_PRODUCTION,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(Map.of(1, heldSince("alpha energy"), 2, heldSince("bravo gas"))),
        Map.of(blockId, new FeatureSummary("21/1", BigDecimal.ONE)),
        POSITION_DATE
    );

    assertThat(result.subareaBeneficialInterests())
        .isEqualTo(List.of(new SubareaBeneficialInterestView(
            "Block 21/1 - Subarea A",
            List.of(
                new BeneficialInterestView("alpha energy", new BigDecimal("60"), null),
                new BeneficialInterestView("bravo gas", new BigDecimal("40"), OrganisationMarker.NOT_A_LICENSEE)))));
  }

  @Test
  void getStateView_whenSubareaBeneficialInterestHolderRenamed_includesThemInNameHistories() {
    var currentPositionId = UUID.randomUUID();
    var blockId = UUID.randomUUID();
    var subarea = new SubareaDetails(UUID.randomUUID(), "Subarea A", "A");

    var state = LicencePositionState.EMPTY.withSubareaEquity(blockId, subarea, 1, new BigDecimal("100"));

    var nameHistories = Map.of(
        1, new OrganisationNamePeriods("Alpha Renewables", List.of(
            new OrganisationNamePeriod("alpha energy", LONG_BEFORE_THE_POSITION_DATE, LocalDate.of(2020, Month.APRIL, 4)),
            new OrganisationNamePeriod("Alpha Renewables", LocalDate.of(2020, Month.APRIL, 4), null)))
    );

    var result = LicencePositionStateViewResolver.getStateView(
        currentPositionId,
        LicenceType.SEAWARD_PRODUCTION,
        resolvedStatesFor(currentPositionId, state),
        OrganisationNameContext.from(nameHistories),
        Map.of(blockId, new FeatureSummary("21/1", BigDecimal.ONE)),
        POSITION_DATE
    );

    assertThat(result.organisationNameHistories())
        .isEqualTo(List.of(new OrganisationNameHistoryView(
            "alpha energy",
            List.of(),
            List.of(new NameHistoryEntryView("Alpha Renewables", "from 4 April 2020")))));
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