package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
public class ScheduleAndWorkProgrammeApplicationWorkAreaService implements WorkAreaItemProvider {

  static final String STEWARD_USER_PURPOSE = "Fetch steward users for schedule work programme work area items";

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final ApplicationAccessService applicationAccessService;
  private final RegulatorRoleService regulatorRoleService;
  private final TeamQueryService teamQueryService;
  private final ApplicationWorkAreaService applicationWorkAreaService;
  private final EnergyPortalUserService energyPortalUserService;

  public ScheduleAndWorkProgrammeApplicationWorkAreaService(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      ApplicationAccessService applicationAccessService,
      RegulatorRoleService regulatorRoleService,
      TeamQueryService teamQueryService,
      ApplicationWorkAreaService applicationWorkAreaService,
      EnergyPortalUserService energyPortalUserService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.applicationAccessService = applicationAccessService;
    this.regulatorRoleService = regulatorRoleService;
    this.teamQueryService = teamQueryService;
    this.applicationWorkAreaService = applicationWorkAreaService;
    this.energyPortalUserService = energyPortalUserService;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.SCHEDULE_APPLICATION;
  }

  @Override
  public WorkAreaSection loadSection(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail,
      WorkAreaScope workAreaScope
  ) {
    var allApplicationDetails = scheduleWorkProgrammeApplicationService
        .getAllScheduleWorkProgrammeApplicationDetailsByStatuses(ApplicationStatus.getSearchableStatuses());

    var licences = allApplicationDetails.stream()
        .map(ScheduleWorkProgrammeApplicationDetail::getLicence)
        .toList();

    return new WorkAreaSection(
        WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION,
        licences,
        OrganisationGroupLookup.REQUIRED,
        context -> getWorkAreaItems(allApplicationDetails, workAreaFilterForm, serviceUserDetail, workAreaScope, context)
    );
  }

  private List<SearchResultItem> getWorkAreaItems(
      List<ScheduleWorkProgrammeApplicationDetail> allApplicationDetails,
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail,
      WorkAreaScope workAreaScope,
      WorkAreaContext context
  ) {
    var userTeamRoles = teamQueryService.getTeamRolesForUser(serviceUserDetail.wuaId());
    var isRegulator = regulatorRoleService.isRegulator(userTeamRoles);
    var regulatorRoles = new RegulatorRoles(isRegulator, regulatorRoleService.isDecisionIssuer(userTeamRoles));

    var visibleApplicationDetails = allApplicationDetails.stream()
        .filter(applicationDetail -> matchesFilterAndHasAccess(
            applicationDetail,
            applicationDetail.getLicence(),
            workAreaFilterForm,
            serviceUserDetail.wuaId(),
            userTeamRoles,
            context,
            regulatorRoles,
            workAreaScope
        ))
        .toList();

    var stewardsByWuaId = getStewardsByWuaId(visibleApplicationDetails);

    return visibleApplicationDetails.stream()
        .map(applicationDetail -> createWorkAreaItem(
            applicationDetail,
            applicationDetail.getLicence(),
            context,
            stewardsByWuaId,
            regulatorRoles.decisionIssuer()
        ))
        .toList();
  }

  private Map<WebUserAccountId, EnergyPortalUserJson> getStewardsByWuaId(
      List<ScheduleWorkProgrammeApplicationDetail> visibleApplicationDetails
  ) {
    var stewardWuaIds = visibleApplicationDetails.stream()
        .map(applicationDetail -> applicationDetail.getScheduleWorkProgrammeApplication().getStewardWuaId())
        .filter(Objects::nonNull)
        .map(WebUserAccountId::from)
        .collect(Collectors.toSet());

    return energyPortalUserService.getEnergyPortalUserMap(stewardWuaIds, STEWARD_USER_PURPOSE);
  }

