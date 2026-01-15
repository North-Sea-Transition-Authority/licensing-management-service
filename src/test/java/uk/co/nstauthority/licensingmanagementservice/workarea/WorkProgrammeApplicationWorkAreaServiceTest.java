package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
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

  @Mock
  private ApplicationAccessService applicationAccessService;

  @InjectMocks
  private WorkProgrammeApplicationWorkAreaService workProgrammeApplicationWorkAreaService;

  private Licence licence1, licence2;
  private ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2;
  private ServiceUserDetail serviceUserDetail;
  private Instant testInstant;

  @BeforeEach
  void setUp() {
    testInstant = Instant.now();
    serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();

    licence1 = LicenceTestUtil.builder().withId(1).withLicenceType(LicenceType.CARBON_STORAGE).
      withLicenceReference("CS001").build();
    scheduleWorkProgrammeApplicationDetail1 = createScheduleWorkProgrammeApplicationDetail(licence1, testInstant);

    licence2 = LicenceTestUtil.builder().withId(2).withLicenceType(LicenceType.CARBON_STORAGE).withLicenceReference("CS002").build();
    scheduleWorkProgrammeApplicationDetail2 = createScheduleWorkProgrammeApplicationDetail(licence2, testInstant.minus(1, ChronoUnit.HOURS));
  }

  @Test
  void getWorkAreaItems_unfiltered() {
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail1))
        .thenReturn(licence1);

    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail2))
        .thenReturn(licence2);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(),any(),any(),any())).thenReturn(true);

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);
    var orgList2 = List.of(org1);
    var licenceResponsibleOrgMap = Map.of(licence1, orgList1, licence2, orgList2);

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence1, licence2))).thenReturn(
        licenceResponsibleOrgMap
    );

    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView1 = SummaryDataView.newBuilder()
        .addStringValue("Licence type", licence1.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .build();
    var summaryDataView2 = SummaryDataView.newBuilder()
        .addStringValue("Licence type", licence2.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", orgList2))
        .build();

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
                List.of(summaryDataView1),
                testInstant
            ),
            tuple(
                scheduleWorkProgrammeApplicationDetail2.getId().toString(),
                String.format("%s - schedule work programme application", licence2.getLicenceReference()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail2.getId(), null, null)),
                List.of(summaryDataView2),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByLicenceReference() {
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail2))
        .thenReturn(licence2);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(),any(),any(),any())).thenReturn(true);

    var org1 = "Org 1";
    var licenceResponsibleOrgMap = Map.of(licence2, List.of(org1));
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence2))).thenReturn(
        licenceResponsibleOrgMap
    );

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceReference("2");
    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence type", licence2.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", org1))
        .build();

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
                List.of(summaryDataView),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser() {
    when(scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail1))
        .thenReturn(licence1);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

      when(applicationAccessService.userHasAccessToApplication(
              scheduleWorkProgrammeApplicationDetail1.getScheduleWorkProgrammeApplication().getId().toString(),
              ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
              scheduleWorkProgrammeApplicationDetail1.getResponsibleOrganisationUnitId(),
              serviceUserDetail.wuaId()
      )).thenReturn(true);

      when(applicationAccessService.userHasAccessToApplication(
              scheduleWorkProgrammeApplicationDetail2.getScheduleWorkProgrammeApplication().getId().toString(),
              ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
              scheduleWorkProgrammeApplicationDetail2.getResponsibleOrganisationUnitId(),
              serviceUserDetail.wuaId()
    )).thenReturn(false);

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);
    var orgList2 = List.of(org1);
    var licenceResponsibleOrgMap = Map.of(licence1, orgList1, licence2, orgList2);

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence1))).thenReturn(
        licenceResponsibleOrgMap
    );

    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence type", licence1.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .build();

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
                List.of(summaryDataView),
                testInstant
            )
        );
  }

  private ScheduleWorkProgrammeApplicationDetail createScheduleWorkProgrammeApplicationDetail(Licence licence, Instant time) {
    var licenceSchedule = LicenceScheduleTestUtil.createLicenceSchedule(licence);
    var licenceScheduleDetail = LicenceScheduleTestUtil.licenceScheduleDetailBuilder(licenceSchedule)
        .withId(UUID.randomUUID())
        .withCreatedInstant(time)
        .build();

    var scheduleWorkProgrammeApplication = ScheduleWorkProgrammeApplicationDetailTestUtil.createScheduleWorkProgrammeApplication(licenceScheduleDetail);
    return ScheduleWorkProgrammeApplicationDetailTestUtil
        .builder()
        .withId(UUID.randomUUID())
        .withCreatedDate(time)
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();
  }
}