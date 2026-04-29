package uk.co.nstauthority.licensingmanagementservice.licence.application.withdraw;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplication;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@ExtendWith(MockitoExtension.class)
class ApplicationWithdrawServiceTest {

  @Mock
  private EmailService emailService;

  @Mock
  private MergedTemplate.MergedTemplateBuilder mergedTemplateBuilder;

  @Mock
  private MergedTemplate mergedTemplateMock;

  @InjectMocks
  private ApplicationWithdrawService applicationWithdrawService;

  private LicenceContinuationApplication licenceContinuationApplication;

  @BeforeEach
  void setUp() {
    UUID applicationId = UUID.randomUUID();
    licenceContinuationApplication = new LicenceContinuationApplication();
    licenceContinuationApplication.setApplicationReference("APP-123");
    licenceContinuationApplication.setId(applicationId);
  }

  @Test
  void sendApplicationWithdrawnEmails_whenValidSubmittersExist() {
    setupEmailTemplateMocks(GovukNotifyTemplate.APPLICATION_WITHDRAWAL_V1);

    var submitter1 = new TeamMemberView(1L, "Mr", "test", "test", "test@test.com", "123", UUID.randomUUID(), List.of(Role.APPLICATION_SUBMITTER), false);
    var submitters = List.of(submitter1);


    applicationWithdrawService.sendApplicationWithdrawnEmails(
        "Test withdrawal reason",
        submitters,
        "CONTINUATION_WITHDRAWAL",
        licenceContinuationApplication
    );

    verify(emailService, times(1)).sendEmail(
        eq(mergedTemplateMock),
        any(EmailRecipient.class),
        any(DomainReference.class)
    );

    verify(mergedTemplateBuilder).withMailMergeField("APPLICATION_TYPE", ApplicationType.CONTINUATION_APPLICATION.getDisplayName());
  }

  @Test
  void sendApplicationWithdrawnEmails_whenSubmittersEmpty_returnsEarly() {
    applicationWithdrawService.sendApplicationWithdrawnEmails(
        "Test withdrawal reason",
        List.of(),
        "CONTINUATION_WITHDRAWAL",
        licenceContinuationApplication
    );

    verifyNoInteractions(emailService);
  }

  private void setupEmailTemplateMocks(GovukNotifyTemplate template) {
    when(emailService.getTemplate(template)).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplateMock);
  }
}