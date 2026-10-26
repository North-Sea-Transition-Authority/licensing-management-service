package uk.co.nstauthority.licensingmanagementservice.workarea;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisationgroup.OrganisationGroupQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.OrganisationUnit;
import uk.co.nstauthority.licensingmanagementservice.licence.licenceresponsibleorganisation.LicenceResponsibleOrganisationService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemView;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaItemViewService;

/**
 * Looks up the data the work area sections share once for all of them, rather than once per section: one query and
 * one Energy Portal call for every section's licensees, one Energy Portal call for their organisation groups and one
 * query for the items the user has viewed.
 */
@Service
class WorkAreaContextService {

  private final LicenceResponsibleOrganisationService licenceResponsibleOrganisationService;
  private final OrganisationGroupQueryService organisationGroupQueryService;
  private final WorkAreaItemViewService workAreaItemViewService;

  WorkAreaContextService(
      LicenceResponsibleOrganisationService licenceResponsibleOrganisationService,
      OrganisationGroupQueryService organisationGroupQueryService,
      WorkAreaItemViewService workAreaItemViewService
  ) {
    this.licenceResponsibleOrganisationService = licenceResponsibleOrganisationService;
    this.organisationGroupQueryService = organisationGroupQueryService;
    this.workAreaItemViewService = workAreaItemViewService;
  }

  WorkAreaContext buildContext(
      List<WorkAreaSection> sections,
      WorkAreaFilterForm workAreaFilterForm,
      ServiceUserDetail serviceUserDetail
  ) {
    var licenseesByLicence = getLicenseesByLicence(sections);

    return new WorkAreaContext(
        licenseesByLicence,
        getOrganisationGroupIdsByUnitId(sections, licenseesByLicence),
        getLicenseeGroupOrganisationUnitIds(workAreaFilterForm),
        getViewedItemIdsByType(sections, serviceUserDetail)
    );
  }

  private Map<Licence, List<OrganisationUnit>> getLicenseesByLicence(List<WorkAreaSection> sections) {
    var licences = sections.stream()
        .flatMap(section -> section.licences().stream())
        .distinct()
        .toList();

    return licences.isEmpty()
        ? Map.of()
        : licenceResponsibleOrganisationService.getResponsibleOrganisationsByLicences(licences);
  }

  private Map<Integer, Integer> getOrganisationGroupIdsByUnitId(
      List<WorkAreaSection> sections,
      Map<Licence, List<OrganisationUnit>> licenseesByLicence
  ) {
    var organisationUnitIds = sections.stream()
        .filter(section -> section.organisationGroupLookup() == OrganisationGroupLookup.REQUIRED)
        .flatMap(section -> section.licences().stream())
        .flatMap(licence -> licenseesByLicence.getOrDefault(licence, List.of()).stream())
        .map(OrganisationUnit::organisationUnitId)
        .filter(Objects::nonNull)
        .distinct()
        .toList();

    return organisationUnitIds.isEmpty()
        ? Map.of()
        : licenceResponsibleOrganisationService.getOrgGroupIdByOrgUnitIds(organisationUnitIds);
  }

  private List<Integer> getLicenseeGroupOrganisationUnitIds(WorkAreaFilterForm workAreaFilterForm) {
    return workAreaFilterForm.getLicenseeOrgGroupId() == null
        ? null
        : organisationGroupQueryService.getOrganisationUnitIdsByOrganisationGroupId(
            workAreaFilterForm.getLicenseeOrgGroupId());
  }

  private Map<WorkAreaDataItemType, Set<UUID>> getViewedItemIdsByType(
      List<WorkAreaSection> sections,
      ServiceUserDetail serviceUserDetail
  ) {
    var itemTypes = sections.stream()
        .map(WorkAreaSection::itemType)
        .distinct()
        .toList();

    if (itemTypes.isEmpty()) {
      return Map.of();
    }

    return workAreaItemViewService.getWorkAreaItemLogsForUser(itemTypes, serviceUserDetail.wuaId()).stream()
        .collect(Collectors.groupingBy(
            WorkAreaItemView::getItemType,
            Collectors.mapping(WorkAreaItemView::getItemId, Collectors.toSet())
        ));
  }
}
