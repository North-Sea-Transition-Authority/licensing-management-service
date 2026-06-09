package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.steward;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationAccessService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationRepository;
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

  AllocateStewardService(
      TeamQueryService teamQueryService,
      EnergyPortalUserService energyPortalUserService,
      ScheduleWorkProgrammeApplicationRepository scheduleWorkProgrammeApplicationRepository,
      ScheduleWorkProgrammeApplicationDetailRepository scheduleWorkProgrammeApplicationDetailRepository,
      ClearDownWorkAreaLogService clearDownWorkAreaLogService
  ) {
    this.teamQueryService = teamQueryService;
    this.energyPortalUserService = energyPortalUserService;
    this.scheduleWorkProgrammeApplicationRepository = scheduleWorkProgrammeApplicationRepository;
    this.scheduleWorkProgrammeApplicationDetailRepository = scheduleWorkProgrammeApplicationDetailRepository;
    this.clearDownWorkAreaLogService = clearDownWorkAreaLogService;
  }

  Map<String, String> getStewardOptions() {
    var stewardTeamRoles = teamQueryService.getAllTeamRolesWithRoles(ApplicationAccessService.STEWARD_ROLES);
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
        .ifPresent(detail -> clearDownWorkAreaLogService.clearDownViewFor(
            stewardWuaId,
            detail.getId(),
            WorkAreaDataItemType.SCHEDULE_WORK_PROGRAMME_APPLICATION
        ));
  }
}
