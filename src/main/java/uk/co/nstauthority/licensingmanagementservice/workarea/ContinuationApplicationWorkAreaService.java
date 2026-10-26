package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Objects;
import java.util.Set;
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
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
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
public class ContinuationApplicationWorkAreaService implements WorkAreaItemProvider {

  private final LicenceContinuationService licenceContinuationService;
  private final ApplicationAccessService applicationAccessService;
  private final RegulatorRoleService regulatorRoleService;
  private final TeamQueryService teamQueryService;
  private final ApplicationWorkAreaService applicationWorkAreaService;

  public ContinuationApplicationWorkAreaService(
      LicenceContinuationService licenceContinuationService,
      ApplicationAccessService applicationAccessService,
      RegulatorRoleService regulatorRoleService,
      TeamQueryService teamQueryService,
      ApplicationWorkAreaService applicationWorkAreaService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.applicationAccessService = applicationAccessService;
    this.regulatorRoleService = regulatorRoleService;
    this.teamQueryService = teamQueryService;
    this.applicationWorkAreaService = applicationWorkAreaService;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.CONTINUATION_APPLICATION;
  }

  @Override
  public WorkAreaSection loadSection(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail,
      WorkAreaScope workAreaScope
  ) {
    var allApplicationDetails = licenceContinuationService
        .getAllContinuationApplicationDetailsByStatuses(ApplicationStatus.getSearchableStatuses());

    var licences = allApplicationDetails.stream()
        .map(LicenceContinuationApplicationDetail::getLicence)
        .toList();

    return new WorkAreaSection(
        WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION,
        licences,
        OrganisationGroupLookup.REQUIRED,
        context -> getWorkAreaItems(allApplicationDetails, workAreaFilterForm, serviceUserDetail, workAreaScope, context)
    );
  }

  private List<SearchResultItem> getWorkAreaItems(
      List<LicenceContinuationApplicationDetail> allApplicationDetails,
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail,
      WorkAreaScope workAreaScope,
      WorkAreaContext context
  ) {
    var userTeamRoles = teamQueryService.getTeamRolesForUser(serviceUserDetail.wuaId());
    var regulatorRoles = new RegulatorRoles(
        regulatorRoleService.isRegulator(userTeamRoles),
        regulatorRoleService.isContinuationIssuer(userTeamRoles)
    );

    return allApplicationDetails.stream()
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
        .map(applicationDetail ->
            createWorkAreaItem(
                applicationDetail,
                applicationDetail.getLicence(),
                context,
                regulatorRoles.continuationIssuer()
            )
        )
        .toList();
  }

  private SearchResultItem createWorkAreaItem(
      LicenceContinuationApplicationDetail applicationDetail,
      Licence licence,
      WorkAreaContext context,
      boolean isContinuationIssuer
  ) {
    var licensees = context.getLicensees(licence)
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
      case DRAFT -> ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
          .getTaskList(applicationDetail.getId(), null, null));

      case ApplicationStatus.ISSUE_DECISION -> ReverseRouter.route(isContinuationIssuer
          ? on(ApplicationLetterController.class).renderEditLetterOverview(
              ApplicationType.CONTINUATION_APPLICATION,
              applicationDetail.getLicenceContinuationApplication().getId()
          )
          : on(LicenceContinuationApplicationOverviewController.class)
              .renderOverview(applicationDetail.getId(), null, null, null));

      default -> ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
                                         .renderOverview(applicationDetail.getId(), null, null, null));
    };

    var transactionDateTime = applicationDetail.getStatus() == ApplicationStatus.DRAFT
        ? applicationDetail.getCreatedDateTime()
        : applicationDetail.getSubmittedDatetime();

    var itemReference = applicationDetail.getStatus() == ApplicationStatus.DRAFT
        ? licence.getLicenceReference()
        : applicationDetail.getLicenceContinuationApplication().getApplicationReference();

    var captionText = applicationDetail.getStatus() == ApplicationStatus.DRAFT
        ? String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getCreatedDateTime()))
        : String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getSubmittedDatetime()));

    var builder = SearchResultItem.newBuilder()
        .withId(applicationDetail.getId().toString())
        .withLinkHeadingText(String.format("%s - %s",
            itemReference,
            ApplicationType.CONTINUATION_APPLICATION.getDisplayName().toLowerCase())
        )
        .withLinkHeadingUrl(linkHeadingUrl)
        .withCaptionText(captionText)
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(transactionDateTime);

    if (isNewItem(applicationDetail, isContinuationIssuer, context)) {
      builder.withNewLabel();
    }

    return builder.build();
  }

  private boolean isNewItem(
      LicenceContinuationApplicationDetail applicationDetail,
      boolean isContinuationIssuer,
      WorkAreaContext context
  ) {
    var itemId = applicationDetail.getStatus() == ApplicationStatus.ISSUE_DECISION && isContinuationIssuer
        ? applicationDetail.getLicenceContinuationApplication().getId()
        : applicationDetail.getId();
    return context.isNewItem(WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION, itemId);
  }

  private boolean matchesFilterAndHasAccess(
      LicenceContinuationApplicationDetail applicationDetail,
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
        Objects.requireNonNullElse(applicationDetail.getLicenceContinuationApplication().getApplicationReference(), ""),
        filterForm.getApplicationReference()
    )) {
      return false;
    }

    if (!FilterUtil.matchesEnum(
        ApplicationType.class,
        ApplicationType.CONTINUATION_APPLICATION,
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

  private record RegulatorRoles(boolean regulator, boolean continuationIssuer) {
  }
}
