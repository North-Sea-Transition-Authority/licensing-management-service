package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;

@Service
public class WorkProgrammeApplicationWorkAreaService implements WorkAreaItemProvider {

  public static final Set<ScheduleWorkProgrammeApplicationStatus> ACTIVE_APPLICATION_STATUSES = Set.of(
      ScheduleWorkProgrammeApplicationStatus.DRAFT,
      ScheduleWorkProgrammeApplicationStatus.SUBMITTED);
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceSearchService licenceSearchService;
  private final ApplicationAccessService applicationAccessService;
  private final TeamQueryService teamQueryService;

  public WorkProgrammeApplicationWorkAreaService(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceSearchService licenceSearchService,
      ApplicationAccessService applicationAccessService,
      TeamQueryService teamQueryService) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceSearchService = licenceSearchService;
    this.applicationAccessService = applicationAccessService;
    this.teamQueryService = teamQueryService;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    //TODO filter correctly by form and user
    var applicationDetails = scheduleWorkProgrammeApplicationService
        .getAllScheduleWorkProgrammeApplicationDetailsByStatuses(ACTIVE_APPLICATION_STATUSES).stream()
        .filter(applicationDetail -> matchesFilterAndHasAccess(applicationDetail, workAreaFilterForm, serviceUserDetail))
        .toList();

    var licences = applicationDetails.stream()
        .map(scheduleWorkProgrammeApplicationService::getLicenceFromScheduleWorkProgrammeApplicationDetail)
        .toList();

    var responsibleOrganisationNames = licenceSearchService.getLicenceToResponsibleOrganisationNameMap(licences);

    return applicationDetails.stream()
        .map(applicationDetail -> createWorkAreaItem(applicationDetail, responsibleOrganisationNames))
        .toList();
  }

  private SearchResultItem createWorkAreaItem(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail,
      Map<Licence, List<String>> responsibleOrganisationNamesByLicences
  ) {
    var licence = scheduleWorkProgrammeApplicationService
        .getLicenceFromScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail);
    var createdDatetime = scheduleWorkProgrammeApplicationDetail.getCreatedDatetime();
    var licensees = responsibleOrganisationNamesByLicences.getOrDefault(
            licence,
            List.of()
        )
        .stream()
        .filter(Objects::nonNull)
        .toList();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence type", licence.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", licensees))
        .build();

    var linkHeadingUrl = scheduleWorkProgrammeApplicationDetail.getStatus() == ScheduleWorkProgrammeApplicationStatus.SUBMITTED
        ? ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
            .renderOverview(scheduleWorkProgrammeApplicationDetail.getId(), null, null))
        : ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
            .getTaskList(scheduleWorkProgrammeApplicationDetail.getId(), null, null));

    return SearchResultItem.newBuilder()
        .withId(scheduleWorkProgrammeApplicationDetail.getId().toString())
        .withLinkHeadingText(String.format("%s - schedule work programme application", licence.getLicenceReference()))
        .withLinkHeadingUrl(linkHeadingUrl)
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdDatetime)))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(createdDatetime)
        .build();
  }

  private boolean isCaseManager(ServiceUserDetail userDetail, Licence licence) {
    var responsibleTeam = licence.getResponsibleTeam();

    if (responsibleTeam == null) {
      return false;
    }

    return teamQueryService.userHasStaticRole(
        userDetail.wuaId(),
        responsibleTeam.getTeamType(),
        responsibleTeam.getCaseManagerRole()
    );
  }

  private boolean matchesFilterAndHasAccess(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      WorkAreaFilterForm filterForm,
      ServiceUserDetail userDetail
  ) {
    var licence = scheduleWorkProgrammeApplicationService
        .getLicenceFromScheduleWorkProgrammeApplicationDetail(applicationDetail);

    if (!FilterUtil.filterTextInput(licence.getLicenceReference(), filterForm.getLicenceReference())) {
      return false;
    }

    var hasApplicationAccess = applicationAccessService.userHasAccessToApplication(
        applicationDetail.getScheduleWorkProgrammeApplication().getId().toString(),
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        applicationDetail.getResponsibleOrganisationUnitId(),
        userDetail.wuaId()
    );

    if (applicationDetail.getStatus() == ScheduleWorkProgrammeApplicationStatus.DRAFT) {
      return hasApplicationAccess;
    }

    return hasApplicationAccess || isCaseManager(userDetail, licence);
  }
}
