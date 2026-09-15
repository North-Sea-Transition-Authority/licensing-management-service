package uk.co.nstauthority.licensingmanagementservice.licence.crosslicenceeventtracker;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.ScheduleEventType;
import uk.co.nstauthority.licensingmanagementservice.util.IntegrationTest;

@Transactional
@IntegrationTest
class CrossLicenceEventTrackerServiceIntegrationTest {

  @Autowired
  private EntityManager entityManager;

  @Autowired
  private CrossLicenceEventTrackerService crossLicenceEventTrackerService;

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

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm());

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

    var result = crossLicenceEventTrackerService.getEventTrackerTable(new EventTrackerForm());

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

    var result = crossLicenceEventTrackerService.getEventTrackerTable(form);

    assertThat(result.tableRows()).hasSize(2);
    assertThat(result.tableRows().get(1).rowValues().get(0).value()).isEqualTo("CS 995");
  }
}
