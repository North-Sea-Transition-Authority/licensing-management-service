package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.tasklist.LicenceContinuationApplicationTaskListController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;

@Service
public class ContinuationApplicationWorkAreaService implements WorkAreaItemProvider {

  private final LicenceContinuationService licenceContinuationService;
  private final LicenceSearchService licenceSearchService;
  private final ApplicationAccessService applicationAccessService;

  public ContinuationApplicationWorkAreaService(
      LicenceContinuationService licenceContinuationService,
      LicenceSearchService licenceSearchService,
      ApplicationAccessService applicationAccessService
  ) {
    this.licenceContinuationService = licenceContinuationService;
    this.licenceSearchService = licenceSearchService;
    this.applicationAccessService = applicationAccessService;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    var applicationDetails = licenceContinuationService
        .getAllContinuationApplicationDetailsByStatus(LicenceContinuationApplicationStatus.DRAFT).stream()
        .filter(applicationDetail -> matchesFilterAndHasAccess(applicationDetail, workAreaFilterForm, serviceUserDetail))
        .toList();

    var licences = applicationDetails.stream()
        .map(licenceContinuationService::getLicenceFromContinuationApplicationDetail)
        .toList();

    var responsibleOrganisationNames = licenceSearchService.getLicenceToResponsibleOrganisationNameMap(licences);

    return applicationDetails.stream()
        .map(applicationDetail -> createWorkAreaItem(applicationDetail, responsibleOrganisationNames))
        .toList();
  }

  private SearchResultItem createWorkAreaItem(
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail,
      Map<Licence, List<String>> responsibleOrganisationNamesByLicences
  ) {
    var licence = licenceContinuationService
        .getLicenceFromContinuationApplicationDetail(licenceContinuationApplicationDetail);
    var createdDatetime = licenceContinuationApplicationDetail.getCreatedDateTime();
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

    return SearchResultItem.newBuilder()
        .withId(licenceContinuationApplicationDetail.getId().toString())
        .withLinkHeadingText(String.format("%s - Licence continuation application", licence.getLicenceReference()))
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceContinuationApplicationTaskListController.class)
            .getTaskList(licenceContinuationApplicationDetail.getId(), null, null))
        )
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdDatetime)))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(createdDatetime)
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

    return applicationAccessService.userHasAccessToApplication(
        applicationDetail.getId().toString(),
        ApplicationType.CONTINUATION_APPLICATION,
        null,
        userDetail.wuaId()
    );
  }
}