  private SearchResultItem createWorkAreaItem(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Licence licence,
      WorkAreaContext context,
      Map<WebUserAccountId, EnergyPortalUserJson> stewardsByWuaId,
      boolean isDecisionIssuer
  ) {
    var licensees = context.getLicensees(licence)
        .stream()
        .filter(Objects::nonNull)
        .map(OrganisationUnit::organisationUnitName)
        .filter(Objects::nonNull)
        .toList();

    var stewardWuaId = applicationDetail.getScheduleWorkProgrammeApplication().getStewardWuaId();
    var stewardName = Optional.ofNullable(stewardWuaId)
        .map(WebUserAccountId::from)
        .map(stewardsByWuaId::get)
        .map(EnergyPortalUserJson::displayName)
        .orElse("Not allocated");

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", licensees))
        .addStringValue("Status", applicationDetail.getStatus().getDisplayName())
        .addStringValue("Steward", stewardName)
        .build();

    var linkHeadingUrl = switch (applicationDetail.getStatus()) {
      case DRAFT -> ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
          .getTaskList(applicationDetail.getId(), null, null));

      case ISSUE_DECISION -> ReverseRouter.route(isDecisionIssuer
          ? on(ApplicationLetterController.class).renderEditLetterOverview(
              ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
              applicationDetail.getScheduleWorkProgrammeApplication().getId()
          )
          : on(ScheduleWorkProgrammeApplicationOverviewController.class)
              .renderOverview(applicationDetail.getId(), null, null, null));

      default -> ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
          .renderOverview(applicationDetail.getId(), null, null, null));
    };

    var transactionDateTime = applicationDetail.getStatus() == ApplicationStatus.DRAFT
        ? applicationDetail.getCreatedDatetime()
        : applicationDetail.getSubmittedDatetime();

    var itemReference = applicationDetail.getStatus() == ApplicationStatus.DRAFT
        ? licence.getLicenceReference()
        : applicationDetail.getScheduleWorkProgrammeApplication().getApplicationReference();

    var captionText = applicationDetail.getStatus() == ApplicationStatus.DRAFT
        ? String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getCreatedDatetime()))
        : String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getSubmittedDatetime()));

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

    if (context.isNewItem(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION, applicationDetail.getId())) {
      builder.withNewLabel();
    }

    return builder.build();
  }

  private boolean matchesFilterAndHasAccess(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Licence licence,
      WorkAreaFilterForm filterForm,
      Long wuaId,
      Set<TeamRole> userTeamRoles,
      WorkAreaContext context,
      RegulatorRoles regulatorRoles,
      WorkAreaScope workAreaScope
  ) {
    if (!FilterUtil.matchesTextInput(licence.getLicenceReference(), filterForm.getLicenceReference())) {
      return false;
    }

    if (!FilterUtil.matchesEnum(LicenceType.class, licence.getType(), filterForm.getLicenceTypes())) {
      return false;
    }

    if (!FilterUtil.matchesTextInput(
        Objects.requireNonNullElse(applicationDetail.getScheduleWorkProgrammeApplication().getApplicationReference(), ""),
        filterForm.getApplicationReference()
    )) {
      return false;
    }

    if (!FilterUtil.matchesEnum(
        ApplicationType.class,
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        filterForm.getApplicationTypes()
    )) {
      return false;
    }

    if (!FilterUtil.matchesEnum(ApplicationStatus.class, applicationDetail.getStatus(), filterForm.getApplicationStatuses())) {
      return false;
    }

    if (!context.matchesLicenseeFilters(licence, filterForm)) {
      return false;
    }

    var licenceOrgUnitGroupMap = context.getOrganisationGroupIdsByUnitId(context.getLicenseeOrganisationUnitIds(licence));

    if (workAreaScope == WorkAreaScope.MY_WORK && !regulatorRoles.regulator()
        && !applicationWorkAreaService.isIndustryMyWork(applicationDetail, licenceOrgUnitGroupMap, wuaId, userTeamRoles)) {
      return false;
    }

    var hasIndustryAccess = applicationAccessService.userHasIndustryAccessToApplication(
        applicationDetail,
        licenceOrgUnitGroupMap,
        userTeamRoles
    );

    if (hasIndustryAccess) {
      return true;
    }

    if (!regulatorRoles.regulator() || !applicationDetail.hasBeenSubmitted()) {
      return false;
    }

    return workAreaScope == WorkAreaScope.MY_WORK
        ? applicationWorkAreaService.isRegulatorMyWork(applicationDetail, wuaId, userTeamRoles)
        : applicationWorkAreaService.isRegulatorAllWork(applicationDetail, wuaId, userTeamRoles);
  }

  private record RegulatorRoles(boolean regulator, boolean decisionIssuer) {
  }
}
