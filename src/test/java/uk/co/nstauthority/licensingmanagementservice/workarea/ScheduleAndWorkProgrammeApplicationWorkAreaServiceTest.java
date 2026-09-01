package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@ExtendWith(MockitoExtension.class)
class ScheduleAndWorkProgrammeApplicationWorkAreaServiceTest {

  @Mock
  private ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @Mock
  private WorkAreaItemViewService workAreaItemViewService;

  @Mock
  private RegulatorRoleService regulatorRoleService;

  @Mock
  private OrganisationGroupQueryService organisationGroupQueryService;

  @InjectMocks
  private ScheduleAndWorkProgrammeApplicationWorkAreaService scheduleAndWorkProgrammeApplicationWorkAreaService;

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

    licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.CARBON_STORAGE)
        .withLicenceReference("CS002")
        .build();
    scheduleWorkProgrammeApplicationDetail2 = createScheduleWorkProgrammeApplicationDetail(licence2, testInstant.minus(1, ChronoUnit.HOURS), "LMS/EEA/002");

    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any())).thenReturn(List.of());
  }

  @Test
  void getWorkAreaItems_unfiltered() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);
    var orgList2 = List.of(org1);

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(
            licence1, List.of(new OrganisationUnit(1, org1), new OrganisationUnit(2, org2)),
            licence2, List.of(new OrganisationUnit(3, org1))
        ));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView1 = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence1.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail1.getStatus().getDisplayName())
        .build();
    var summaryDataView2 = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence2.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", orgList2))
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail2.getStatus().getDisplayName())
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
                String.format("%s - %s", licence1.getLicenceReference(), ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail1.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView1),
                testInstant
            ),
            tuple(
                scheduleWorkProgrammeApplicationDetail2.getId().toString(),
                String.format("%s - %s", licence2.getLicenceReference(), ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName().toLowerCase()),
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
    scheduleWorkProgrammeApplicationDetail1.setStatus(ApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(
            licence1, List.of(new OrganisationUnit(1, "Org 1")),
            licence2, List.of(new OrganisationUnit(2, "Org 2"))
        ));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

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
  void getWorkAreaItems_whenSubmitted_mapsAllFieldsCorrectly() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, org1))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence1.getLicenceReference())
        .addStringValue("Licensees", org1)
        .addStringValue("Status", ApplicationStatus.SUBMITTED.getDisplayName())
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
                String.format("%s - %s",
                    scheduleWorkProgrammeApplicationDetail1.getScheduleWorkProgrammeApplication().getApplicationReference(),
                    ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                    .renderOverview(scheduleWorkProgrammeApplicationDetail1.getId(), null, null)),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView),
                testInstant
            )
        );
  }

  @Test
  void getWorkAreaItems_whenIssueDecision_IsDecisionIssuer_linksToLetterController() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ApplicationStatus.ISSUE_DECISION);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);
    mockIsDecisionIssuer();
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

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
  void getWorkAreaItems_whenIssueDecision_IsNotDecisionIssuer_linksToOverview() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ApplicationStatus.ISSUE_DECISION);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::linkHeadingUrl)
        .containsExactly(
            ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
                .renderOverview(scheduleWorkProgrammeApplicationDetail1.getId(), null, null))
        );
  }

  @Test
  void getWorkAreaItems_whenItemNotYetViewed_showsNewBadge() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(true);
  }

  @Test
  void getWorkAreaItems_whenItemAlreadyViewed_doesNotShowNewBadge() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any()))
        .thenReturn(List.of(new WorkAreaItemView(
            scheduleWorkProgrammeApplicationDetail1.getId(),
            WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION,
            serviceUserDetail.wuaId()
        )));
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(false);
  }

  @Test
  void getWorkAreaItems_whenDraftNotYetViewed_showsNewBadge() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(true);
  }

  @Test
  void getWorkAreaItems_whenDraftAlreadyViewed_doesNotShowNewBadge() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any()))
        .thenReturn(List.of(new WorkAreaItemView(
            scheduleWorkProgrammeApplicationDetail1.getId(),
            WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION,
            serviceUserDetail.wuaId()
        )));
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(false);
  }

  @Test
  void getWorkAreaItems_fetchesViewLogsOnceForAllItems_notPerItem() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);
    scheduleWorkProgrammeApplicationDetail2.setStatus(ApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail2.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(
            licence1, List.of(new OrganisationUnit(1, "Org 1")),
            licence2, List.of(new OrganisationUnit(2, "Org 2"))
        ));

    scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    // Single batched fetch regardless of item count (no N+1), and the per-item lookup is never used.
    verify(workAreaItemViewService, times(1)).getWorkAreaItemLogsForUser(any(), any());
    verify(workAreaItemViewService, never()).hasUserViewedItem(any());
  }

  private void mockIsDecisionIssuer() {
    when(regulatorRoleService.isDecisionIssuer(serviceUserDetail)).thenReturn(true);
  }

  @Test
  void getWorkAreaItems_filteredByLicenceReference() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, org1))));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceReference("2");
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence2.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", org1))
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail2.getStatus().getDisplayName())
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
                String.format("%s - %s", licence2.getLicenceReference(), ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail2.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.minus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByLicenceType() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, org1))));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name()));
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(scheduleWorkProgrammeApplicationDetail2.getId().toString());
  }

  @Test
  void getWorkAreaItems_filteredByLicenseeOrgUnitId_matching() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(
            licence1, List.of(new OrganisationUnit(1, "Org 1")),
            licence2, List.of(new OrganisationUnit(2, "Org 2"))
        ));
    when(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(any(), eq(licence1)))
        .thenReturn(List.of(1));
    when(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(any(), eq(licence2)))
        .thenReturn(List.of(2));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenseeOrgUnitId(2);
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(scheduleWorkProgrammeApplicationDetail2.getId().toString());
  }

  @Test
  void getWorkAreaItems_filteredByLicenseeOrgGroupId_matching() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(
            licence1, List.of(new OrganisationUnit(1, "Org 1")),
            licence2, List.of(new OrganisationUnit(2, "Org 2"))
        ));
    when(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(any(), eq(licence1)))
        .thenReturn(List.of(1));
    when(licenceResponsibleOrganisationService.getOrganisationUnitIdsFromLicenceOrgUnitMap(any(), eq(licence2)))
        .thenReturn(List.of(2));
    when(organisationGroupQueryService.getOrganisationUnitIdsByOrganisationGroupId(99))
        .thenReturn(List.of(1));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenseeOrgGroupId(99);
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(scheduleWorkProgrammeApplicationDetail1.getId().toString());
  }

  @Test
  void getWorkAreaItems_filteredByApplicationReference() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, org1))));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationReference("001");
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(scheduleWorkProgrammeApplicationDetail1.getId().toString());
  }

  @Test
  void getWorkAreaItems_whenDraftHasNoApplicationReferenceYet_andApplicationReferenceFilterSet_thenExcludedWithoutError() {
    scheduleWorkProgrammeApplicationDetail1.getScheduleWorkProgrammeApplication().setApplicationReference(null);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationReference("EEA");
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_filteredByApplicationType_matching() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(
            licence1, List.of(new OrganisationUnit(1, "Org 1")),
            licence2, List.of(new OrganisationUnit(2, "Org 2"))
        ));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationTypes(List.of(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name()));
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactlyInAnyOrder(
            scheduleWorkProgrammeApplicationDetail1.getId().toString(),
            scheduleWorkProgrammeApplicationDetail2.getId().toString()
        );
  }

  @Test
  void getWorkAreaItems_filteredByApplicationType_nonMatching_thenExcluded() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationTypes(List.of(ApplicationType.CONTINUATION_APPLICATION.name()));
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_filteredByApplicationStatus_matching() {
    scheduleWorkProgrammeApplicationDetail2.setStatus(ApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail2.setSubmittedDatetime(testInstant.minus(1, ChronoUnit.HOURS));

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, org1))));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationStatuses(List.of(ApplicationStatus.SUBMITTED.name()));
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(scheduleWorkProgrammeApplicationDetail2.getId().toString());
  }

  @Test
  void getWorkAreaItems_filteredByApplicationStatus_nonMatching_thenExcluded() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationStatuses(List.of(ApplicationStatus.ISSUE_DECISION.name()));
    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
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

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, org1), new OrganisationUnit(2, org2))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence1.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .addStringValue("Status", scheduleWorkProgrammeApplicationDetail1.getStatus().getDisplayName())
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
                String.format("%s - %s", licence1.getLicenceReference(), ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
                    .getTaskList(scheduleWorkProgrammeApplicationDetail1.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView),
                testInstant
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser_isDecisionIssuer() {
    scheduleWorkProgrammeApplicationDetail1.setStatus(ApplicationStatus.SUBMITTED);
    scheduleWorkProgrammeApplicationDetail1.setSubmittedDatetime(testInstant);

    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));

    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail1, true);
    mockUserHasAccessToApplication(scheduleWorkProgrammeApplicationDetail2, false);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, org1))));

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

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

  @Test
  void getWorkAreaItems_whenDraftAndIsRegulator_isExcluded() {
    when(scheduleWorkProgrammeApplicationService.getAllScheduleWorkProgrammeApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(scheduleWorkProgrammeApplicationDetail1, scheduleWorkProgrammeApplicationDetail2));
    when(applicationAccessService.userHasAccessToApplication(
        any(LicenceApplicationDetail.class), anyMap(), any(Long.class)
    )).thenReturn(true);
    when(regulatorRoleService.isRegulator(serviceUserDetail)).thenReturn(true);

    var workAreaItems = scheduleAndWorkProgrammeApplicationWorkAreaService
        .getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  private void mockUserHasAccessToApplication(ScheduleWorkProgrammeApplicationDetail applicationDetail,
                                              boolean hasAccess) {
    when(applicationAccessService.userHasAccessToApplication(
        eq(applicationDetail),
        anyMap(),
        eq(serviceUserDetail.wuaId())
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
        .withStatus(ApplicationStatus.DRAFT)
        .withApplicationReference(applicationReference)
        .withScheduleWorkProgrammeApplication(scheduleWorkProgrammeApplication)
        .build();
  }
}
