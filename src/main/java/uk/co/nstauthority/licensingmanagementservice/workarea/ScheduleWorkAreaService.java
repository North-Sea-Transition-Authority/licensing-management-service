package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;

@Service
public class ScheduleWorkAreaService implements WorkAreaItemProvider {

  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceSearchService licenceSearchService;

  public ScheduleWorkAreaService(LicenceScheduleDetailService licenceScheduleDetailService,
                                 LicenceSearchService licenceSearchService) {
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceSearchService = licenceSearchService;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(WorkAreaFilterForm workAreaFilterForm,
                                                 ServiceUserDetail serviceUserDetail) {
    //TODO filter correctly by form, user, and draft only
    var licenceSchedules = licenceScheduleDetailService.getAllLicenceScheduleDetails(serviceUserDetail).stream()
        .filter(licenceScheduleDetail -> FilterUtil.filterTextInput(
            licenceScheduleDetail.getLicenceSchedule().getLicence().getLicenceReference(),
            workAreaFilterForm.getLicenceReference()
            ))
        .toList();

    var licences = licenceSchedules.stream()
        .map(licenceScheduleDetail -> licenceScheduleDetail.getLicenceSchedule().getLicence())
        .toList();

    var responsibleOrganisations = licenceSearchService.getResponsibleOrganisationsForLicences(licences);

    var responsibleOrganisationNames = licenceSearchService.getResponsibleOrganisationNamesByLicences(responsibleOrganisations);

    return licenceSchedules.stream()
        .map(licenceScheduleDetail -> getScheduleWorkAreaItem(licenceScheduleDetail, responsibleOrganisationNames))
        .toList();
  }

  private SearchResultItem getScheduleWorkAreaItem(LicenceScheduleDetail licenceScheduleDetail,
                                                   Map<Licence, List<String>> responsibleOrganisationNamesByLicences) {

    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var createdDatetime = Instant.now(); //TODO add created datetime to schedule details
    var licensees = responsibleOrganisationNamesByLicences.get(licenceScheduleDetail.getLicenceSchedule().getLicence())
        .stream()
        .filter(Objects::nonNull)
        .toList();

    var dataItemRow = SummaryDataView.newBuilder()
        .addStringValue("Licence type", licence.getType().getDisplayName())
        .addStringValue("Licensees", String.join(", ", licensees))
        .build();

    return SearchResultItem.newBuilder()
        .withId(licenceScheduleDetail.getId().toString())
        .withLinkHeadingText(String.format("%s - draft schedule", licence.getLicenceReference()))
        .withLinkHeadingUrl(ReverseRouter.route(on(LicenceScheduleTimelineController.class)
            .renderLicenceScheduleTimeline(licenceScheduleDetail.getId(), null))
        )
        .withCaptionText(String.format("Created %s", DateFormatUtil.convertToDisplayTextWithTime(createdDatetime)))
        .withDataItemRow(dataItemRow)
        .withTransactionDatetime(createdDatetime)
        .build();

  }
}
