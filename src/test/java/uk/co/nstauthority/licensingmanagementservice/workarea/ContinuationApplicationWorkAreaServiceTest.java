package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
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
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@ExtendWith(MockitoExtension.class)
class ContinuationApplicationWorkAreaServiceTest {

  @Mock
  private TeamQueryService teamQueryService;

  @Mock
  private LicenceContinuationService licenceContinuationService;

  @Mock
  private LicenceSearchService licenceSearchService;

  @Mock
  private ApplicationAccessService applicationAccessService;

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
    licenceContinuationApplicationDetail.getLicenceContinuationApplication().setApplicationReference("LMS/CONT/001");

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
    licenceContinuationApplicationDetail2.setStatus(LicenceContinuationApplicationStatus.SUBMITTED);
    licenceContinuationApplicationDetail2.setSubmittedDatetime(testInstant.plus(1, ChronoUnit.HOURS));
    licenceContinuationApplicationDetail2.getLicenceContinuationApplication().setApplicationReference("LMS/CONT/002");
  }

  @Test
  void getWorkAreaItems_unfiltered() {
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(licence1);

    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail2))
        .thenReturn(licence2);

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(),any(),any(),any())).thenReturn(true);

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);
    var orgList2 = List.of(org1);
    var licenceResponsibleOrgMap = Map.of(licence1, orgList1, licence2, orgList2);

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(any())).thenReturn(licenceResponsibleOrgMap);

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView1 = SummaryDataView
        .newBuilder()
        .addStringValue("Status", licenceContinuationApplicationDetail.getStatus().getDisplayName())
        .addStringValue("Licence type", licence1.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", orgList1))
        .build();
    var summaryDataView2 = SummaryDataView.newBuilder()
        .addStringValue("Status", licenceContinuationApplicationDetail2.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail.getId().toString(),
                String.format("%s - Licence continuation application", licence1.getLicenceReference()),
                ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(licenceContinuationApplicationDetail.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView1),
                testInstant
            ),
            tuple(
                licenceContinuationApplicationDetail2.getId().toString(),
                "LMS/CONT/002 - Licence continuation application",
                ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(licenceContinuationApplicationDetail2.getId(), null, null)),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.plus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView2),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByLicenceReference() {
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(licence1);
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail2))
        .thenReturn(licence2);

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(any(),any(),any(),any())).thenReturn(true);

    var org1 = "Org 1";
    var licenceResponsibleOrgMap = Map.of(licence2, List.of(org1));

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(any()))
        .thenReturn(licenceResponsibleOrgMap);

    var workAreaFilter = new WorkAreaFilterForm();
    workAreaFilter.setLicenceReference("2");
    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(workAreaFilter, serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Status", licenceContinuationApplicationDetail2.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail2.getId().toString(),
                "LMS/CONT/002 - Licence continuation application",
                ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(licenceContinuationApplicationDetail2.getId(), null, null)),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.plus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser() {
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(licence1);
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail2))
        .thenReturn(licence2);

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(applicationAccessService.userHasAccessToApplication(
        licenceContinuationApplicationDetail.getId().toString(),
        ApplicationType.CONTINUATION_APPLICATION,
        null,
        serviceUserDetail.wuaId()
    )).thenReturn(true);

    when(applicationAccessService.userHasAccessToApplication(
        licenceContinuationApplicationDetail2.getId().toString(),
        ApplicationType.CONTINUATION_APPLICATION,
        null,
        serviceUserDetail.wuaId()
    )).thenReturn(false);

    var org1 = "Org 1";
    var org2 = "Org 2";
    var orgList1 = List.of(org1, org2);
    var orgList2 = List.of(org1);
    var licenceResponsibleOrgMap = Map.of(licence1, orgList1, licence2, orgList2);

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(any()))
        .thenReturn(licenceResponsibleOrgMap);

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Status", licenceContinuationApplicationDetail.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail.getId().toString(),
                String.format("%s - Licence continuation application", licence1.getLicenceReference()),
                ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class).getTaskList(licenceContinuationApplicationDetail.getId(), null, null)),
                String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant)),
                List.of(summaryDataView),
                testInstant
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser_isContinuationReviewer() {
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(licence1);
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail2))
        .thenReturn(licence2);

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(teamQueryService.userHasAtLeastOneStaticRole(
        eq(serviceUserDetail.wuaId()),
        eq(TeamType.OFFSHORE_PRODUCTION_LICENSING),
        anySet()
    )).thenReturn(true);

    var org1 = "Org 1";
    var licenceResponsibleOrgMap = Map.of(licence2, List.of(org1));

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(any()))
        .thenReturn(licenceResponsibleOrgMap);

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Status", licenceContinuationApplicationDetail2.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail2.getId().toString(),
                "LMS/CONT/002 - Licence continuation application",
                ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(licenceContinuationApplicationDetail2.getId(), null, null)),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.plus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  @Test
  void getWorkAreaItems_filteredByUser_isContinuationIssuer() {
    licenceContinuationApplicationDetail2.setStatus(LicenceContinuationApplicationStatus.ISSUE_DECISION);
    licenceContinuationApplicationDetail2.setSubmittedDatetime(testInstant.plus(1, ChronoUnit.HOURS));

    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail))
        .thenReturn(licence1);
    when(licenceContinuationService.getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail2))
        .thenReturn(licence2);

    when(licenceContinuationService.getAllContinuationApplicationDetailsByStatuses(any()))
        .thenReturn(List.of(licenceContinuationApplicationDetail, licenceContinuationApplicationDetail2));

    when(teamQueryService.userHasAtLeastOneStaticRole(
        eq(serviceUserDetail.wuaId()),
        eq(TeamType.REGULATIONS_LICENSING),
        anySet()
    )).thenReturn(true);

    var org1 = "Org 1";
    var licenceResponsibleOrgMap = Map.of(licence2, List.of(org1));

    when(licenceSearchService.getLicenceToResponsibleOrganisationNameMap(any()))
        .thenReturn(licenceResponsibleOrgMap);

    var workAreaItems = continuationApplicationWorkAreaService.getWorkAreaItems(new WorkAreaFilterForm(), serviceUserDetail);

    var summaryDataView = SummaryDataView.newBuilder()
        .addStringValue("Status", licenceContinuationApplicationDetail2.getStatus().getDisplayName())
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
                licenceContinuationApplicationDetail2.getId().toString(),
                "LMS/CONT/002 - Licence continuation application",
                ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(ApplicationType.CONTINUATION_APPLICATION, licenceContinuationApplicationDetail2.getLicenceContinuationApplication().getId())),
                String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(testInstant.plus(1, ChronoUnit.HOURS))),
                List.of(summaryDataView),
                testInstant.minus(1, ChronoUnit.HOURS)
            )
        );
  }

  }
