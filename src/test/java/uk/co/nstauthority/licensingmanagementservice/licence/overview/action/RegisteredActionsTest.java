package uk.co.nstauthority.licensingmanagementservice.licence.overview.action;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.EnumMap;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceStatusType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicenceTimelinePositionTab;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTab;
import uk.co.nstauthority.licensingmanagementservice.licence.tab.LicenceTab;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;

class RegisteredActionsTest {

  private static final Licence LICENCE = LicenceTestUtil.builder()
      .withId(1)
      .withLicenceType(LicenceType.CARBON_STORAGE)
      .build();

  @Test
  void isPrimary_whenPredicateMatches_assertTrue() {
    var registeredActions = builder()
        .withPrimaryActionPredicate(LicenceActionItem.EDIT_LICENCE_DETAILS, licence -> true)
        .build();

    assertThat(registeredActions.isPrimary(LicenceActionItem.EDIT_LICENCE_DETAILS, LICENCE)).isTrue();
  }

  @Test
  void isPrimary_whenPredicateDoesNotMatch_assertFalse() {
    var registeredActions = builder()
        .withPrimaryActionPredicate(LicenceActionItem.EDIT_LICENCE_DETAILS, licence -> false)
        .build();

    assertThat(registeredActions.isPrimary(LicenceActionItem.EDIT_LICENCE_DETAILS, LICENCE)).isFalse();
  }

  @Test
  void isPrimary_assertPredicateEvaluatedAgainstGivenLicence() {
    var registeredActions = builder()
        .withPrimaryActionPredicate(
            LicenceActionItem.EDIT_LICENCE_DETAILS,
            licence -> LicenceType.CARBON_STORAGE.equals(licence.getType())
        )
        .build();

    var landwardLicence = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.LANDWARD_PRODUCTION)
        .build();

