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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetailService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.timeline.LicenceScheduleTimelineController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.phasedrelease.ReleaseFeature;
import uk.co.nstauthority.licensingmanagementservice.query.SearchResultItem;
import uk.co.nstauthority.licensingmanagementservice.summary.SummaryDataView;
import uk.co.nstauthority.licensingmanagementservice.util.FilterUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

@Service
public class ScheduleWorkAreaService implements WorkAreaItemProvider {

  private final LicenceScheduleDetailService licenceScheduleDetailService;
  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final WorkAreaItemViewService workAreaItemViewService;

  public ScheduleWorkAreaService(
      LicenceScheduleDetailService licenceScheduleDetailService,
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      OrganisationGroupQueryService organisationGroupQueryService,
      WorkAreaItemViewService workAreaItemViewService
  ) {
    this.licenceScheduleDetailService = licenceScheduleDetailService;
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.workAreaItemViewService = workAreaItemViewService;
  }

  @Override
  public ReleaseFeature getReleaseFeature() {
    return ReleaseFeature.MANAGE_SCHEDULE;
  }

  @Override
  public List<SearchResultItem> getWorkAreaItems(
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    var allDraftLicenceScheduleDetails = licenceScheduleDetailService.getAllDraftLicenceScheduleDetailsForUser(serviceUserDetail);

    var allLicences = allDraftLicenceScheduleDetails.stream()
        .map(licenceScheduleDetail -> licenceScheduleDetail.getLicenceSchedule().getLicence())
        .toList();

    var responsibleOrganisations = licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(allLicences);
    var licenseeGroupOrgUnitIds = workAreaFilterForm.getLicenseeOrgGroupId() == null
        ? null
        : organisationGroupQueryService.getOrganisationUnitIdsByOrganisationGroupId(workAreaFilterForm.getLicenseeOrgGroupId());

    var licenceSchedules = allDraftLicenceScheduleDetails.stream()
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
        .filter(licenceScheduleDetail -> matchesLicenseeFilter(
            licenceScheduleDetail.getLicenceSchedule().getLicence(),
            workAreaFilterForm,
            responsibleOrganisations,
            licenseeGroupOrgUnitIds
        ))
        .toList();

    var viewedItemIds = workAreaItemViewService.getWorkAreaItemLogsForUser(
            List.of(WorkAreaDataItemType.DRAFT_LICENCE_SCHEDULE),
            serviceUserDetail.wuaId()
        ).stream()
        .map(WorkAreaItemView::getItemId)
        .collect(Collectors.toSet());

    return licenceSchedules.stream()
        .map(licenceScheduleDetail -> getScheduleWorkAreaItem(
            licenceScheduleDetail,
            responsibleOrganisations,
            viewedItemIds
        ))
        .toList();
  }

  private boolean matchesLicenseeFilter(
      Licence licence,
      WorkAreaFilterForm filterForm,
      Map<Licence, List<OrganisationUnit>> responsibleOrganisations,
      List<Integer> licenseeGroupOrgUnitIds
  ) {
    var licenceUnitIds = licenceResponsibleOrganisationService
        .getOrganisationUnitIdsFromLicenceOrgUnitMap(responsibleOrganisations, licence);

    return FilterUtil.matchesIdList(licenceUnitIds, filterForm.getLicenseeOrgUnitId())
        && FilterUtil.listMatchesIdList(licenceUnitIds, licenseeGroupOrgUnitIds);
  }

  private SearchResultItem getScheduleWorkAreaItem(
      LicenceScheduleDetail licenceScheduleDetail,
      Map<Licence, List<OrganisationUnit>> responsibleOrganisationsByLicences,
      Set<UUID> viewedItemIds
  ) {
    var licence = licenceScheduleDetail.getLicenceSchedule().getLicence();
    var createdDatetime = licenceScheduleDetail.getCreatedInstant();
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
