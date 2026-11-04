package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overview.requestupdate;

import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailDuplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetailRepository;

@Service
public class RequestUpdateService {

  private final ScheduleWorkProgrammeApplicationDetailRepository detailRepository;
  private final ScheduleWorkProgrammeApplicationDetailDuplicationService duplicationService;

  public RequestUpdateService(
      ScheduleWorkProgrammeApplicationDetailRepository detailRepository,
      ScheduleWorkProgrammeApplicationDetailDuplicationService duplicationService
  ) {
    this.detailRepository = detailRepository;
    this.duplicationService = duplicationService;
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
  public ScheduleWorkProgrammeApplicationDetail requestUpdate(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      RequestUpdateForm form,
      ServiceUserDetail user
  ) {
    var newDetail = duplicationService
        .createNewDraftScheduleWorkProgrammeApplicationDetailVersion(applicationDetail, user);
    form.getUpdateDeadlineDate().getAsLocalDate().ifPresent(newDetail::setUpdateDeadlineDate);
    newDetail.setUpdateReason(form.getUpdateReason());
    return detailRepository.save(newDetail);
  }
}
