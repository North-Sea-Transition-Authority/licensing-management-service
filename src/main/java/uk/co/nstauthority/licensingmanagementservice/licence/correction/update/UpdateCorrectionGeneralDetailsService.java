package uk.co.nstauthority.licensingmanagementservice.licence.correction.update;

import jakarta.transaction.Transactional;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrection;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamRole;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.ClearDownWorkAreaLogService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
class UpdateCorrectionGeneralDetailsService {

  static final String ALLOCATABLE_USERS_PURPOSE = "Fetch users who can be allocated to a licence correction";

  private final TeamQueryService teamQueryService;
  private final EnergyPortalUserService energyPortalUserService;
  private final LicenceCorrectionService licenceCorrectionService;
  private final ClearDownWorkAreaLogService clearDownWorkAreaLogService;

  UpdateCorrectionGeneralDetailsService(
      TeamQueryService teamQueryService,
      EnergyPortalUserService energyPortalUserService,
      LicenceCorrectionService licenceCorrectionService,
      ClearDownWorkAreaLogService clearDownWorkAreaLogService
  ) {
    this.teamQueryService = teamQueryService;
    this.energyPortalUserService = energyPortalUserService;
    this.licenceCorrectionService = licenceCorrectionService;
    this.clearDownWorkAreaLogService = clearDownWorkAreaLogService;
  }

  Map<String, String> getAllocatableUsers(Licence licence) {
    var requiredRole = LicenceCorrectionRoles.getRequiredRoleForLicenceType(licence.getType());
    if (requiredRole.isEmpty()) {
      return Map.of();
    }

    List<WebUserAccountId> wuaIds = teamQueryService.getAllTeamRolesWithRoles(List.of(requiredRole.get()))
        .stream()
        .map(TeamRole::getWuaId)
        .map(WebUserAccountId::from)
        .distinct()
        .toList();

    return energyPortalUserService.findByWuaIds(wuaIds, ALLOCATABLE_USERS_PURPOSE)
        .stream()
        .sorted(Comparator.comparing(EnergyPortalUserJson::displayName))
        .collect(StreamUtil.toLinkedHashMap(
            energyPortalUserJson -> String.valueOf(energyPortalUserJson.webUserAccountId()),
            EnergyPortalUserJson::displayName
        ));
  }

  @Transactional
  public void updateGeneralDetails(
      LicenceCorrection licenceCorrection,
      String correctionReference,
      String reason,
      long allocatedToWuaId
  ) {
    var reallocated = !Objects.equals(licenceCorrection.getAllocatedToWuaId(), allocatedToWuaId);

    licenceCorrectionService.updateGeneralDetails(licenceCorrection, correctionReference, reason, allocatedToWuaId);

    if (reallocated) {
      clearDownWorkAreaLogService.clearDownViewFor(
          allocatedToWuaId,
          licenceCorrection.getId(),
          WorkAreaDataItemType.LICENCE_CORRECTION
      );
    }
  }
}