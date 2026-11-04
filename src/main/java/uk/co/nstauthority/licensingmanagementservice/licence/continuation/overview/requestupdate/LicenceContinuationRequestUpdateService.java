package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.requestupdate;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailDuplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailRepository;

@Service
public class LicenceContinuationRequestUpdateService {

  private final LicenceContinuationApplicationDetailRepository detailRepository;
  private final LicenceContinuationApplicationDetailDuplicationService duplicationService;

  public LicenceContinuationRequestUpdateService(
      LicenceContinuationApplicationDetailRepository detailRepository,
      LicenceContinuationApplicationDetailDuplicationService duplicationService
  ) {
    this.detailRepository = detailRepository;
    this.duplicationService = duplicationService;
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
  public LicenceContinuationApplicationDetail requestUpdate(
      LicenceContinuationApplicationDetail applicationDetail,
      RequestUpdateForm form,
      ServiceUserDetail user
  ) {
    var newDetail = duplicationService
        .createNewDraftLicenceContinuationApplicationDetailVersion(applicationDetail, user);
    form.getUpdateDeadlineDate().getAsLocalDate().ifPresent(newDetail::setUpdateDeadlineDate);
    newDetail.setUpdateReason(form.getUpdateReason());
    return detailRepository.save(newDetail);
  }
}
