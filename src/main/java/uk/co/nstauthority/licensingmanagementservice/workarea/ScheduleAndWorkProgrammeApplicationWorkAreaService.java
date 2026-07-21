package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.ScheduleWorkProgrammeApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.tasklist.ScheduleWorkProgrammeApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@Service
public class ScheduleAndWorkProgrammeApplicationWorkAreaService implements WorkAreaItemProvider {

  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;
  private final ApplicationAccessService applicationAccessService;
  private final TeamQueryService teamQueryService;
  private final WorkAreaItemViewService workAreaItemViewService;
  private final RegulatorRoleService regulatorRoleService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  public ScheduleAndWorkProgrammeApplicationWorkAreaService(
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService,
      ApplicationAccessService applicationAccessService,
      TeamQueryService teamQueryService,
      WorkAreaItemViewService workAreaItemViewService,
      RegulatorRoleService regulatorRoleService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService
  ) {
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
    this.applicationAccessService = applicationAccessService;
    this.teamQueryService = teamQueryService;
    this.workAreaItemViewService = workAreaItemViewService;
    this.regulatorRoleService = regulatorRoleService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    var decisionIssuer = regulatorRoleService.isDecisionIssuer(serviceUserDetail);
    var isRegulator = regulatorRoleService.isRegulator(serviceUserDetail);

    var allApplicationDetails = scheduleWorkProgrammeApplicationService
        .getAllScheduleWorkProgrammeApplicationDetailsByStatuses(ApplicationStatus.getSearchableStatuses());

    var licenceByApplicationDetail = allApplicationDetails.stream()
        .collect(Collectors.toMap(Function.identity(),
            ScheduleWorkProgrammeApplicationDetail::getLicence));

    var licences = allApplicationDetails.stream()
        .map(licenceByApplicationDetail::get)
        .toList();

    var responsibleOrganisations = licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(licences);
    var orgUnitToGroupMap = licenceResponsibleOrganisationService.getOrgUnitToGroupIdMap(
        responsibleOrganisations,
        allApplicationDetails
    );

    var viewedItemIds = workAreaItemViewService.getWorkAreaItemLogsForUser(
            List.of(WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION),
            serviceUserDetail.wuaId()
        ).stream()
        .map(WorkAreaItemView::getItemId)
        .collect(Collectors.toSet());

    return allApplicationDetails.stream()
        .filter(applicationDetail -> matchesFilterAndHasAccess(
            applicationDetail,
            licenceByApplicationDetail.get(applicationDetail),
            workAreaFilterForm,
            serviceUserDetail,
            responsibleOrganisations,
            orgUnitToGroupMap,
            isRegulator
            )
        )
        .map(applicationDetail -> createWorkAreaItem(
            applicationDetail,
            licenceByApplicationDetail.get(applicationDetail),
            responsibleOrganisations,
            viewedItemIds,
            decisionIssuer
        ))
        .toList();
  }

  private SearchResultItem createWorkAreaItem(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      Licence licence,
      Map<Licence, List<OrganisationUnit>> responsibleOrganisationsByLicences,
      Set<UUID> viewedItemIds,
      boolean decisionIssuer
  ) {
    var licensees = responsibleOrganisationsByLicences.getOrDefault(
            licence,
            List.of()
        )
        .stream()
        .filter(Objects::nonNull)
        .map(OrganisationUnit::organisationUnitName)
        .filter(Objects::nonNull)
        .toList();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", licensees))
        .addStringValue("Status", applicationDetail.getStatus().getDisplayName())
        .build();

    var linkHeadingUrl = switch (applicationDetail.getStatus()) {
      case DRAFT -> ReverseRouter.route(on(ScheduleWorkProgrammeApplicationTaskListController.class)
          .getTaskList(applicationDetail.getId(), null, null));

      case ApplicationStatus.ISSUE_DECISION -> decisionIssuer
          ? ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(
              ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
              applicationDetail.getScheduleWorkProgrammeApplication().getId()
            ))
          : ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
              .renderOverview(applicationDetail.getId(), null, null));

      default -> ReverseRouter.route(on(ScheduleWorkProgrammeApplicationOverviewController.class)
          .renderOverview(applicationDetail.getId(), null, null));
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
      Licence licence,
      WorkAreaFilterForm filterForm,
      ServiceUserDetail userDetail,
      Map<Licence, List<OrganisationUnit>> responsibleOrganisations,
      Map<Integer, Integer> orgUnitToGroupMap,
      boolean isRegulator
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

    var licenceUnitIds = licenceResponsibleOrganisationService
        .getOrganisationUnitIdsFromLicenceOrgUnitMap(responsibleOrganisations, licence);

    var licenceOrgUnitGroupMap = licenceUnitIds.stream()
        .filter(orgUnitToGroupMap::containsKey)
        .collect(Collectors.toMap(Function.identity(), orgUnitToGroupMap::get));

    var hasApplicationAccess = applicationAccessService.userHasAccessToApplication(
        applicationDetail,
        licenceOrgUnitGroupMap,
        userDetail.wuaId()
    );

    if (applicationDetail.getStatus() == ApplicationStatus.DRAFT) {
      return hasApplicationAccess && !isRegulator;
    }

    return hasApplicationAccess || isCaseManager(userDetail, licence);
  }
}