    assertThat(registeredActions.isPrimary(LicenceActionItem.EDIT_LICENCE_DETAILS, LICENCE)).isTrue();
    assertThat(registeredActions.isPrimary(LicenceActionItem.EDIT_LICENCE_DETAILS, landwardLicence)).isFalse();
  }

  @Test
  void isPrimary_whenNoPredicateRegistered_assertFalse() {
    var registeredActions = builder().build();

    assertThat(registeredActions.isPrimary(LicenceActionItem.EDIT_LICENCE_DETAILS, LICENCE)).isFalse();
  }

  @Test
  void getLicenceActionItemsForTab_whenTabHasRegisteredActions_assertActionsForThatTabOnly() {
    var registeredActions = builder()
        .withActionsForTab(LicenceScheduleTab.class, LicenceActionItem.CREATE_LICENCE_SCHEDULE)
        .withActionsForTab(LicenceTimelinePositionTab.class, LicenceActionItem.START_CORRECTION)
        .build();

    assertThat(registeredActions.getLicenceActionItemsForTab(new LicenceScheduleTab()))
        .containsExactly(LicenceActionItem.CREATE_LICENCE_SCHEDULE);
  }

  @Test
  void getLicenceActionItemsForTab_whenTabHasNoRegisteredActions_assertEmpty() {
    var registeredActions = builder()
        .withActionsForTab(LicenceScheduleTab.class, LicenceActionItem.CREATE_LICENCE_SCHEDULE)
        .build();

    assertThat(registeredActions.getLicenceActionItemsForTab(new LicenceTimelinePositionTab())).isEmpty();
  }

  @Test
  void isLicenceApplicableToStatus_whenActionRegisteredForLicenceStatus_assertTrue() {
    var registeredActions = builder()
        .withActionsForStatus(LicenceStatusType.EXTANT, LicenceActionItem.EDIT_LICENCE_DETAILS)
        .build();

    assertThat(registeredActions.isLicenceApplicableToStatus(
        LicenceActionItem.EDIT_LICENCE_DETAILS,
        LicenceStatusType.EXTANT
    )).isTrue();
  }

  @Test
  void isLicenceApplicableToStatus_whenActionNotRegisteredForLicenceStatus_assertFalse() {
    var registeredActions = builder()
        .withActionsForStatus(LicenceStatusType.EXTANT, LicenceActionItem.EDIT_LICENCE_DETAILS)
        .build();

    assertThat(registeredActions.isLicenceApplicableToStatus(
        LicenceActionItem.START_CORRECTION,
        LicenceStatusType.EXTANT
    )).isFalse();
  }

  @Test
  void isLicenceApplicableToStatus_whenNoActionsRegisteredForLicenceStatus_assertFalse() {
    var registeredActions = builder()
        .withActionsForStatus(LicenceStatusType.REVOKED, LicenceActionItem.EDIT_LICENCE_DETAILS)
        .build();

    assertThat(registeredActions.isLicenceApplicableToStatus(
        LicenceActionItem.EDIT_LICENCE_DETAILS,
        LicenceStatusType.EXTANT
    )).isFalse();
  }

  @Test
  void canRolesAccessAction_whenUserHasOneOfTheRequiredRoles_assertTrue() {
    var registeredActions = builder()
        .withRolesForAction(
            LicenceActionItem.UPDATE_LICENCE_SCHEDULE,
            Role.SCHEDULE_ADMINISTRATOR,
            Role.WORK_PROGRAMME_ADMINISTRATOR
        )
        .build();

    assertThat(registeredActions.canRolesAccessAction(
        LicenceActionItem.UPDATE_LICENCE_SCHEDULE,
        Set.of(Role.WORK_PROGRAMME_ADMINISTRATOR)
    )).isTrue();
  }

  @Test
  void canRolesAccessAction_whenUserHasNoneOfTheRequiredRoles_assertFalse() {
    var registeredActions = builder()
        .withRolesForAction(LicenceActionItem.UPDATE_LICENCE_SCHEDULE, Role.SCHEDULE_ADMINISTRATOR)
        .build();

    assertThat(registeredActions.canRolesAccessAction(
        LicenceActionItem.UPDATE_LICENCE_SCHEDULE,
        Set.of(Role.OFFLINE_LICENCE_ADMINISTRATOR)
    )).isFalse();
  }

  @Test
  void canRolesAccessAction_whenUserHasNoRoles_assertFalse() {
    var registeredActions = builder()
        .withRolesForAction(LicenceActionItem.UPDATE_LICENCE_SCHEDULE, Role.SCHEDULE_ADMINISTRATOR)
        .build();

    assertThat(registeredActions.canRolesAccessAction(LicenceActionItem.UPDATE_LICENCE_SCHEDULE, Set.of())).isFalse();
  }

  @Test
  void canRolesAccessAction_whenActionHasNoRegisteredRoles_assertFalse() {
    var registeredActions = builder().build();

    assertThat(registeredActions.canRolesAccessAction(
        LicenceActionItem.UPDATE_LICENCE_SCHEDULE,
        Set.of(Role.SCHEDULE_ADMINISTRATOR)
    )).isFalse();
  }

  @Test
  void isActionApplicableToLicenceType_whenActionRegisteredForLicenceType_assertTrue() {
    var registeredActions = builder()
        .withLicenceTypesForAction(LicenceActionItem.START_CORRECTION, LicenceType.CARBON_STORAGE)
        .build();

    assertThat(registeredActions.isActionApplicableToLicenceType(LicenceActionItem.START_CORRECTION, LICENCE))
        .isTrue();
  }

  @Test
  void isActionApplicableToLicenceType_whenActionNotRegisteredForLicenceType_assertFalse() {
    var registeredActions = builder()
        .withLicenceTypesForAction(LicenceActionItem.START_CORRECTION, LicenceType.SEAWARD_PRODUCTION)
        .build();

    assertThat(registeredActions.isActionApplicableToLicenceType(LicenceActionItem.START_CORRECTION, LICENCE))
        .isFalse();
  }

  private static TestRegisteredActionsBuilder builder() {
    return new TestRegisteredActionsBuilder();
  }

  private static class TestRegisteredActionsBuilder {

    private final Map<LicenceStatusType, Set<LicenceActionItem>> statusMap = new EnumMap<>(LicenceStatusType.class);
    private final Map<LicenceActionItem, Set<Role>> roleMap = new EnumMap<>(LicenceActionItem.class);
    private final Map<LicenceActionItem, Set<LicenceType>> licenceTypeMap = new EnumMap<>(LicenceActionItem.class);
    private final Map<LicenceActionItem, Set<LicenceScheduleRequirement>> licenceScheduleRequirementMap =
        new EnumMap<>(LicenceActionItem.class);
    private final Map<LicenceActionItem, Predicate<Licence>> primaryActionPredicateMap =
        new EnumMap<>(LicenceActionItem.class);
    private final Set<LicenceActionItem> topLevelLicenceActionItems = new HashSet<>();
    private final Map<Class<? extends LicenceTab>, Set<LicenceActionItem>> licenceActionItemsByLicenceTabClass =
        new HashMap<>();

    TestRegisteredActionsBuilder withActionsForStatus(LicenceStatusType status, LicenceActionItem... actionItems) {
      statusMap.put(status, Set.of(actionItems));
      return this;
    }

    TestRegisteredActionsBuilder withRolesForAction(LicenceActionItem actionItem, Role... roles) {
      roleMap.put(actionItem, Set.of(roles));
      return this;
    }

    TestRegisteredActionsBuilder withLicenceTypesForAction(LicenceActionItem actionItem, LicenceType... licenceTypes) {
      licenceTypeMap.put(actionItem, Set.of(licenceTypes));
      return this;
    }

    TestRegisteredActionsBuilder withPrimaryActionPredicate(LicenceActionItem actionItem, Predicate<Licence> predicate) {
      primaryActionPredicateMap.put(actionItem, predicate);
      return this;
    }

    TestRegisteredActionsBuilder withActionsForTab(
        Class<? extends LicenceTab> licenceTabClass,
        LicenceActionItem... actionItems
    ) {
      licenceActionItemsByLicenceTabClass.put(licenceTabClass, Set.of(actionItems));
      return this;
    }

    RegisteredActions build() {
      return new RegisteredActions(
          statusMap,
          roleMap,
          licenceTypeMap,
          licenceScheduleRequirementMap,
          primaryActionPredicateMap,
          topLevelLicenceActionItems,
          licenceActionItemsByLicenceTabClass
      );
    }
  }
}
