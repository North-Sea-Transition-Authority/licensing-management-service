package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeApplicationWorkAreaServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceSearchService licenceSearchService;

  @InjectMocks
  private WorkProgrammeApplicationWorkAreaService workProgrammeApplicationWorkAreaService;

  @Test
  void getWorkAreaItems_unfiltered() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var testInstant = Instant.now();

    var licence1 = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS001")
        .build();
    var licenceSchedule1 = LicenceScheduleTestUtil.createLicenceSchedule(licence1);
    var licenceScheduleDetail1 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule1)
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant)
        .build();
    var scheduleWorkProgrammeApplication1 = ScheduleWorkProgrammeApplicationTestUtil
        .createScheduleWorkProgrammeApplication(licenceScheduleDetail1);
    var scheduleWorkProgrammeApplicationDetail1 = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail1.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplicationDetail1.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication1);
    scheduleWorkProgrammeApplicationDetail1.setCreatedDatetime(testInstant);
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail1))
        .thenReturn(licence1);

    var licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS002")
        .build();
    var licenceSchedule2 = LicenceScheduleTestUtil.createLicenceSchedule(licence2);
    var licenceScheduleDetail2 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule2)
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant.minus(1, ChronoUnit.HOURS))
        .build();
    var scheduleWorkProgrammeApplication2 = ScheduleWorkProgrammeApplicationTestUtil
        .createScheduleWorkProgrammeApplication(licenceScheduleDetail2);
    var scheduleWorkProgrammeApplicationDetail2 = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail2.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplicationDetail2.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication2);
    scheduleWorkProgrammeApplicationDetail2.setCreatedDatetime(testInstant);
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail2))
        .thenReturn(licence2);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);
    var orgList2 = List.of(org1);
    var licenceResponsibleOrgMap = Map.of(licence1, orgList1, licence2, orgList2);

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence1, licence2))).thenReturn(
        licenceResponsibleOrgMap
    );

    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(
            SearchResultItem::id,
            SearchResultItem::linkHeadingText,
            SearchResultItem::linkHeadingUrl,
            SearchResultItem::dataItemRows,
            SearchResultItem::transactionDatetime
        )
        .containsExactly(
            tuple(
                scheduleWorkProgrammeApplicationDetail1.getId().toString(),
                String.format("%s - schedule work programme application", licence1.getLicenceReference()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail1.getId(), null, null)),
                List.of(SummaryDataView.newBuilder()
                    .addStringValue("Licence type", licence1.getType().getDisplayName())
                    .addStringValue("Licensees", String.join(", ", orgList1))
                    .build()),
                testInstant
            ),
            tuple(
                scheduleWorkProgrammeApplicationDetail2.getId().toString(),
                String.format("%s - schedule work programme application", licence2.getLicenceReference()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail2.getId(), null, null)),
                List.of(SummaryDataView.newBuilder()
                    .addStringValue("Licence type", licence2.getType().getDisplayName())
                    .addStringValue("Licensees", String.join(", ", orgList2))
                    .build()),
                testInstant
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByLicenceReference() {
    var serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var testInstant = Instant.now();

    var licence1 = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS001")
        .build();
    var licenceSchedule1 = LicenceScheduleTestUtil.createLicenceSchedule(licence1);
    var licenceScheduleDetail1 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule1)
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant)
        .build();
    var scheduleWorkProgrammeApplication1 = ScheduleWorkProgrammeApplicationTestUtil
        .createScheduleWorkProgrammeApplication(licenceScheduleDetail1);
    var scheduleWorkProgrammeApplicationDetail1 = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail1.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplicationDetail1.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication1);
    scheduleWorkProgrammeApplicationDetail1.setCreatedDatetime(testInstant);
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail1))
        .thenReturn(licence1);

    var licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS002")
        .build();
    var licenceSchedule2 = LicenceScheduleTestUtil.createLicenceSchedule(licence2);
    var licenceScheduleDetail2 = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule2)
        .withId(UUID.randomUUID())
        .withCreatedInstant(testInstant)
        .build();
    var scheduleWorkProgrammeApplication2 = ScheduleWorkProgrammeApplicationTestUtil
        .createScheduleWorkProgrammeApplication(licenceScheduleDetail2);
    var scheduleWorkProgrammeApplicationDetail2 = new ScheduleWorkProgrammeApplicationDetail();
    scheduleWorkProgrammeApplicationDetail2.setId(UUID.randomUUID());
    scheduleWorkProgrammeApplicationDetail2.setScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication2);
    scheduleWorkProgrammeApplicationDetail2.setCreatedDatetime(testInstant);
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail2))
        .thenReturn(licence2);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    var org1 = "Org 1";
    var licenceResponsibleOrgMap = Map.of(licence2, List.of(org1));
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence2))).thenReturn(
        licenceResponsibleOrgMap
    );

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceReference("2");
    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(
            SearchResultItem::id,
            SearchResultItem::linkHeadingText,
            SearchResultItem::linkHeadingUrl,
            SearchResultItem::dataItemRows,
            SearchResultItem::transactionDatetime
        )
        .containsExactly(
            tuple(
                scheduleWorkProgrammeApplicationDetail2.getId().toString(),
                String.format("%s - schedule work programme application", licence2.getLicenceReference()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail2.getId(), null, null)),
                List.of(SummaryDataView.newBuilder()
                    .addStringValue("Licence type", licence2.getType().getDisplayName())
                    .addStringValue("Licensees", String.join(", ", org1))
                    .build()),
                testInstant
            )
        );
  }
}