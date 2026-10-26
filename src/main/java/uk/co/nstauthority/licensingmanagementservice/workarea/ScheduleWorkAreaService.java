package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
public class ScheduleWorkAreaService implements WorkAreaItemProvider {

  private final LicenceScheduleDetailService licenceScheduleDetailService;

  public ScheduleWorkAreaService(LicenceScheduleDetailService licenceScheduleDetailService) {
    this.licenceScheduleDetailService = licenceScheduleDetailService;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.MANAGE_SCHEDULE;
  }

  @Override
  public WorkAreaSection loadSection(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail,
      WorkAreaScope workAreaScope
  ) {
    // Draft schedules are not applications, so none match when the work area is filtered by application
    if (workAreaFilterForm.hasApplicationFilterApplied()) {
      return WorkAreaSection.empty(WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE);
    }

    var allDraftLicenceScheduleDetails = licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail);

    var licences = allDraftLicenceScheduleDetails.stream()
        .map(licenceScheduleDetail -> licenceScheduleDetail.getLicenceSchedule().getLicence())
        .toList();

    return new WorkAreaSection(
        WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE,
        licences,
        OrganisationGroupLookup.NOT_REQUIRED,
        context -> getWorkAreaItems(allDraftLicenceScheduleDetails, workAreaFilterForm, context)
    );
  }

  private List<SearchResultItem> getWorkAreaItems(
      List<LicenceScheduleDetail> allDraftLicenceScheduleDetails,
      WorkAreaFilterForm workAreaFilterForm,
      WorkAreaContext context
  ) {
    return allDraftLicenceScheduleDetails.stream()
        .filter(licenceScheduleDetail -> FilterUtil.matchesTextInput(
            licenceScheduleDetail.getLicenceSchedule().getLicence().getLicenceReference(),
            workAreaFilterForm.getLicenceReference()
        ))
        .filter(licenceScheduleDetail -> FilterUtil.matchesEnum(
            LicenceType.class,
            licenceScheduleDetail.getLicenceSchedule().getLicence().getType(),
            workAreaFilterForm.getLicenceTypes()
        ))
        .filter(licenceScheduleDetail -> context.matchesLicenseeFilters(
            licenceScheduleDetail.getLicenceSchedule().getLicence(),
            workAreaFilterForm
        ))
        .map(licenceScheduleDetail -> getScheduleWorkAreaItem(licenceScheduleDetail, context))
        .toList();
  }

  private SearchResultItem getScheduleWorkAreaItem(LicenceScheduleDetail licenceScheduleDetail, WorkAreaContext context) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var createdDatetime = licenceScheduleDetail.getCreatedInstant();
    var licensees = context.getLicensees(licence)
        .stream()
        .filter(Objects::nonNull)
        .map(OrganisationUnit::organisationUnitName)
        .filter(Objects::nonNull)
        .toList();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence", licence.getLicenceReference())
        .addStringValue("Licensees", String.join(", ", licensees))
        .build();

    var builder = SearchResultItem.newBuilder()
        .withId(licenceScheduleDetail.getId().toString())
        .withLinkHeadingText(String.format("%s - draft schedule", licence.getLicenceReference()))
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceScheduleTimelineController.class)
            .renderLicenceScheduleTimeline(licenceScheduleDetail.getId(), null, null, null))
        )
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdDatetime)))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(createdDatetime);

    if (context.isNewItem(WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE, licenceScheduleDetail.getId())) {
      builder.withNewLabel();
    }

    return builder.build();
  }
}
