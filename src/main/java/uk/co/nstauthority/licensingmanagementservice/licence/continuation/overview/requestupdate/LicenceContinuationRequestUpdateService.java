package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.requestupdate;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateRequestPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailDuplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailRepository;

@Service
public class LicenceContinuationRequestUpdateService {

  private final LicenceContinuationApplicationDetailRepository detailRepository;
  private final LicenceContinuationApplicationDetailDuplicationService duplicationService;
  private final CaseEventService caseEventService;

  public LicenceContinuationRequestUpdateService(
      LicenceContinuationApplicationDetailRepository detailRepository,
      LicenceContinuationApplicationDetailDuplicationService duplicationService,
      CaseEventService caseEventService
  ) {
    this.detailRepository = detailRepository;
    this.duplicationService = duplicationService;
    this.caseEventService = caseEventService;
  }

  public RequestUpdateForm getFormForApplication(LicenceContinuationApplicationDetail applicationDetail) {
    var form = new RequestUpdateForm();

    if (applicationDetail.getUpdateDeadlineDate() != null) {
      form.getUpdateDeadlineDate().setDate(applicationDetail.getUpdateDeadlineDate());
    }
    form.setUpdateReason(applicationDetail.getUpdateReason());

    return form;
  }

  @Transactional
  public void requestUpdate(
      LicenceContinuationApplicationDetail applicationDetail,
      RequestUpdateForm form,
      ServiceUserDetail user
  ) {
    var newDetail = duplicationService
        .createNewDraftLicenceContinuationApplicationDetailVersion(applicationDetail, user);
    form.getUpdateDeadlineDate().getAsLocalDate().ifPresent(newDetail::setUpdateDeadlineDate);
    newDetail.setUpdateReason(form.getUpdateReason());
    detailRepository.save(newDetail);

    caseEventService.recordCaseEvent(
        CaseEventType.APPLICATION_UPDATE_REQUESTED,
        applicationDetail.getLicenceContinuationApplication(),
        new CaseUpdateRequestPayload(newDetail.getUpdateDeadlineDate(), newDetail.getUpdateReason())
    );
  }
}
