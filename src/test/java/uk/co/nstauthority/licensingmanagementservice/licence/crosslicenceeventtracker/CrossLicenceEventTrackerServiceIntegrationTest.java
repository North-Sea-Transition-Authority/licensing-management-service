package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.fivium.energyportalapi.client.organisation.OrganisationApi;
import uk.co.fivium.energyportalapi.generated.types.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisation;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class CrossLicenceEventTrackerServiceIntegrationTest {

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private CrossLicenceEventTrackerService crossLicenceEventTrackerService;

  @Autowired
  private TeamQueryService teamQueryService;

  @MockitoBean
  private OrganisationApi organisationApi;

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
    eventCache.setEventDate(LocalDate.of(2030, 1, 1));
    eventCache.setQuadBlock("12/3");
    entityManager.persist(eventCache);

    entityManager.flush();

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), industryUser);

    assertThat(result.tableRows()).hasSize(2);

    var row = result.tableRows().get(1).rowValues();
    assertThat(row.get(0).value()).isEqualTo("P 999");
    assertThat(row.get(1).value()).isEqualTo("Initial Term to Second Term");
    assertThat(row.get(3).value()).isEqualTo(DateFormatUtil.convertToDisplayText(LocalDate.of(2030, 1, 1)));
    assertThat(row.get(3).sortValue()).isEqualTo("2030-01-01");
    assertThat(row.get(5).value()).isEqualTo("");
    assertThat(row.get(6).value()).isEqualTo("12/3");
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
    laterEventCache.setEventDate(LocalDate.of(2032, 1, 1));
    entityManager.persist(laterEventCache);

    var earlierEventCache = new LicenceEventCache();
    earlierEventCache.setLicenceId(earlierLicence.getId());
    earlierEventCache.setLicenceReference(earlierLicence.getLicenceReference());
    earlierEventCache.setEventType(ScheduleEventType.TERM);
    earlierEventCache.setEventDate(LocalDate.of(2031, 1, 1));
    entityManager.persist(earlierEventCache);

    entityManager.flush();

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm(), industryUser);

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
    seawardEventCache.setEventDate(LocalDate.of(2033, 1, 1));
    entityManager.persist(seawardEventCache);

    var carbonStorageEventCache = new LicenceEventCache();
    carbonStorageEventCache.setLicenceId(carbonStorageLicence.getId());
    carbonStorageEventCache.setLicenceReference(carbonStorageLicence.getLicenceReference());
    carbonStorageEventCache.setEventType(ScheduleEventType.TERM);
    carbonStorageEventCache.setEventDate(LocalDate.of(2034, 1, 1));
    entityManager.persist(carbonStorageEventCache);

    entityManager.flush();

    var form = new EventTrackerForm();
    form.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name()));

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, industryUser);

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("CS 995");
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
    earlyEventCache.setEventDate(LocalDate.of(2029, 12, 31));
    entityManager.persist(earlyEventCache);

    var withinRangeEventCache = new LicenceEventCache();
    withinRangeEventCache.setLicenceId(withinRangeLicence.getId());
    withinRangeEventCache.setLicenceReference(withinRangeLicence.getLicenceReference());
    withinRangeEventCache.setEventType(ScheduleEventType.TERM);
    withinRangeEventCache.setEventDate(LocalDate.of(2030, 6, 15));
    entityManager.persist(withinRangeEventCache);

    var lateEventCache = new LicenceEventCache();
    lateEventCache.setLicenceId(lateLicence.getId());
    lateEventCache.setLicenceReference(lateLicence.getLicenceReference());
    lateEventCache.setEventType(ScheduleEventType.TERM);
    lateEventCache.setEventDate(LocalDate.of(2031, 1, 1));
    entityManager.persist(lateEventCache);

    entityManager.flush();

    var form = new EventTrackerForm();
    form.setFromDate("01/01/2030");
    form.setToDate("31/12/2030");

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, industryUser);

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("P 993");
  }

  @Test
  void getEventTrackerTable_whenLicenseeFilterAppliedByRegulator_thenOnlyMatchingLicenseeReturned() {
    var matchingLicence = persistLicenceWithResponsibleOrganisation(9009, "991", "P 991", 700);
    var otherLicence = persistLicenceWithResponsibleOrganisation(9010, "990", "P 990", 800);

    persistEventCache(matchingLicence, LocalDate.of(2035, 1, 1));
    persistEventCache(otherLicence, LocalDate.of(2036, 1, 1));

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
  void getEventTrackerTable_whenLicenseeFilterAppliedByIndustryUser_thenFilterIsIgnored() {
    var matchingLicence = persistLicenceWithResponsibleOrganisation(9011, "989", "P 989", 700);
    var otherLicence = persistLicenceWithResponsibleOrganisation(9012, "988", "P 988", 800);

    persistEventCache(matchingLicence, LocalDate.of(2037, 1, 1));
    persistEventCache(otherLicence, LocalDate.of(2038, 1, 1));

    entityManager.flush();

    when(organisationApi.getOrganisationUnitsByIds(any(), any(), any(), any()))
        .thenReturn(List.of(organisationUnit(700, "Licensee A"), organisationUnit(800, "Licensee B")));

    var form = new EventTrackerForm();
    form.setLicenseeOrgUnitId(700);

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form, industryUser);

    assertThat(result.tableRows()).hasSize(3);
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

  private ServiceUserDetail regulatorUser() {
    var team = teamQueryService.getStaticTeam(TeamType.LICENCE_MANAGEMENT);

    var teamRole = new TeamRole();
    teamRole.setTeam(team);
    teamRole.setRole(Role.SCHEDULE_ADMINISTRATOR);
    teamRole.setWuaId(9200L);
    entityManager.persist(teamRole);

    return ServiceUserDetailTestUtil.newBuilder().withWuaId(9200L).build();
  }
}
