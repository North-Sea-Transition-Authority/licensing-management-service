package uk.co.nstauthority.licensingmanagementservice.workarea;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.licence.search.LicenceSearchService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@Service
public class ScheduleWorkAreaService implements WorkAreaItemProvider {

  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceSearchService licenceSearchService;
  private final WorkAreaItemViewService workAreaItemViewService;

  public ScheduleWorkAreaService(
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceSearchService licenceSearchService,
      WorkAreaItemViewService workAreaItemViewService
  ) {
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceSearchService = licenceSearchService;
    this.workAreaItemViewService = workAreaItemViewService;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    var licenceSchedules = licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail).stream()
        .filter(licenceScheduleDetail -> !workAreaFilterForm.hasApplicationFilterApplied())
        .filter(licenceScheduleDetail -> FilterUtil.matchesTextInput(
            licenceScheduleDetail.getLicenceSchedule().getLicence().getLicenceReference(),
            workAreaFilterForm.getLicenceReference()
            ))
        .filter(licenceScheduleDetail -> FilterUtil.matchesEnum(
            LicenceType.class,
            licenceScheduleDetail.getLicenceSchedule().getLicence().getType(),
            workAreaFilterForm.getLicenceTypes()
        ))
        .toList();

    var licences = licenceSchedules.stream()
        .map(licenceScheduleDetail -> licenceScheduleDetail.getLicenceSchedule().getLicence())
        .toList();

    var responsibleOrganisationNames = licenceSearchService.getLicenceToResponsibleOrganisationNameMap(licences);

    var viewedItemIds = workAreaItemViewService.getWorkAreaItemLogsForUser(
            List.of(WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE),
            serviceUserDetail.wuaId()
        ).stream()
        .map(WorkAreaItemView::getItemId)
        .collect(Collectors.toSet());

    return licenceSchedules.stream()
        .map(licenceScheduleDetail -> getScheduleWorkAreaItem(
            licenceScheduleDetail,
            responsibleOrganisationNames,
            viewedItemIds
        ))
        .toList();
  }

  private SearchResultItem getScheduleWorkAreaItem(
      LicenceScheduleDetail licenceScheduleDetail,
      Map<Licence, List<String>> responsibleOrganisationNamesByLicences,
      Set<UUID> viewedItemIds
  ) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var createdDatetime = licenceScheduleDetail.getCreatedInstant();
    var licensees = responsibleOrganisationNamesByLicences.getOrDefault(
            licence,
            List.of()
        )
        .stream()
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

    var isNewItem = !viewedItemIds.contains(licenceScheduleDetail.getId());

    if (isNewItem) {
      builder.withNewLabel();
    }

    return builder.build();
  }
}
