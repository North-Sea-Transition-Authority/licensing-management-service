package uk.co.nstauthority.licensingmanagementservice.caseevent;

import jakarta.annotation.Nullable;
import jakarta.transaction.Transactional;
import java.time.Clock;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.audit.AuditRevisionUtil;
import uk.co.nstauthority.licensingmanagementservice.authentication.UserDetailService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseEventPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;

@Service
public class CaseEventService {

  private final CaseEventRepository caseEventRepository;
  private final UserDetailService userDetailService;
  private final Clock clock;

  public CaseEventService(
      CaseEventRepository caseEventRepository,
      UserDetailService userDetailService,
      Clock clock
  ) {
    this.caseEventRepository = caseEventRepository;
    this.userDetailService = userDetailService;
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
}
