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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@ExtendWith(MockitoExtension.class)
class ContinuationApplicationWorkAreaServiceTest {

  @Mock
  private RegulatorRoleService regulatorRoleService;

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  @Mock
  private ApplicationAccessService applicationAccessService;

  @Mock
  private WorkAreaItemViewService workAreaItemViewService;

  @InjectMocks
  private ContinuationApplicationWorkAreaService continuationApplicationWorkAreaService;

  private Licence licence1, licence2;
  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2;
  private ServiceUserDetail serviceUserDetail;
  private Instant testInstant;

  @BeforeEach
  void setUp() {
    testInstant = Instant.now();
    serviceUserDetail = ServiceUserDetailTestUtil.newBuilder().build();

    licence1 = LicenceTestUtil.builder()
        .withId(1)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P1")
        .build();

    var scheduleDetail1 = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence1)
    );

    licenceContinuationApplicationDetail = LicenceContinuationApplicationTestUtil
        .createLicenceContinuationApplicationDetail(scheduleDetail1);
    licenceContinuationApplicationDetail.setCreatedDateTime(testInstant);
    licenceContinuationApplicationDetail.getLicenceContinuationApplication().setApplicationReference("LMS/CA/001");

    licence2 = LicenceTestUtil.builder()
        .withId(2)
        .withLicenceType(LicenceType.SEAWARD_PRODUCTION)
        .withLicenceReference("P2")
        .build();

    var scheduleDetail2 = LicenceScheduleTestUtil.createLicenceScheduleDetail(
        LicenceScheduleTestUtil.createLicenceSchedule(licence2)
    );

    licenceContinuationApplicationDetail2 = LicenceContinuationApplicationTestUtil
        .createLicenceContinuationApplicationDetail(scheduleDetail2);
    licenceContinuationApplicationDetail2.setCreatedDateTime(testInstant.minus(1, ChronoUnit.HOURS));
    licenceContinuationApplicationDetail2.setStatus(ApplicationStatus.SUBMITTED);
    licenceContinuationApplicationDetail2.setSubmittedDatetime(testInstant.plus(1, ChronoUnit.HOURS));
    licenceContinuationApplicationDetail2.getLicenceContinuationApplication().setApplicationReference("LMS/CA/002");

    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any())).thenReturn(List.of());
  }

  @Test
  void getWorkAreaItems_unfiltered() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

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

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView1 = SummaryDataView
        .newBuilder()
        .addStringValue("Licence", licence1.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .addStringValue("Status", licenceContinuationApplicationDetail.getStatus().getDisplayName())
        .build();
    var summaryDataView2 = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence2.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", orgList2))
        .addStringValue("Status", licenceContinuationApplicationDetail2.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail.getId().toString(),
                String.format("%s - %s", licence1.getLicenceReference(), ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(licenceContinuationApplicationDetail.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView1),
                testInstant
            ),
            tuple(
                licenceContinuationApplicationDetail2.getId().toString(),
                String.format("%s - %s", "LMS/CA/002", ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(licenceContinuationApplicationDetail2.getId(), null, null, null)),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.plus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView2),
                testInstant.plus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByLicenceReference() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var org1 = "Org 1";

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(3, org1))));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceReference("2");
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence2.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", org1))
        .addStringValue("Status", licenceContinuationApplicationDetail2.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail2.getId().toString(),
                String.format("%s - %s", "LMS/CA/002", ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(licenceContinuationApplicationDetail2.getId(), null, null, null)),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.plus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.plus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByLicenceType() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    licence1.setType(LicenceType.CARBON_STORAGE);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, org1))));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceTypes(List.of(LicenceType.CARBON_STORAGE.name()));
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(licenceContinuationApplicationDetail.getId().toString());
  }

  @Test
  void getWorkAreaItems_filteredByApplicationReference() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, org1))));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationReference("002");
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(licenceContinuationApplicationDetail2.getId().toString());
  }

  @Test
  void getWorkAreaItems_whenDraftHasNoApplicationReferenceYet_andApplicationReferenceFilterSet_thenExcludedWithoutError() {
    licenceContinuationApplicationDetail.getLicenceContinuationApplication().setApplicationReference(null);

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationReference("CA");
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_filteredByApplicationType_matching() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(
            licence1, List.of(new OrganisationUnit(1, "Org 1")),
            licence2, List.of(new OrganisationUnit(2, "Org 2"))
        ));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationTypes(List.of(ApplicationType.CONTINUATION_APPLICATION.name()));
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactlyInAnyOrder(
            licenceContinuationApplicationDetail.getId().toString(),
            licenceContinuationApplicationDetail2.getId().toString()
        );
  }

  @Test
  void getWorkAreaItems_filteredByApplicationType_nonMatching_thenExcluded() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationTypes(List.of(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.name()));
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_filteredByApplicationStatus_matching() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(true);

    var org1 = "Org 1";
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, org1))));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationStatuses(List.of(ApplicationStatus.SUBMITTED.name()));
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::id)
        .containsExactly(licenceContinuationApplicationDetail2.getId().toString());
  }

  @Test
  void getWorkAreaItems_filteredByApplicationStatus_nonMatching_thenExcluded() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setApplicationStatuses(List.of(ApplicationStatus.ISSUE_DECISION.name()));
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_filteredByUser() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(
        eq(licenceContinuationApplicationDetail),
        anyMap(),
        eq(serviceUserDetail.wuaId())
    )).thenReturn(true);

    when(applicationAccessService.userHasAccessToApplication(
        eq(licenceContinuationApplicationDetail2),
        anyMap(),
        eq(serviceUserDetail.wuaId())
    )).thenReturn(false);

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(
            licence1, List.of(new OrganisationUnit(1, org1), new OrganisationUnit(2, org2)),
            licence2, List.of(new OrganisationUnit(3, org1))
        ));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence1.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .addStringValue("Status", licenceContinuationApplicationDetail.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail.getId().toString(),
                String.format("%s - %s", licence1.getLicenceReference(), ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(licenceContinuationApplicationDetail.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView),
                testInstant
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser_isContinuationReviewer() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(regulatorRoleService.isContinuationReviewer(serviceUserDetail)).thenReturn(true);

    var org1 = "Org 1";

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, org1))));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence2.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", org1))
        .addStringValue("Status", licenceContinuationApplicationDetail2.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail2.getId().toString(),
                String.format("%s - %s", "LMS/CA/002", ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(licenceContinuationApplicationDetail2.getId(), null, null, null)),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.plus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.plus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser_isContinuationIssuer() {
    licenceContinuationApplicationDetail2.setStatus(ApplicationStatus.ISSUE_DECISION);
    licenceContinuationApplicationDetail2.setSubmittedDatetime(testInstant.plus(1, ChronoUnit.HOURS));

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(regulatorRoleService.isContinuationIssuer(serviceUserDetail)).thenReturn(true);

    var org1 = "Org 1";

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, org1))));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence2.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", org1))
        .addStringValue("Status", licenceContinuationApplicationDetail2.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail2.getId().toString(),
                String.format("%s - %s", "LMS/CA/002", ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase()),
                ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(ApplicationType.CONTINUATION_APPLICATION, licenceContinuationApplicationDetail2.getLicenceContinuationApplication().getId())),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.plus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.plus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_whenIssueDecision_isNotContinuationIssuer_linksToOverview() {
    licenceContinuationApplicationDetail2.setStatus(ApplicationStatus.ISSUE_DECISION);

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(
        eq(licenceContinuationApplicationDetail),
        anyMap(),
        eq(serviceUserDetail.wuaId())
    )).thenReturn(false);
    when(applicationAccessService.userHasAccessToApplication(
        eq(licenceContinuationApplicationDetail2),
        anyMap(),
        eq(serviceUserDetail.wuaId())
    )).thenReturn(true);

    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::linkHeadingUrl)
        .containsExactly(
            ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
                .renderOverview(licenceContinuationApplicationDetail2.getId(), null, null, null))
        );
  }

  @Test
  void getWorkAreaItems_whenIssueDecision_isContinuationReviewerButNotIssuer_isExcluded() {
    licenceContinuationApplicationDetail2.setStatus(ApplicationStatus.ISSUE_DECISION);

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(), any(), any())).thenReturn(false);
    when(regulatorRoleService.isContinuationReviewer(serviceUserDetail)).thenReturn(true);

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_whenItemNotYetViewed_showsNewBadge() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));
    when(regulatorRoleService.isContinuationReviewer(serviceUserDetail)).thenReturn(true);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(true);

    verify(workAreaItemViewService, times(1)).getWorkAreaItemLogsForUser(any(), any());
    verify(workAreaItemViewService, never()).hasUserViewedItem(any());
  }

  @Test
  void getWorkAreaItems_whenItemAlreadyViewed_doesNotShowNewBadge() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));
    when(regulatorRoleService.isContinuationReviewer(serviceUserDetail)).thenReturn(true);
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any()))
        .thenReturn(List.of(new WorkAreaItemView(
            licenceContinuationApplicationDetail2.getId(),
            WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION,
            serviceUserDetail.wuaId()
        )));
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(false);
  }

  @Test
  void getWorkAreaItems_whenDraftApplicationAlreadyViewed_doesNotShowNewBadge() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));
    when(applicationAccessService.userHasAccessToApplication(
        eq(licenceContinuationApplicationDetail),
        anyMap(),
        eq(serviceUserDetail.wuaId())
    )).thenReturn(true);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, "Org 1"))));
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any()))
        .thenReturn(List.of(new WorkAreaItemView(
            licenceContinuationApplicationDetail.getId(),
            WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION,
            serviceUserDetail.wuaId()
        )));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(false);
  }

  @Test
  void getWorkAreaItems_whenDraftApplicationNotYetViewed_showsNewBadge() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));
    when(applicationAccessService.userHasAccessToApplication(
        eq(licenceContinuationApplicationDetail),
        anyMap(),
        eq(serviceUserDetail.wuaId())
    )).thenReturn(true);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence1, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(true);
  }

  @Test
  void getWorkAreaItems_whenIssueDecisionItemNotYetViewed_showsNewBadge() {
    licenceContinuationApplicationDetail2.setStatus(ApplicationStatus.ISSUE_DECISION);
    licenceContinuationApplicationDetail2.setSubmittedDatetime(testInstant.plus(1, ChronoUnit.HOURS));

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));
    when(regulatorRoleService.isContinuationIssuer(serviceUserDetail)).thenReturn(true);
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(true);
  }

  @Test
  void getWorkAreaItems_whenDraftAndUserIsRegulator_isExcluded() {
    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(anySet()))
        .thenReturn(List.of(licenceContinuationApplicationDetail));
    when(applicationAccessService.userHasAccessToApplication(
        any(LicenceApplicationDetail.class), anyMap(), any(Long.class)
    )).thenReturn(true);
    when(regulatorRoleService.isRegulator(serviceUserDetail)).thenReturn(true);

    var workAreaItems = continuationApplicationWorkAreaService
        .getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems).isEmpty();
  }

  @Test
  void getWorkAreaItems_whenIssueDecisionItemAlreadyViewed_doesNotShowNewBadge() {
    licenceContinuationApplicationDetail2.setStatus(ApplicationStatus.ISSUE_DECISION);
    licenceContinuationApplicationDetail2.setSubmittedDatetime(testInstant.plus(1, ChronoUnit.HOURS));

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));
    when(regulatorRoleService.isContinuationIssuer(serviceUserDetail)).thenReturn(true);
    // Issuer views are logged against the application id, not the detail id
    when(workAreaItemViewService.getWorkAreaItemLogsForUser(any(), any()))
        .thenReturn(List.of(new WorkAreaItemView(
            licenceContinuationApplicationDetail2.getLicenceContinuationApplication().getId(),
            WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION,
            serviceUserDetail.wuaId()
        )));
    when(licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(any()))
        .thenReturn(Map.of(licence2, List.of(new OrganisationUnit(1, "Org 1"))));

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    assertThat(workAreaItems)
        .extracting(SearchResultItem::hasNewLabel)
        .containsExactly(false);
  }

}
