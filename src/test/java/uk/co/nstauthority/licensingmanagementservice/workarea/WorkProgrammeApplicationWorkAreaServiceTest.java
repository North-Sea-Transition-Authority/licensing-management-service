package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
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
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
import uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam.LicenceTeam;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@ExtendWith(MockitoExtension.class)
class WorkProgrammeApplicationWorkAreaServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceSearchService licenceSearchService;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @Mock
  private TeamQueryService teamQueryService;

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

    licence1 = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P001")
        .build();
    scheduleWorkProgrammeApplicationDetail1 = createScheduleWorkProgrammeApplicationDetail(licence1, testInstant, "LMS/EEA/001");
    when(scheduleWorkProgrammeApplicationService
        .getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail1)).thenReturn(licence1);

    licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS002")
        .withResponsibleTeam(LicenceTeam.CS_CARBON_TRANSPORT_AND_STORAGE)
        .build();
    scheduleWorkProgrammeApplicationDetail2 = createScheduleWorkProgrammeApplicationDetail(licence2, testInstant.minus(1, ChronoUnit.HOURS), "LMS/EEA/002");
    when(scheduleWorkProgrammeApplicationService
        .getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail2)).thenReturn(licence2);
  }

  @Test
  void getWorkAreaItems_unfiltered() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
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
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail1.getStatus().getDisplayName())
        .addStringValue("Licence type", licence1.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .build();
    var summaryDataView2 = SummaryDataView.newBuilder()
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail2.getStatus().getDisplayName())
        .addStringValue("Licence type", licence2.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", orgList2))
        .build();

    assertThat(workAreaItems)
        .extracting(
            SearchResultItem::id,
            SearchResultItem::linkHeadingText,
            SearchResultItem::linkHeadingUrl,
            SearchResultItem::captionText,
            SearchResultItem::dataItemRows,
            SearchResultItem::transactionDatetime
        )
        .containsExactly(
            tuple(
                scheduleWorkProgrammeApplicationDetail1.getId().toString(),
                String.format("%s - schedule work programme application", licence1.getLicenceReference()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail1.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView1),
                testInstant
            ),
            tuple(
                scheduleWorkProgrammeApplicationDetail2.getId().toString(),
                String.format("%s - schedule work programme application", licence2.getLicenceReference()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail2.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.minus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView2),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_whenSubmitted_linksToOverview() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ScheduleWorkProgrammeApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any(), any())).thenReturn(true);
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence1, licence2)))
        .thenReturn(Map.of(licence1, List.of("Org 1"), licence2, List.of("Org 2")));

    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::linkHeadingUrl)
        .containsExactly(
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                .renderOverview(scheduleWorkProgrammeApplicationDetail1.getId(), null, null)),
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                .getTaskList(scheduleWorkProgrammeApplicationDetail2.getId(), null, null))
        );
  }

  @Test
  void getWorkAreaItems_whenIssueDecision_linksToLetterController() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ScheduleWorkProgrammeApplicationStatus.ISSUE_DECISION);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence1)))
        .thenReturn(Map.of(licence1, List.of("Org 1")));

    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::linkHeadingUrl)
        .containsExactly(
            ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(
                ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
                scheduleWorkProgrammeApplicationDetail1.getScheduleWorkProgrammeApplication().getId()
            ))
        );
  }

  @Test
  void getWorkAreaItems_filteredByLicenceReference() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
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
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail2.getStatus().getDisplayName())
        .addStringValue("Licence type", licence2.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", org1))
        .build();

    assertThat(workAreaItems)
        .extracting(
            SearchResultItem::id,
            SearchResultItem::linkHeadingText,
            SearchResultItem::linkHeadingUrl,
            SearchResultItem::captionText,
            SearchResultItem::dataItemRows,
            SearchResultItem::transactionDatetime
        )
        .containsExactly(
            tuple(
                scheduleWorkProgrammeApplicationDetail2.getId().toString(),
                String.format("%s - schedule work programme application", licence2.getLicenceReference()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail2.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.minus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser_userHasAccessToApplication() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);

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
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail1.getStatus().getDisplayName())
        .addStringValue("Licence type", licence1.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .build();

    assertThat(workAreaItems)
        .extracting(
            SearchResultItem::id,
            SearchResultItem::linkHeadingText,
            SearchResultItem::linkHeadingUrl,
            SearchResultItem::captionText,
            SearchResultItem::dataItemRows,
            SearchResultItem::transactionDatetime
        )
        .containsExactly(
            tuple(
                scheduleWorkProgrammeApplicationDetail1.getId().toString(),
                String.format("%s - schedule work programme application", licence1.getLicenceReference()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail1.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView),
                testInstant
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser_isCaseManager() {
    scheduleWorkProgrammeApplicationDetail2.setStatus(ScheduleWorkProgrammeApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail2.setSubmittedDatetime(testInstant.minus(1, ChronoUnit.HOURS));

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, false);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);
    when(teamQueryService.userHasStaticRole(
        serviceUserDetail.wuaId(),
        LicenceTeam.CS_CARBON_TRANSPORT_AND_STORAGE.getTeamType(),
        LicenceTeam.CS_CARBON_TRANSPORT_AND_STORAGE.getCaseManagerRole()
    )).thenReturn(true);

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);
    var orgList2 = List.of(org1);
    var licenceResponsibleOrgMap = Map.of(licence1, orgList1, licence2, orgList2);

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence2))).thenReturn(
        licenceResponsibleOrgMap
    );

    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail2.getStatus().getDisplayName())
        .addStringValue("Licence type", licence2.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", orgList2))
        .build();

    assertThat(workAreaItems)
        .extracting(
            SearchResultItem::id,
            SearchResultItem::linkHeadingText,
            SearchResultItem::linkHeadingUrl,
            SearchResultItem::captionText,
            SearchResultItem::dataItemRows,
            SearchResultItem::transactionDatetime
        )
        .containsExactly(
            tuple(
                scheduleWorkProgrammeApplicationDetail2.getId().toString(),
                "LMS/EEA/002 - schedule work programme application",
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                    .renderOverview(scheduleWorkProgrammeApplicationDetail2.getId(), null, null)),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.minus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser_isDecisionIssuer() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ScheduleWorkProgrammeApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);

    var org1 = "Org 1";
    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(List.of(licence1))).thenReturn(
        Map.of(licence1, List.of(org1))
    );

    var workAreaItems = workProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id, SearchResultItem::linkHeadingUrl)
        .containsExactly(
            tuple(
                scheduleWorkProgrammeApplicationDetail1.getId().toString(),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                    .renderOverview(scheduleWorkProgrammeApplicationDetail1.getId(), null, null))
            )
        );
  }

  private void mockUserHasAccessToApplication(ScheduleWorkProgrammeApplicationDetail applicationDetail,
                                              boolean hasAccess) {
    when(applicationAccessService.userHasAccessToApplication(
        applicationDetail.getScheduleWorkProgrammeApplication().getId().toString(),
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        applicationDetail.getResponsibleOrganisationUnitId(),
        serviceUserDetail.wuaId()
    )).thenReturn(hasAccess);
  }

  private ScheduleWorkProgrammeApplicationDetail createScheduleWorkProgrammeApplicationDetail(Licence licence, Instant time,
                                                                                              String applicationReference
  ) {
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
        .withStatus(ScheduleWorkProgrammeApplicationStatus.DRAFT)
        .withApplicationReference(applicationReference)
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();
  }
}