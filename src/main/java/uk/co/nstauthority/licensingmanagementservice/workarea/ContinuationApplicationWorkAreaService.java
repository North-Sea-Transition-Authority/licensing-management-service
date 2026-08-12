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
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@Service
public class ContinuationApplicationWorkAreaService implements WorkAreaItemProvider {

  private final LicenceContinuationService licenceContinuationService;
  private final ApplicationAccessService applicationAccessService;
  private final RegulatorRoleService regulatorRoleService;
  private final WorkAreaItemViewService workAreaItemViewService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;

  public ContinuationApplicationWorkAreaService(
      LicenceContinuationService licenceContinuationService,
      ApplicationAccessService applicationAccessService,
      RegulatorRoleService regulatorRoleService,
      WorkAreaItemViewService workAreaItemViewService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.applicationAccessService = applicationAccessService;
    this.regulatorRoleService = regulatorRoleService;
    this.workAreaItemViewService = workAreaItemViewService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.CONTINUATION_APPLICATION;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    var isContinuationIssuer = regulatorRoleService.isContinuationIssuer(serviceUserDetail);
    var isContinuationReviewer = regulatorRoleService.isContinuationReviewer(serviceUserDetail);
    var isRegulator = regulatorRoleService.isRegulator(serviceUserDetail);

    var allApplicationDetails = licenceContinuationService
        .getAllContinuationApplicationDetailsByStatuses(ApplicationStatus.getSearchableStatuses());

    var licenceByApplicationDetail = allApplicationDetails.stream()
        .collect(Collectors.toMap(Function.identity(),
            LicenceContinuationApplicationDetail::getLicence));

    var licences = allApplicationDetails.stream()
        .map(licenceByApplicationDetail::get)
        .toList();

    var responsibleOrganisations = licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(licences);
    var orgUnitToGroupMap = licenceResponsibleOrganisationService
        .getOrgUnitToGroupIdMap(responsibleOrganisations, allApplicationDetails);

    var viewedItemIds = workAreaItemViewService.getWorkAreaItemLogsForUser(
            List.of(WorkAreaDataItemType.LICENCE_CONTINUATION_APPLICATION),
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
            isContinuationIssuer,
            isContinuationReviewer,
            isRegulator
        ))
        .map(applicationDetail ->
            createWorkAreaItem(
                applicationDetail,
                licenceByApplicationDetail.get(applicationDetail),
                responsibleOrganisations,
                isContinuationIssuer,
                viewedItemIds
            )
        )
        .toList();
  }

  private SearchResultItem createWorkAreaItem(
      LicenceContinuationApplicationDetail applicationDetail,
      Licence licence,
      Map<Licence, List<OrganisationUnit>> responsibleOrganisationsByLicences,
      boolean isContinuationIssuer,
      Set<UUID> viewedItemIds
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
      case DRAFT -> ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
          .getTaskList(applicationDetail.getId(), null, null));

      case ApplicationStatus.ISSUE_DECISION -> isContinuationIssuer
              ? ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(
                    ApplicationType.CONTINUATION_APPLICATION,
                    applicationDetail.getLicenceContinuationApplication().getId()
                ))
              : ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
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

    if (isNewItem(applicationDetail, isContinuationIssuer, viewedItemIds)) {
      builder.withNewLabel();
    }

    return builder.build();
  }

  private boolean isNewItem(
      LicenceContinuationApplicationDetail applicationDetail,
      boolean isContinuationIssuer,
      Set<UUID> viewedItemIds
  ) {
    if (applicationDetail.getStatus() == ApplicationStatus.ISSUE_DECISION && isContinuationIssuer) {
      return !viewedItemIds.contains(applicationDetail.getLicenceContinuationApplication().getId());
    }
    return !viewedItemIds.contains(applicationDetail.getId());
  }

  private boolean matchesFilterAndHasAccess(
      LicenceContinuationApplicationDetail applicationDetail,
      Licence licence,
      WorkAreaFilterForm filterForm,
      ServiceUserDetail userDetail,
      Map<Licence, List<OrganisationUnit>> responsibleOrganisations,
      Map<Integer, Integer> orgUnitToGroupMap,
      boolean isContinuationIssuer,
      boolean isContinuationReviewer,
      boolean isRegulator
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

    var licenceUnitIds = licenceResponsibleOrganisationService
        .getOrganisationUnitIdsFromLicenceOrgUnitMap(responsibleOrganisations, licence);

    var licenceOrgUnitGroupMap = licenceUnitIds.stream()
        .filter(orgUnitToGroupMap::containsKey)
        .collect(Collectors.toMap(Function.identity(), orgUnitToGroupMap::get));

    var hasAppAccess = applicationAccessService.userHasAccessToApplication(
        applicationDetail,
        licenceOrgUnitGroupMap,
        userDetail.wuaId()
    );

    return hasAppAccess(
        applicationDetail.getStatus(),
        hasAppAccess,
        isContinuationReviewer,
        isContinuationIssuer,
        isRegulator
    );
  }

  private static boolean hasAppAccess(
      ApplicationStatus status,
      boolean hasAppAccess,
      boolean isReviewer,
      boolean isIssuer,
      boolean isRegulator
  ) {
    switch (status) {
      case DRAFT:
        return hasAppAccess && !isRegulator;

      case SUBMITTED:
        return hasAppAccess || isReviewer;

      case ISSUE_DECISION:
        if (isIssuer) {
          return true;
        }
        if (isReviewer) {
          return false;
        }
        return hasAppAccess;

      default:
        return hasAppAccess;
    }
  }
}