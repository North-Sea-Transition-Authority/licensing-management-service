package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Collection;
import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionController;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.OrganisationGroupLookup;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaContext;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaFilterForm;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaItemProvider;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaScope;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaSection;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
public class CorrectionWorkAreaService implements WorkAreaItemProvider {

  private final LicenceCorrectionService licenceCorrectionService;

  public CorrectionWorkAreaService(LicenceCorrectionService licenceCorrectionService) {
    this.licenceCorrectionService = licenceCorrectionService;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.START_CORRECTION;
  }

  @Override
  public WorkAreaSection loadSection(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail,
      WorkAreaScope workAreaScope
  ) {
    // Corrections are not applications, so none match when the work area is filtered by application
    if (workAreaFilterForm.hasApplicationFilterApplied()) {
      return WorkAreaSection.empty(WorkAreaDataItemType.LICENCE_CORRECTION);
    }

    var corrections = licenceCorrectionService.getAllInProgressCorrectionsForUser(serviceUserDetail);

    var licences = corrections.stream()
        .map(LicenceCorrection::getLicence)
        .toList();

    return new WorkAreaSection(
        WorkAreaDataItemType.LICENCE_CORRECTION,
        licences,
        OrganisationGroupLookup.NOT_REQUIRED,
        context -> getWorkAreaItems(corrections, workAreaFilterForm, context)
    );
  }

  private List<SearchResultItem> getWorkAreaItems(
      Collection<LicenceCorrection> corrections,
      WorkAreaFilterForm workAreaFilterForm,
      WorkAreaContext context
  ) {
    return corrections.stream()
        .filter(correction -> FilterUtil.matchesTextInput(
            correction.getLicence().getLicenceReference(),
            workAreaFilterForm.getLicenceReference()
        ))
        .filter(correction -> FilterUtil.matchesEnum(
            LicenceType.class,
            correction.getLicence().getType(),
            workAreaFilterForm.getLicenceTypes()
        ))
        .filter(correction -> context.matchesLicenseeFilters(correction.getLicence(), workAreaFilterForm))
        .map(correction -> getCorrectionWorkAreaItem(correction, context))
        .toList();
  }

  private SearchResultItem getCorrectionWorkAreaItem(LicenceCorrection correction, WorkAreaContext context) {
    var licence = correction.getLicence();
    var createdInstant = correction.getCreatedInstant();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Correction reference", correction.getCorrectionReference())
        .build();

    var builder = SearchResultItem.newBuilder()
        .withId(correction.getId().toString())
        .withLinkHeadingText(String.format("%s - licence correction", licence.getLicenceReference()))
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction, null)))
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdInstant)))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(createdInstant);

    if (context.isNewItem(WorkAreaDataItemType.LICENCE_CORRECTION, correction.getId())) {
      builder.withNewLabel();
    }

    return builder.build();
  }
}
