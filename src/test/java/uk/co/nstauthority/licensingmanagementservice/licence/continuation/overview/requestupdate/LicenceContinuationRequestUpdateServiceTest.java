package uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.requestupdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventService;
import uk.co.nstauthority.licensingmanagementservice.caseevent.CaseEventType;
import uk.co.nstauthority.licensingmanagementservice.caseevent.payload.CaseUpdateRequestPayload;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateEmailService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate.RequestUpdateForm;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailDuplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetailRepository;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationRequestUpdateServiceTest {

  private static final LocalDate DEADLINE = LocalDate.of(2024, Month.APRIL, 1);
  private static final String REASON = "Further information is needed";

  @Mock
  private LicenceContinuationApplicationDetailRepository detailRepository;

  @Mock
  private LicenceContinuationApplicationDetailDuplicationService duplicationService;

  @Mock
  private CaseEventService caseEventService;

  @Mock
  private RequestUpdateEmailService updateRequestedEmailService;

  @InjectMocks
  private LicenceContinuationRequestUpdateService service;

  @Test
  void getFormForApplication_whenUpdateAlreadyRequested_populatesForm() {
    var applicationDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    applicationDetail.setUpdateDeadlineDate(DEADLINE);
    applicationDetail.setUpdateReason(REASON);

    var form = service.getFormForApplication(applicationDetail);

    assertThat(form.getUpdateDeadlineDate().getAsLocalDate()).isEqualTo(Optional.of(DEADLINE));
    assertThat(form.getUpdateReason()).isEqualTo(REASON);
  }

  @Test
  void getFormForApplication_whenNoUpdateRequested_leavesFormBlank() {
    var applicationDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());

    var form = service.getFormForApplication(applicationDetail);

    assertThat(form.getUpdateDeadlineDate().getAsLocalDate()).isEmpty();
    assertThat(form.getUpdateReason()).isNull();
  }

  @Test
  void requestUpdate() {
    var user = ServiceUserDetailTestUtil.newBuilder().withWuaId(5L).build();
    var oldDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    var application = new LicenceContinuationApplication();
    oldDetail.setLicenceContinuationApplication(application);
    var newDetail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    var form = new RequestUpdateForm();
    form.getUpdateDeadlineDate().setDate(DEADLINE);
    form.setUpdateReason(REASON);

    when(duplicationService.createNewDraftLicenceContinuationApplicationDetailVersion(oldDetail, user))
        .thenReturn(newDetail);

    service.requestUpdate(oldDetail, form, user);

    assertThat(newDetail.getUpdateDeadlineDate()).isEqualTo(DEADLINE);
    assertThat(newDetail.getUpdateReason()).isEqualTo(REASON);
    assertThat(oldDetail.getUpdateDeadlineDate()).isNull();
    assertThat(oldDetail.getUpdateReason()).isNull();
    verify(detailRepository).save(newDetail);
    verify(caseEventService).recordCaseEvent(CaseEventType.APPLICATION_UPDATE_REQUESTED, application,
        new CaseUpdateRequestPayload(DEADLINE, REASON));
    verify(updateRequestedEmailService).sendUpdateRequestedEmails(newDetail);
  }
}
