package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.requestupdate;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateRequestPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateEmailService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailDuplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;

@Service
public class RequestUpdateService {

  private final ScheduleWorkProgrammeApplicationDetailRepository detailRepository;
  private final ScheduleWorkProgrammeApplicationDetailDuplicationService duplicationService;
  private final CaseEventService caseEventService;
  private final RequestUpdateEmailService updateRequestedEmailService;

  public RequestUpdateService(
      ScheduleWorkProgrammeApplicationDetailRepository detailRepository,
      ScheduleWorkProgrammeApplicationDetailDuplicationService duplicationService,
      CaseEventService caseEventService,
      RequestUpdateEmailService updateRequestedEmailService
  ) {
    this.detailRepository = detailRepository;
    this.duplicationService = duplicationService;
    this.caseEventService = caseEventService;
    this.updateRequestedEmailService = updateRequestedEmailService;
  }

  public RequestUpdateForm getFormForApplication(ScheduleWorkProgrammeApplicationDetail applicationDetail) {
    var form = new RequestUpdateForm();

    if (applicationDetail.getUpdateDeadlineDate() != null) {
      form.getUpdateDeadlineDate().setDate(applicationDetail.getUpdateDeadlineDate());
    }
    form.setUpdateReason(applicationDetail.getUpdateReason());

    return form;
  }

  @Transactional
  public void requestUpdate(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      RequestUpdateForm form,
      ServiceUserDetail user
  ) {
    var newDetail = duplicationService
        .createNewDraftScheduleWorkProgrammeApplicationDetailVersion(applicationDetail, user);
    form.getUpdateDeadlineDate().getAsLocalDate().ifPresent(newDetail::setUpdateDeadlineDate);
    newDetail.setUpdateReason(form.getUpdateReason());
    detailRepository.save(newDetail);

    caseEventService.recordCaseEvent(
        CaseEventType.APPLICATION_UPDATE_REQUESTED,
        applicationDetail.getScheduleWorkProgrammeApplication(),
        new CaseUpdateRequestPayload(newDetail.getUpdateDeadlineDate(), newDetail.getUpdateReason())
    );

    updateRequestedEmailService.sendUpdateRequestedEmails(newDetail);
  }
}
