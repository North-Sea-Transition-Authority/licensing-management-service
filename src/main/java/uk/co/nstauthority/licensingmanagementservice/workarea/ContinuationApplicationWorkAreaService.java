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
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.ApplicationLetterController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.teams.RegulatorRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;

@Service
public class ContinuationApplicationWorkAreaService implements WorkAreaItemProvider {

  public static final Set<LicenceContinuationApplicationStatus> ACTIVE_APPLICATION_STATUSES = Set.of(
      LicenceContinuationApplicationStatus.DRAFT,
      LicenceContinuationApplicationStatus.SUBMITTED,
      LicenceContinuationApplicationStatus.ISSUE_DECISION,
      LicenceContinuationApplicationStatus.COMPLETE
  );

  private final LicenceContinuationService licenceContinuationService;
  private final LicenceSearchService licenceSearchService;
  private final ApplicationAccessService applicationAccessService;
  private final RegulatorRoleService regulatorRoleService;

  public ContinuationApplicationWorkAreaService(
      LicenceContinuationService licenceContinuationService,
      LicenceSearchService licenceSearchService,
      ApplicationAccessService applicationAccessService,
      RegulatorRoleService regulatorRoleService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.licenceSearchService = licenceSearchService;
    this.applicationAccessService = applicationAccessService;
    this.regulatorRoleService = regulatorRoleService;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    var applicationDetails = licenceContinuationService
        .getAllContinuationApplicationDetailsByStatuses(ACTIVE_APPLICATION_STATUSES).stream()
        .filter(applicationDetail -> matchesFilterAndHasAccess(applicationDetail, workAreaFilterForm, serviceUserDetail))
        .toList();

    var licences = applicationDetails.stream()
        .map(licenceContinuationService::getLicenceFromContinuationApplicationDetail)
        .toList();

    var responsibleOrganisationNames = licenceSearchService.getLicenceToResponsibleOrganisationNameMap(licences);

    return applicationDetails.stream()
        .map(applicationDetail -> createWorkAreaItem(applicationDetail, responsibleOrganisationNames, serviceUserDetail))
        .toList();
  }

  private SearchResultItem createWorkAreaItem(
      LicenceContinuationApplicationDetail applicationDetail,
      Map<Licence, List<String>> responsibleOrganisationNamesByLicences,
      ServiceUserDetail serviceUserDetail
  ) {
    var licence = licenceContinuationService.getLicenceFromContinuationApplicationDetail(applicationDetail);
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
      case DRAFT -> ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
          .getTaskList(applicationDetail.getId(), null, null));

      case LicenceContinuationApplicationStatus.ISSUE_DECISION -> (regulatorRoleService.isContinuationIssuer(serviceUserDetail))
              ? ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(
                    ApplicationType.CONTINUATION_APPLICATION,
                    applicationDetail.getLicenceContinuationApplication().getId()
                ))
              : ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
                                    .renderOverview(applicationDetail.getId(), null, null, null));

      default -> ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class)
                                         .renderOverview(applicationDetail.getId(), null, null, null));
    };

    var itemReference = applicationDetail.getStatus() == LicenceContinuationApplicationStatus.DRAFT
        ? licence.getLicenceReference()
        : applicationDetail.getLicenceContinuationApplication().getApplicationReference();

    var captionText = applicationDetail.getStatus() == LicenceContinuationApplicationStatus.DRAFT
        ? String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getCreatedDateTime()))
        : String.format("Submitted %s", DateFormatUtil.convertToDisplayTextWithTime(applicationDetail.getSubmittedDatetime()));

    return SearchResultItem.newBuilder()
        .withId(applicationDetail.getId().toString())
        .withLinkHeadingText(String.format("%s - Licence continuation application", itemReference))
        .withLinkHeadingUrl(linkHeadingUrl)
        .withCaptionText(captionText)
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(applicationDetail.getCreatedDateTime())
        .build();
  }

  private boolean matchesFilterAndHasAccess(
      LicenceContinuationApplicationDetail applicationDetail,
      WorkAreaFilterForm filterForm,
      ServiceUserDetail userDetail
  ) {
    Licence licence = licenceContinuationService.getLicenceFromContinuationApplicationDetail(applicationDetail);

    if (!FilterUtil.filterTextInput(licence.getLicenceReference(), filterForm.getLicenceReference())) {
      return false;
    }

    var status = applicationDetail.getStatus();
    boolean isIssuer = regulatorRoleService.isContinuationIssuer(userDetail);
    boolean isReviewer = regulatorRoleService.isContinuationReviewer(userDetail);
    boolean hasAppAccess = applicationAccessService.userHasAccessToApplication(
        applicationDetail.getId().toString(),
        ApplicationType.CONTINUATION_APPLICATION,
        applicationDetail.getResponsibleOrganisationUnitId(),
        userDetail.wuaId()
    );

    return hasAppAccess(
        status,
        hasAppAccess,
        isReviewer,
        isIssuer
    );
  }

  private static boolean hasAppAccess(
      LicenceContinuationApplicationStatus status,
      boolean hasAppAccess,
      boolean isReviewer,
      boolean isIssuer
  ) {
    switch (status) {
      case DRAFT:
        return hasAppAccess;

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