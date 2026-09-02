package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaFilterForm;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaItemProvider;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@Service
public class CorrectionWorkAreaService implements WorkAreaItemProvider {

  private final LicenceCorrectionService licenceCorrectionService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final WorkAreaItemViewService workAreaItemViewService;

  public CorrectionWorkAreaService(
      LicenceCorrectionService licenceCorrectionService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      OrganisationGroupQueryService organisationGroupQueryService,
      WorkAreaItemViewService workAreaItemViewService
  ) {
    this.licenceCorrectionService = licenceCorrectionService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.workAreaItemViewService = workAreaItemViewService;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.START_CORRECTION;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    var corrections = licenceCorrectionService.getAllInProgressCorrectionsForUser(serviceUserDetail);

    var licences = corrections.stream()
        .map(LicenceCorrection::getLicence)
        .toList();

    var responsibleOrganisations = licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(licences);
    var licenseeGroupOrgUnitIds = workAreaFilterForm.getLicenseeOrgGroupId() == null
        ? null
        : organisationGroupQueryService.getOrganisationUnitIdsByOrganisationGroupId(workAreaFilterForm.getLicenseeOrgGroupId());

    var viewedItemIds = workAreaItemViewService.getWorkAreaItemLogsForUser(
            List.of(WorkAreaDataItemType.LICENCE_CORRECTION),
            serviceUserDetail.wuaId()
        ).stream()
        .map(WorkAreaItemView::getItemId)
        .collect(Collectors.toSet());

    return corrections.stream()
        .filter(correction -> !workAreaFilterForm.hasApplicationFilterApplied())
        .filter(correction -> FilterUtil.matchesTextInput(
            correction.getLicence().getLicenceReference(),
            workAreaFilterForm.getLicenceReference()
        ))
        .filter(correction -> FilterUtil.matchesEnum(
            LicenceType.class,
            correction.getLicence().getType(),
            workAreaFilterForm.getLicenceTypes()
        ))
        .filter(correction -> {
          var licenceUnitIds = licenceResponsibleOrganisationService
              .getOrganisationUnitIdsFromLicenceOrgUnitMap(responsibleOrganisations, correction.getLicence());

          return FilterUtil.matchesIdList(licenceUnitIds, workAreaFilterForm.getLicenseeOrgUnitId())
              && FilterUtil.listMatchesIdList(licenceUnitIds, licenseeGroupOrgUnitIds);
        })
        .map(correction -> getCorrectionWorkAreaItem(correction, viewedItemIds))
        .toList();
  }

  private SearchResultItem getCorrectionWorkAreaItem(LicenceCorrection correction, Set<UUID> viewedItemIds) {
    var licence = correction.getLicence();
    var createdInstant = correction.getCreatedInstant();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Correction reference", correction.getCorrectionReference())
        .build();

    var builder = SearchResultItem.newBuilder()
        .withId(correction.getId().toString())
        .withLinkHeadingText(String.format("%s - licence correction", licence.getLicenceReference()))
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction.getId(), null)))
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdInstant)))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(createdInstant);

    if (!viewedItemIds.contains(correction.getId())) {
      builder.withNewLabel();
    }

    return builder.build();
  }
}