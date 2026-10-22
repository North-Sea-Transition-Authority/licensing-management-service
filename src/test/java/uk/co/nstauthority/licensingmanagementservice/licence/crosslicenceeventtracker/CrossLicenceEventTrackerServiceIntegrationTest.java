package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import jakarta.persistence.EntityManager;
import jakarta.persistence.EntityManagerFactory;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.UUID;
import org.hibernate.SessionFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.client.user.UserApi;
import uk.co.fivium.energyportalapi.generated.types.OrganisationGroup;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.components.duration.ThreeFieldDuration;
import uk.co.nstauthority.licensingmanagementservice.fds.table.Tag;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.PhaseType;
import uk.co.nstauthority.licensingmanagementservice.licence.TermType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceSchedule;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhase;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.EnergyPortalUserTestUtil;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class CrossLicenceEventTrackerServiceIntegrationTest {

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private EntityManagerFactory entityManagerFactory;

  @Autowired
  private CrossLicenceEventTrackerService crossLicenceEventTrackerService;

  @Autowired
  private TeamQueryService teamQueryService;

  @MockitoBean
  private OrganisationApi organisationApi;

  @MockitoBean
  private UserApi userApi;

  private final ServiceUserDetail industryUser = ServiceUserDetailTestUtil.newBuilder().withWuaId(9100L).build();

  @Test
  void getEventTrackerTable_retrievesCachedEventsViaJooq() {
    var licence = LicenceTestUtil.builder()
        .withId(9001)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("999")
        .withLicenceReference("P 999")
        .build();
    entityManager.persist(licence);

    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licence.getId());
    eventCache.setLicenceReference(licence.getLicenceReference());
    eventCache.setEventType(ScheduleEventType.TERM);
    eventCache.setCurrentTermPhase("Initial Term");
    eventCache.setNextTermPhase("Second Term");
    eventCache.setEventDate(LocalDate.of(2030, Month.JANUARY, 1));
    eventCache.setQuadBlock("12/3");
    entityManager.persist(eventCache);

    entityManager.flush();

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), regulatorUser());

    assertThat(result.tableRows()).hasSize(2);

    var row = result.tableRows().get(1).rowValues();
    assertThat(row.get(0).value()).isEqualTo("P 999");
    assertThat(row.get(0).tags()).isEmpty();
    assertThat(row.get(1).value()).isEqualTo("Initial Term to Second Term");
    assertThat(row.get(3).value()).isEqualTo(DateFormatUtil.convertToDisplayText(LocalDate.of(2030, Month.JANUARY, 1)));
    assertThat(row.get(3).sortValue()).isEqualTo("2030-01-01");
    assertThat(row.get(5).value()).isEqualTo("");
    assertThat(row.get(6).value()).isEqualTo("12/3");
  }

  @Test
  void getEventTrackerTable_whenEventLinkedToApplication_thenApplicationStatusAndStewardArePopulated() {
    var licence = LicenceTestUtil.builder()
        .withId(9020)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("987")
        .withLicenceReference("P 987")
        .build();
    entityManager.persist(licence);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    entityManager.persist(licenceSchedule);

    var application = new ScheduleWorkProgrammeApplication();
    application.setLicenceSchedule(licenceSchedule);
    application.setStewardWuaId(9300L);
    application.setApplicationReference("LMS/EAA/2040/1");
    entityManager.persist(application);
    entityManager.flush();

    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    applicationDetail.setScheduleWorkProgrammeApplication(application);
    applicationDetail.setVersionNumber(1);
    applicationDetail.setStatus(ApplicationStatus.SUBMITTED);
    applicationDetail.setCreatedDatetime(Instant.now());
    entityManager.persist(applicationDetail);

    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licence.getId());
    eventCache.setLicenceReference(licence.getLicenceReference());
    eventCache.setEventType(ScheduleEventType.TERM);
    eventCache.setEventDate(LocalDate.of(2040, Month.JANUARY, 1));
    eventCache.setApplicationId(application.getId());
    eventCache.setApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    eventCache.setStewardWuaId(9300L);
    entityManager.persist(eventCache);

    entityManager.flush();

    var steward = EnergyPortalUserTestUtil.newBuilder()
        .withWebUserAccountId(9300L)
        .withForename("Steve")
        .withSurname("Steward")
        .build();
    when(userApi.searchUsersByIds(any(), any(), any())).thenReturn(List.of(steward));

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), regulatorUser());

    assertThat(result.tableRows()).hasSize(2);

    var row = result.tableRows().get(1).rowValues();
    assertThat(row.get(0).value()).isEqualTo("P 987 (LMS/EAA/2040/1)");
    assertThat(row.get(0).link()).isEqualTo(
        ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
            .renderOverview(applicationDetail.getId(), null, null, null))
    );
    assertThat(row.get(0).tags()).containsExactly(
        new Tag(
            ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getTagDisplayName(),
            ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getTagColour()
        )
    );
    assertThat(row.get(4).value()).isEqualTo("Submitted");
    assertThat(row.get(7).value()).isEqualTo("Steve Steward");
  }

  @Test
  void getEventTrackerTable_whenEventLinkedToContinuationApplication_thenApplicationStatusAndReferenceArePopulated() {
    var licence = LicenceTestUtil.builder()
        .withId(9021)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("985")
        .withLicenceReference("P 985")
        .build();
    entityManager.persist(licence);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    entityManager.persist(licenceSchedule);

    var application = new LicenceContinuationApplication();
    application.setLicenceSchedule(licenceSchedule);
    application.setApplicationReference("LMS/CA/2040/1");
    entityManager.persist(application);
    entityManager.flush();

    var applicationDetail = new LicenceContinuationApplicationDetail();
    applicationDetail.setLicenceContinuationApplication(application);
    applicationDetail.setVersionNumber(1);
    applicationDetail.setStatus(ApplicationStatus.SUBMITTED);
    applicationDetail.setCreatedDateTime(Instant.now());
    entityManager.persist(applicationDetail);

    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licence.getId());
    eventCache.setLicenceReference(licence.getLicenceReference());
    eventCache.setEventType(ScheduleEventType.TERM);
    eventCache.setEventDate(LocalDate.of(2041, Month.JANUARY, 1));
    eventCache.setApplicationId(application.getId());
    eventCache.setApplicationType(ApplicationType.CONTINUATION_APPLICATION);
    entityManager.persist(eventCache);

    entityManager.flush();

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), regulatorUser());

    assertThat(result.tableRows()).hasSize(2);

    var row = result.tableRows().get(1).rowValues();
    assertThat(row.get(0).value()).isEqualTo("P 985 (LMS/CA/2040/1)");
    assertThat(row.get(0).link()).isEqualTo(
        ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
            .renderOverview(applicationDetail.getId(), null, null, null))
    );
    assertThat(row.get(0).tags()).containsExactly(
        new Tag(
            ApplicationType.CONTINUATION_APPLICATION.getTagDisplayName(),
            ApplicationType.CONTINUATION_APPLICATION.getTagColour()
        )
    );
    assertThat(row.get(4).value()).isEqualTo("Submitted");
    assertThat(row.get(7).value()).isEqualTo("");
  }

  @Test
  void getEventTrackerTable_whenMultipleCachedEvents_thenRowsSortedByEventDate() {
    var earlierLicence = LicenceTestUtil.builder()
        .withId(9002)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("998")
        .withLicenceReference("P 998")
        .build();
    entityManager.persist(earlierLicence);

    var laterLicence = LicenceTestUtil.builder()
        .withId(9003)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("997")
        .withLicenceReference("P 997")
        .build();
    entityManager.persist(laterLicence);

    var laterEventCache = new LicenceEventCache();
    laterEventCache.setLicenceId(laterLicence.getId());
    laterEventCache.setLicenceReference(laterLicence.getLicenceReference());
    laterEventCache.setEventType(ScheduleEventType.TERM);
    laterEventCache.setEventDate(LocalDate.of(2032, Month.JANUARY, 1));
    entityManager.persist(laterEventCache);

    var earlierEventCache = new LicenceEventCache();
    earlierEventCache.setLicenceId(earlierLicence.getId());
    earlierEventCache.setLicenceReference(earlierLicence.getLicenceReference());
    earlierEventCache.setEventType(ScheduleEventType.TERM);
    earlierEventCache.setEventDate(LocalDate.of(2031, Month.JANUARY, 1));
    entityManager.persist(earlierEventCache);

    entityManager.flush();

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), regulatorUser());

    assertThat(result.tableRows()).hasSize(3);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("P 998");
    assertThat(result.tableRows().get(2).rowValues().get(0).value()).isEqualTo("P 997");
  }

  @Test
  void getEventTrackerTable_whenLicenceTypeFilterApplied_thenOnlyMatchingLicenceTypesReturned() {
    var seawardLicence = LicenceTestUtil.builder()
        .withId(9004)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("996")
        .withLicenceReference("P 996")
        .build();
    entityManager.persist(seawardLicence);

    var carbonStorageLicence = LicenceTestUtil.builder()
        .withId(9005)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceNumber("995")
        .withLicenceReference("CS 995")
        .build();
    entityManager.persist(carbonStorageLicence);

    var seawardEventCache = new LicenceEventCache();
    seawardEventCache.setLicenceId(seawardLicence.getId());
    seawardEventCache.setLicenceReference(seawardLicence.getLicenceReference());
    seawardEventCache.setEventType(ScheduleEventType.TERM);
    seawardEventCache.setEventDate(LocalDate.of(2033, Month.JANUARY, 1));
    entityManager.persist(seawardEventCache);

    var carbonStorageEventCache = new LicenceEventCache();
    carbonStorageEventCache.setLicenceId(carbonStorageLicence.getId());
    carbonStorageEventCache.setLicenceReference(carbonStorageLicence.getLicenceReference());
    carbonStorageEventCache.setEventType(ScheduleEventType.TERM);
    carbonStorageEventCache.setEventDate(LocalDate.of(2034, Month.JANUARY, 1));
    entityManager.persist(carbonStorageEventCache);

    entityManager.flush();

    var form = new EventTrackerForm();
    form.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name()));

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, regulatorUser());

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("CS 995");
  }

  @Test
  void getEventTrackerTable_whenRequestTypeFilterApplied_thenOnlyMatchingApplicationTypesReturned() {
    var extensionLicence = LicenceTestUtil.builder()
        .withId(9009)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("991")
        .withLicenceReference("P 991")
        .build();
    entityManager.persist(extensionLicence);

    var continuationLicence = LicenceTestUtil.builder()
        .withId(9010)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("990")
        .withLicenceReference("P 990")
        .build();
    entityManager.persist(continuationLicence);

    var noApplicationLicence = LicenceTestUtil.builder()
        .withId(9011)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("989")
        .withLicenceReference("P 989")
        .build();
    entityManager.persist(noApplicationLicence);

    var extensionEventCache = new LicenceEventCache();
    extensionEventCache.setLicenceId(extensionLicence.getId());
    extensionEventCache.setLicenceReference(extensionLicence.getLicenceReference());
    extensionEventCache.setEventType(ScheduleEventType.TERM);
    extensionEventCache.setEventDate(LocalDate.of(2035, Month.JANUARY, 1));
    extensionEventCache.setApplicationId(UUID.randomUUID());
    extensionEventCache.setApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    entityManager.persist(extensionEventCache);

    var continuationEventCache = new LicenceEventCache();
    continuationEventCache.setLicenceId(continuationLicence.getId());
    continuationEventCache.setLicenceReference(continuationLicence.getLicenceReference());
    continuationEventCache.setEventType(ScheduleEventType.TERM);
    continuationEventCache.setEventDate(LocalDate.of(2036, Month.JANUARY, 1));
    continuationEventCache.setApplicationId(UUID.randomUUID());
    continuationEventCache.setApplicationType(ApplicationType.CONTINUATION_APPLICATION);
    entityManager.persist(continuationEventCache);

    var noApplicationEventCache = new LicenceEventCache();
    noApplicationEventCache.setLicenceId(noApplicationLicence.getId());
    noApplicationEventCache.setLicenceReference(noApplicationLicence.getLicenceReference());
    noApplicationEventCache.setEventType(ScheduleEventType.TERM);
    noApplicationEventCache.setEventDate(LocalDate.of(2037, Month.JANUARY, 1));
    entityManager.persist(noApplicationEventCache);

    entityManager.flush();

    var form = new EventTrackerForm();
    form.setRequestTypes(List.of(EventTrackerRequestType.CONTINUATION.name()));

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, regulatorUser());

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("P 990");
  }

  @Test
  void getEventTrackerTable_whenDateFilterApplied_thenOnlyEventsWithinRangeReturned() {
    var earlyLicence = LicenceTestUtil.builder()
        .withId(9006)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("994")
        .withLicenceReference("P 994")
        .build();
    entityManager.persist(earlyLicence);

    var withinRangeLicence = LicenceTestUtil.builder()
        .withId(9007)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("993")
        .withLicenceReference("P 993")
        .build();
    entityManager.persist(withinRangeLicence);

    var lateLicence = LicenceTestUtil.builder()
        .withId(9008)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber("992")
        .withLicenceReference("P 992")
        .build();
    entityManager.persist(lateLicence);

    var earlyEventCache = new LicenceEventCache();
    earlyEventCache.setLicenceId(earlyLicence.getId());
    earlyEventCache.setLicenceReference(earlyLicence.getLicenceReference());
    earlyEventCache.setEventType(ScheduleEventType.TERM);
    earlyEventCache.setEventDate(LocalDate.of(2029, Month.DECEMBER, 31));
    entityManager.persist(earlyEventCache);

    var withinRangeEventCache = new LicenceEventCache();
    withinRangeEventCache.setLicenceId(withinRangeLicence.getId());
    withinRangeEventCache.setLicenceReference(withinRangeLicence.getLicenceReference());
    withinRangeEventCache.setEventType(ScheduleEventType.TERM);
    withinRangeEventCache.setEventDate(LocalDate.of(2030, Month.JUNE, 15));
    entityManager.persist(withinRangeEventCache);

    var lateEventCache = new LicenceEventCache();
    lateEventCache.setLicenceId(lateLicence.getId());
    lateEventCache.setLicenceReference(lateLicence.getLicenceReference());
    lateEventCache.setEventType(ScheduleEventType.TERM);
    lateEventCache.setEventDate(LocalDate.of(2031, Month.JANUARY, 1));
    entityManager.persist(lateEventCache);

    entityManager.flush();

    var form = new EventTrackerForm();
    form.setFromDate("01/01/2030");
    form.setToDate("31/12/2030");

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, regulatorUser());

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("P 993");
  }

  @Test
  void getEventTrackerTable_whenLicenseeFilterAppliedByRegulator_thenOnlyMatchingLicenseeReturned() {
    var matchingLicence = persistLicenceWithResponsibleOrganisation(9009, "991", "P 991", 700);
    var otherLicence = persistLicenceWithResponsibleOrganisation(9010, "990", "P 990", 800);

    persistEventCache(matchingLicence, LocalDate.of(2035, Month.JANUARY, 1));
    persistEventCache(otherLicence, LocalDate.of(2036, Month.JANUARY, 1));

    entityManager.flush();

    when(organisationApi.getOrganisationUnitsByIds(any(), any(), any(), any()))
        .thenReturn(List.of(organisationUnit(700, "Licensee A"), organisationUnit(800, "Licensee B")));

    var form = new EventTrackerForm();
    form.setLicenseeOrgUnitId(700);

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, regulatorUser());

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("P 991");
  }

  @Test
  void getEventTrackerTable_whenLicenseeGroupFilterAppliedByRegulator_thenOnlyMatchingLicenseeReturned() {
    var matchingLicence = persistLicenceWithResponsibleOrganisation(9014, "985", "P 985", 700);
    var otherLicence = persistLicenceWithResponsibleOrganisation(9015, "984", "P 984", 800);

    persistEventCache(matchingLicence, LocalDate.of(2042, Month.JANUARY, 1));
    persistEventCache(otherLicence, LocalDate.of(2043, Month.JANUARY, 1));

    entityManager.flush();

    when(organisationApi.getOrganisationUnitsByIds(any(), any(), any(), any()))
        .thenReturn(List.of(organisationUnit(700, "Licensee A"), organisationUnit(800, "Licensee B")));

    var organisationGroup = OrganisationGroup.newBuilder()
        .organisationGroupId(500)
        .name("Organisation group 500")
        .organisationUnits(List.of(organisationUnit(700, "Licensee A")))
        .build();
    when(organisationApi.getAllOrganisationGroupsByIds(any(), any(), any()))
        .thenReturn(List.of(organisationGroup));

    var form = new EventTrackerForm();
    form.setLicenseeOrgGroupId(500);

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, regulatorUser());

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("P 985");
  }

  @Test
  void getEventTrackerTable_whenIndustryUser_thenOnlySeesLicencesInOwnOrganisationGroupRegardlessOfFilter() {
    var ownLicence = persistLicenceWithResponsibleOrganisation(9011, "989", "P 989", 700);
    var otherLicence = persistLicenceWithResponsibleOrganisation(9012, "988", "P 988", 800);

    persistEventCache(ownLicence, LocalDate.of(2037, Month.JANUARY, 1));
    persistEventCache(otherLicence, LocalDate.of(2038, Month.JANUARY, 1));

    entityManager.flush();

    when(organisationApi.getOrganisationUnitsByIds(any(), any(), any(), any()))
        .thenReturn(List.of(organisationUnit(700, "Licensee A"), organisationUnit(800, "Licensee B")));
    grantOrganisationGroupAccess(industryUser.wuaId(), 500, 700);

    var form = new EventTrackerForm();
    form.setLicenseeOrgUnitId(800);

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, industryUser);

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("P 989");
  }

  @Test
  void getEventTrackerTable_whenIndustryUserHasNoOrganisationAccess_thenSeesNoRows() {
    var licence = persistLicenceWithResponsibleOrganisation(9013, "986", "P 986", 700);
    persistEventCache(licence, LocalDate.of(2039, Month.JANUARY, 1));

    entityManager.flush();

    when(organisationApi.getOrganisationUnitsByIds(any(), any(), any(), any()))
        .thenReturn(List.of(organisationUnit(700, "Licensee A")));

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), industryUser);

    assertThat(result.tableRows()).hasSize(1);
  }

  private void grantOrganisationGroupAccess(Long wuaId, int organisationGroupId, int organisationUnitId) {
    var team = new Team();
    team.setName("Organisation group %d".formatted(organisationGroupId));
    team.setTeamType(TeamType.ORGANISATION);
    team.setScopeType(ScopeType.ORGANISATION_GROUP.name());
    team.setScopeId(String.valueOf(organisationGroupId));
    entityManager.persist(team);

    var teamRole = new TeamRole();
    teamRole.setTeam(team);
    teamRole.setRole(Role.APPLICATION_SUBMITTER);
    teamRole.setWuaId(wuaId);
    entityManager.persist(teamRole);

    entityManager.flush();

    var organisationGroup = OrganisationGroup.newBuilder()
        .organisationGroupId(organisationGroupId)
        .name("Organisation group %d".formatted(organisationGroupId))
        .organisationUnits(List.of(organisationUnit(organisationUnitId, "Licensee A")))
        .build();

    when(organisationApi.getAllOrganisationGroupsByIds(any(), any(), any()))
        .thenReturn(List.of(organisationGroup));
  }

  private static OrganisationUnit organisationUnit(int organisationUnitId, String name) {
    var unit = new OrganisationUnit();
    unit.setOrganisationUnitId(organisationUnitId);
    unit.setName(name);
    return unit;
  }

  private Licence persistLicenceWithResponsibleOrganisation(
      int licenceId,
      String licenceNumber,
      String licenceReference,
      int organisationUnitId
  ) {
    var licence = LicenceTestUtil.builder()
        .withId(licenceId)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber(licenceNumber)
        .withLicenceReference(licenceReference)
        .build();
    entityManager.persist(licence);

    var responsibleOrganisation = new LicenceResponsibleOrganisation();
    responsibleOrganisation.setLicence(licence);
    responsibleOrganisation.setResponsibleOrganisationId(organisationUnitId);
    responsibleOrganisation.setManagedByLms(true);
    entityManager.persist(responsibleOrganisation);

    return licence;
  }

  private void persistEventCache(Licence licence, LocalDate eventDate) {
    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licence.getId());
    eventCache.setLicenceReference(licence.getLicenceReference());
    eventCache.setEventType(ScheduleEventType.TERM);
    eventCache.setEventDate(eventDate);
    entityManager.persist(eventCache);
  }

  @Test
  void jdbcBatchingIsEnabled() {
    // Cache rows claimed by an application (its term/phase row and any linked work programme activity
    // rows) are saved one entity at a time - genuinely necessary writes, not a redundant re-fetch. Without
    // JDBC batching each one is a separate round trip to the database; this guards against that regressing.
    var sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
    assertThat(sessionFactory.getSessionFactoryOptions().getJdbcBatchSize()).isGreaterThan(0);
  }

  private ServiceUserDetail regulatorUser() {
    var team = teamQueryService.getStaticTeam(TeamType.LICENCE_MANAGEMENT);

    var teamRole = new TeamRole();
    teamRole.setTeam(team);
    teamRole.setRole(Role.SCHEDULE_ADMINISTRATOR);
    teamRole.setWuaId(9200L);
    entityManager.persist(teamRole);

    return ServiceUserDetailTestUtil.newBuilder().withWuaId(9200L).build();
  }

  @Test
  void getEventTrackerTable_whenManyApplicationsPresent_thenQueryCountStaysConstant() {
    var statistics = entityManagerFactory.unwrap(SessionFactory.class).getStatistics();
    statistics.setStatisticsEnabled(true);

    for (int i = 0; i < 5; i++) {
      persistScheduleAmendmentApplicationWithEventCache(9500 + i, "SA" + i, "LMS/EAA/2040/" + i);
      persistContinuationApplicationWithTermsAndPhasesAndEventCache(9700 + i, "CA" + i, "LMS/CA/2040/" + i);
    }

    // Clears the first-level cache so this reflects a fresh request, rather than silently resolving eager
    // associations against entities this same test already loaded into the session moments earlier.
    entityManager.flush();
    entityManager.clear();
    statistics.clear();
    crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), regulatorUser());
    var queryCountForFiveApplications = statistics.getPrepareStatementCount();

    for (int i = 5; i < 20; i++) {
      persistScheduleAmendmentApplicationWithEventCache(9500 + i, "SA" + i, "LMS/EAA/2040/" + i);
      persistContinuationApplicationWithTermsAndPhasesAndEventCache(9700 + i, "CA" + i, "LMS/CA/2040/" + i);
    }

    entityManager.flush();
    entityManager.clear();
    statistics.clear();
    crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), regulatorUser());
    var queryCountForTwentyApplications = statistics.getPrepareStatementCount();

    assertThat(queryCountForTwentyApplications).isEqualTo(queryCountForFiveApplications);
  }

  private void persistContinuationApplicationWithTermsAndPhasesAndEventCache(
      int licenceId, String licenceNumber, String applicationReference
  ) {
    var licence = LicenceTestUtil.builder()
        .withId(licenceId)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber(licenceNumber)
        .withLicenceReference("P " + licenceNumber)
        .build();
    entityManager.persist(licence);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    entityManager.persist(licenceSchedule);

    var licenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule).build();
    entityManager.persist(licenceScheduleDetail);

    var currentTerm = persistTerm(licenceSchedule, licenceScheduleDetail, TermType.INITIAL);
    var nextTerm = persistTerm(licenceSchedule, licenceScheduleDetail, TermType.SECOND);
    var currentPhase = persistPhase(licenceSchedule, licenceScheduleDetail, currentTerm, PhaseType.PHASE_A);
    var nextPhase = persistPhase(licenceSchedule, licenceScheduleDetail, currentTerm, PhaseType.PHASE_B);

    var application = new LicenceContinuationApplication();
    application.setLicenceSchedule(licenceSchedule);
    application.setSubmittedLicenceScheduleDetail(licenceScheduleDetail);
    application.setApplicationReference(applicationReference);
    entityManager.persist(application);
    entityManager.flush();

    var applicationDetail = new LicenceContinuationApplicationDetail();
    applicationDetail.setLicenceContinuationApplication(application);
    applicationDetail.setVersionNumber(1);
    applicationDetail.setStatus(ApplicationStatus.SUBMITTED);
    applicationDetail.setCreatedDateTime(Instant.now());
    applicationDetail.setCurrentTerm(currentTerm);
    applicationDetail.setCurrentPhase(currentPhase);
    applicationDetail.setNextTerm(nextTerm);
    applicationDetail.setNextPhase(nextPhase);
    entityManager.persist(applicationDetail);

    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licence.getId());
    eventCache.setLicenceReference(licence.getLicenceReference());
    eventCache.setEventType(ScheduleEventType.PHASE);
    eventCache.setEventDate(LocalDate.of(2041, Month.JANUARY, 1));
    eventCache.setApplicationId(application.getId());
    eventCache.setApplicationType(ApplicationType.CONTINUATION_APPLICATION);
    entityManager.persist(eventCache);
    entityManager.flush();
  }

  private LicenceScheduleTerm persistTerm(
      LicenceSchedule licenceSchedule, LicenceScheduleDetail licenceScheduleDetail, TermType termType
  ) {
    var term = new LicenceScheduleTerm();
    term.setLicenceSchedule(licenceSchedule);
    term.setLicenceScheduleDetail(licenceScheduleDetail);
    term.setTermType(termType);
    term.setTermDuration(new ThreeFieldDuration(5, 0, 0));
    entityManager.persist(term);
    return term;
  }

  private LicenceSchedulePhase persistPhase(
      LicenceSchedule licenceSchedule,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleTerm term,
      PhaseType phaseType
  ) {
    var phase = new LicenceSchedulePhase();
    phase.setLicenceSchedule(licenceSchedule);
    phase.setLicenceScheduleDetail(licenceScheduleDetail);
    phase.setLicenceScheduleTerm(term);
    phase.setPhaseType(phaseType);
    phase.setPhaseDuration(new ThreeFieldDuration(1, 0, 0));
    entityManager.persist(phase);
    return phase;
  }

  private void persistScheduleAmendmentApplicationWithEventCache(
      int licenceId, String licenceNumber, String applicationReference
  ) {
    var licence = LicenceTestUtil.builder()
        .withId(licenceId)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceNumber(licenceNumber)
        .withLicenceReference("P " + licenceNumber)
        .build();
    entityManager.persist(licence);

    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(null, licence);
    entityManager.persist(licenceSchedule);

    var application = new ScheduleWorkProgrammeApplication();
    application.setLicenceSchedule(licenceSchedule);
    application.setApplicationReference(applicationReference);
    entityManager.persist(application);
    entityManager.flush();

    var applicationDetail = new ScheduleWorkProgrammeApplicationDetail();
    applicationDetail.setScheduleWorkProgrammeApplication(application);
    applicationDetail.setVersionNumber(1);
    applicationDetail.setStatus(ApplicationStatus.SUBMITTED);
    applicationDetail.setCreatedDatetime(Instant.now());
    entityManager.persist(applicationDetail);

    var eventCache = new LicenceEventCache();
    eventCache.setLicenceId(licence.getId());
    eventCache.setLicenceReference(licence.getLicenceReference());
    eventCache.setEventType(ScheduleEventType.TERM);
    eventCache.setEventDate(LocalDate.of(2040, Month.JANUARY, 1));
    eventCache.setApplicationId(application.getId());
    eventCache.setApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    entityManager.persist(eventCache);
    entityManager.flush();
  }
}
