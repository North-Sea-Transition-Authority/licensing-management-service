package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService.DECISION_ISSUER_ROLES;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
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
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@Service
public class ScheduleAndWorkProgrammeApplicationWorkAreaService implements WorkAreaItemProvider {

  public static final Set<ScheduleWorkProgrammeApplicationStatus> ACTIVE_APPLICATION_STATUSES = Set.of(
      ScheduleWorkProgrammeApplicationStatus.DRAFT,
      ScheduleWorkProgrammeApplicationStatus.SUBMITTED,
      ScheduleWorkProgrammeApplicationStatus.ISSUE_DECISION
  );
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final LicenceSearchService licenceSearchService;
  private final ApplicationAccessService applicationAccessService;
  private final TeamQueryService teamQueryService;
  private final WorkAreaItemViewService workAreaItemViewService;

  public ScheduleAndWorkProgrammeApplicationWorkAreaService(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      LicenceSearchService licenceSearchService,
      ApplicationAccessService applicationAccessService,
      TeamQueryService teamQueryService,
      WorkAreaItemViewService workAreaItemViewService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.licenceSearchService = licenceSearchService;
    this.applicationAccessService = applicationAccessService;
    this.teamQueryService = teamQueryService;
    this.workAreaItemViewService = workAreaItemViewService;
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

    var viewedItemIds = workAreaItemViewService.getWorkAreaItemLogsForUser(
            List.of(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION),
            serviceUserDetail.wuaId()
        ).stream()
        .map(WorkAreaItemView::getItemId)
        .collect(Collectors.toSet());

    return applicationDetails.stream()
        .map(applicationDetail ->
            createWorkAreaItem(applicationDetail, responsibleOrganisationNames, serviceUserDetail, viewedItemIds))
        .toList();
  }

  private SearchResultItem createWorkAreaItem(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Map<Licence, List<String>> responsibleOrganisationNamesByLicences,
      ServiceUserDetail serviceUserDetail,
      Set<UUID> viewedItemIds
  ) {
    var licence = scheduleWorkProgrammeApplicationService
        .getLicenceFromScheduleWorkProgrammeApplicationDetail(applicationDetail);
    var licensees = responsibleOrganisationNamesByLicences.getOrDefault(
            licence,
            List.of()
        )
        .stream()
        .filter(Objects::nonNull)
        .toList();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Status", applicationDetail.getStatus().getDisplayName())
        .addStringValue("Licence type", licence.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", licensees))
        .build();

    var linkHeadingUrl = switch (applicationDetail.getStatus()) {
      case DRAFT -> ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
          .getTaskList(applicationDetail.getId(), null, null));

      case ScheduleWorkProgrammeApplicationStatus.ISSUE_DECISION -> (isDecisionIssuer(serviceUserDetail))
          ? ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(
              ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
              applicationDetail.getScheduleWorkProgrammeApplication().getId()
            ))
          : ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
              .renderOverview(applicationDetail.getId(), null, null));

      default -> ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
          .renderOverview(applicationDetail.getId(), null, null));
    };

    var transactionDateTime = applicationDetail.getStatus() == ScheduleWorkProgrammeApplicationStatus.DRAFT
        ? applicationDetail.getCreatedDatetime()
        : applicationDetail.getSubmittedDatetime();

    var itemReference = applicationDetail.getStatus() == ScheduleWorkProgrammeApplicationStatus.DRAFT
        ? licence.getLicenceReference()
        : applicationDetail.getScheduleWorkProgrammeApplication().getApplicationReference();

    var captionText = applicationDetail.getStatus() == ScheduleWorkProgrammeApplicationStatus.DRAFT
        ? String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getCreatedDatetime()))
        : String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getSubmittedDatetime()));

    var isNewItem = !viewedItemIds.contains(applicationDetail.getId());

    var builder = SearchResultItem.newBuilder()
        .withId(applicationDetail.getId().toString())
        .withLinkHeadingText(String.format("%s - %s",
            itemReference,
            ApplicationType.SCHEDULE_AMENDMENT_APPLICATION.getDisplayName().toLowerCase())
        )
        .withLinkHeadingUrl(linkHeadingUrl)
        .withCaptionText(captionText)
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(transactionDateTime);

    if (isNewItem) {
      builder.withNewLabel();
    }

    return builder.build();
  }

  private boolean isDecisionIssuer(ServiceUserDetail userDetail) {
    return teamQueryService.getTeamRolesForUser(userDetail.wuaId()).stream()
        .anyMatch(teamRole -> DECISION_ISSUER_ROLES.contains(teamRole.getRole()));
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
