package uk.co.nstauthority.licensingmanagementservice.caseevent;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.audit.AuditRevisionUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseEventPayload;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserJson;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.EnergyPortalUserService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.user.WebUserAccountId;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;

@Service
public class CaseEventService {

  static final String CASE_EVENT_USER_PURPOSE = "Fetch user names for application case events";
  static final String SYSTEM_USER_DISPLAY_NAME = "System";

  private final CaseEventRepository caseEventRepository;
  private final UserDetailService userDetailService;
  private final EnergyPortalUserService energyPortalUserService;
  private final Clock clock;

  public CaseEventService(
      CaseEventRepository caseEventRepository,
      UserDetailService userDetailService,
      EnergyPortalUserService energyPortalUserService,
      Clock clock
  ) {
    this.caseEventRepository = caseEventRepository;
    this.userDetailService = userDetailService;
    this.energyPortalUserService = energyPortalUserService;
    this.clock = clock;
  }

  @Transactional
  public CaseEvent recordCaseEvent(
      CaseEventType eventType,
      LicenceApplication application,
      @Nullable CaseEventPayload payload
  ) {
    var caseEvent = new CaseEvent(
        application.getApplicationType(),
        application.getId(),
        eventType,
        clock.instant(),
        getCurrentUser(),
        payload
    );
    return caseEventRepository.save(caseEvent);
  }

  public List<CaseEventView> getCaseEventViews(LicenceApplication application) {
    var caseEvents = caseEventRepository.findByApplicationTypeAndApplicationIdOrderByEventInstantDesc(
        application.getApplicationType(),
        application.getId()
    );

    if (caseEvents.isEmpty()) {
      return List.of();
    }

    // One call to the Energy Portal for every user, rather than one per event.
    var usersByWuaId = energyPortalUserService.getEnergyPortalUserMap(
        getDisplayUserWuaIds(caseEvents),
        CASE_EVENT_USER_PURPOSE
    );

    return caseEvents.stream()
        .map(caseEvent -> createViewFrom(caseEvent, usersByWuaId))
        .toList();
  }

  private List<WebUserAccountId> getDisplayUserWuaIds(List<CaseEvent> caseEvents) {
    var wuaIds = new ArrayList<WebUserAccountId>();
    for (var caseEvent : caseEvents) {
      var wuaId = getDisplayUserWuaId(caseEvent);
      if (wuaId != null && !wuaIds.contains(WebUserAccountId.from(wuaId))) {
        wuaIds.add(WebUserAccountId.from(wuaId));
      }
    }
    return wuaIds;
  }

  private CaseEventUser getCurrentUser() {
    var loggedInUser = userDetailService.findUserDetail();
    if (loggedInUser.isPresent()) {
      return CaseEventUser.from(loggedInUser.get());
    }

    var fallbackUser = AuditRevisionUtil.getFallbackAuditUser();
    if (fallbackUser != null) {
      return CaseEventUser.from(fallbackUser);
    }

    return CaseEventUser.system();
  }

  private CaseEventView createViewFrom(
      CaseEvent caseEvent,
      Map<WebUserAccountId, EnergyPortalUserJson> usersByWuaId
  ) {
    return new CaseEventView(
        caseEvent.getEventType(),
        getEventBy(caseEvent, usersByWuaId),
        caseEvent.getEventInstant()
    );
  }

  private static String getEventBy(CaseEvent caseEvent, Map<WebUserAccountId, EnergyPortalUserJson> usersByWuaId) {
    if (caseEvent.isSystem()) {
      return SYSTEM_USER_DISPLAY_NAME;
    }

    var user = usersByWuaId.get(WebUserAccountId.from(getDisplayUserWuaId(caseEvent)));
    if (user == null) {
      return null;
    }

    return "%s (%s)".formatted(user.displayName(), user.emailAddress());
  }

  private static Long getDisplayUserWuaId(CaseEvent caseEvent) {
    return caseEvent.getProxyWuaId() != null
        ? caseEvent.getProxyWuaId()
        : caseEvent.getUserWuaId();
  }
}
