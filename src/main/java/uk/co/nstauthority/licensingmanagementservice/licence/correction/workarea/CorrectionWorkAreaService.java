package uk.co.nstauthority.licensingmanagementservice.licence.correction.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

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
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaFilterForm;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaItemProvider;

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
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    return licenceCorrectionService
        .getAllInProgressCorrectionsForUser(serviceUserDetail)
        .stream()
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
        .map(this::getCorrectionWorkAreaItem)
        .toList();
  }

  private SearchResultItem getCorrectionWorkAreaItem(LicenceCorrection correction) {
    var licence = correction.getLicence();
    var createdInstant = correction.getCreatedInstant();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Correction reference", correction.getCorrectionReference())
        .build();

    return SearchResultItem.newBuilder()
        .withId(correction.getId().toString())
        .withLinkHeadingText(licence.getLicenceReference())
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceCorrectionController.class)
            .renderCorrection(correction.getId(), null)))
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdInstant)))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(createdInstant)
        .build();
  }
}