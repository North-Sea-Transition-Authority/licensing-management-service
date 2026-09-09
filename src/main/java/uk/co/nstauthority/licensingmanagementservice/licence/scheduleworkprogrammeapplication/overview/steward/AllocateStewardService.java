package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.steward;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.StewardRoles;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.StewardAssignedNotificationService;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.ClearDownWorkAreaLogService;
import uk.co.nstauthority.licensingmanagementservice.workarea.workareaitemview.WorkAreaDataItemType;

@Service
class AllocateStewardService {

  static final String STEWARD_OPTIONS_PURPOSE = "Fetch steward options for allocate steward page";

  private final TeamQueryService teamQueryService;
  private final EnergyPortalUserService energyPortalUserService;
  private final ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository;
  private final ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository;
  private final ClearDownWorkAreaLogService clearDownWorkAreaLogService;
  private final StewardAssignedNotificationService stewardAssignedNotificationService;

  AllocateStewardService(
      TeamQueryService teamQueryService,
      EnergyPortalUserService energyPortalUserService,
      ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository,
      ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository,
      ClearDownWorkAreaLogService clearDownWorkAreaLogService,
      StewardAssignedNotificationService stewardAssignedNotificationService
  ) {
    this.teamQueryService = teamQueryService;
    this.energyPortalUserService = energyPortalUserService;
    this.scheduleWorkProgrammeApplicationRepository = scheduleWorkProgrammeApplicationRepository;
    this.scheduleWorkProgrammeApplicationDetailRepository = scheduleWorkProgrammeApplicationDetailRepository;
    this.clearDownWorkAreaLogService = clearDownWorkAreaLogService;
    this.stewardAssignedNotificationService = stewardAssignedNotificationService;
  }

  Map<String, String> getStewardOptions(LicenceType licenceType) {
    var requiredRole = StewardRoles.getRequiredRoleForLicenceType(licenceType);
    if (requiredRole.isEmpty()) {
      return Map.of();
    }

    var stewardTeamRoles = teamQueryService.getAllTeamRolesWithRoles(Set.of(requiredRole.get()));
    List<WebUserAccountId> wuaIds = stewardTeamRoles.stream()
        .map(teamRole -> WebUserAccountId.from(teamRole.getWuaId()))
        .distinct()
        .toList();
    return energyPortalUserService.findByWuaIds(wuaIds, STEWARD_OPTIONS_PURPOSE)
        .stream()
        .collect(StreamUtil.toLinkedHashMap(
            energyPortalUserJson -> String.valueOf(energyPortalUserJson.webUserAccountId()),
            EnergyPortalUserJson::displayName
        ));
  }

  AllocateStewardForm getFormForApplication(ScheduleWorkProgrammeApplication application) {
    var form = new AllocateStewardForm();
    if (application.getStewardWuaId() != null) {
      form.setStewardWuaId(String.valueOf(application.getStewardWuaId()));
    }
    return form;
  }

  @Transactional
  public void saveSteward(ScheduleWorkProgrammeApplication application, Long stewardWuaId) {
    application.setStewardWuaId(stewardWuaId);
    scheduleWorkProgrammeApplicationRepository.save(application);
    scheduleWorkProgrammeApplicationDetailRepository
        .getFirstByScheduleWorkProgrammeApplicationOrderByVersionNumberDesc(application)
        .ifPresent(detail -> {
          clearDownWorkAreaLogService.clearDownViewFor(
              stewardWuaId,
              detail.getId(),
              WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION
          );
          stewardAssignedNotificationService.sendCaseAssignedEmail(detail, stewardWuaId);
        });
  }
}
