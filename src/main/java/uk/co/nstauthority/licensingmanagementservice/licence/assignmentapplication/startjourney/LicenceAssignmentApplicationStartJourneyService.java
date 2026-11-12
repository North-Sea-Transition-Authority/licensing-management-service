package uk.co.nstauthority.licensingmanagementservice.licence.assignmentapplication.startjourney;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryRoleService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;

@Service
class LicenceAssignmentApplicationStartJourneyService {

  private final IndustryRoleService industryRoleService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final LicenceService licenceService;

  LicenceAssignmentApplicationStartJourneyService(
      IndustryRoleService industryRoleService,
      OrganisationUnitQueryService organisationUnitQueryService,
      LicenceService licenceService
  ) {
    this.industryRoleService = industryRoleService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.licenceService = licenceService;
  }

  Map<String, String> getOrganisationOptions(ServiceUserDetail user) {
    var organisationUnitIds = industryRoleService.getEditorOrSubmitterOrganisationUnitIds(user.wuaId())
        .stream()
        .toList();

    return organisationUnitQueryService.getOrganisationUnitNamesByIds(organisationUnitIds).entrySet().stream()
        .sorted(Map.Entry.comparingByValue(String.CASE_INSENSITIVE_ORDER))
        .collect(StreamUtil.toLinkedHashMap(entry -> String.valueOf(entry.getKey()), Map.Entry::getValue));
  }

  List<Licence> getLicencesOfType(List<Integer> licenceIds, LicenceType licenceType) {
    Map<Integer, Licence> licencesById = licenceService.getLicencesByIds(licenceIds).stream()
        .filter(licence -> licence.getType() == licenceType)
        .collect(Collectors.toMap(Licence::getId, Function.identity()));

    return licenceIds.stream()
        .map(licencesById::get)
        .filter(Objects::nonNull)
        .toList();
  }
}
