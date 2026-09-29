package uk.co.nstauthority.licensingmanagementservice.caseevent.casenote;

import jakarta.transaction.Transactional;
import java.time.Clock;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseNotePayload;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;

@Service
public class CaseNoteService {

  private final CaseNoteRepository caseNoteRepository;
  private final CaseEventService caseEventService;
  private final Clock clock;

  public CaseNoteService(
      CaseNoteRepository caseNoteRepository,
      CaseEventService caseEventService,
      Clock clock
  ) {
    this.caseNoteRepository = caseNoteRepository;
    this.caseEventService = caseEventService;
    this.clock = clock;
  }

  @Transactional
  public CaseNote addCaseNote(
      LicenceApplication application,
      String noteText,
      ServiceUserDetail author
  ) {
    var caseNote = caseNoteRepository.save(new CaseNote(
        application.getApplicationType(),
        application.getId(),
        noteText,
        clock.instant(),
        author.wuaId(),
        author.proxyWuaId()
    ));

    caseEventService.recordCaseEvent(CaseEventType.CASE_NOTE_ADDED, application, new CaseNotePayload(caseNote.getId()));
    return caseNote;
  }
}
