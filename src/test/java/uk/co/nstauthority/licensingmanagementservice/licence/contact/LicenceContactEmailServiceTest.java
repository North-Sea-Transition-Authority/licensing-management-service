package uk.co.nstauthority.licensingmanagementservice.licence.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;

@ExtendWith(MockitoExtension.class)
class LicenceContactEmailServiceTest {

  private static final String CONTACT_EMAIL = "contact@example.com";
  private static final String LICENSEE_NAME = "Shell U.K. Limited";
  private static final Integer ORG_ID = 10;

  @Mock
  private EmailService emailService;

  @Mock
  private MergedTemplate.MergedTemplateBuilder mergedTemplateBuilder;

  @Mock
  private MergedTemplate mergedTemplate;

  @InjectMocks
  private LicenceContactEmailService licenceContactEmailService;

  @Captor
  private ArgumentCaptor<EmailRecipient> emailRecipientCaptor;

  @Captor
  private ArgumentCaptor<DomainReference> domainReferenceCaptor;

  @BeforeEach
  void setUp() {
    when(emailService.getTemplate(GovukNotifyTemplate.LICENCE_CONTACT_UPDATED_V1)).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplate);
  }

  @Test
  void sendContactUpdatedEmail_mergesExpectedFieldsAndSendsToContact() {
    licenceContactEmailService.sendContactUpdatedEmail(
        CONTACT_EMAIL,
        LICENSEE_NAME,
        List.of("P 123", "P 456"),
        ORG_ID
    );

    verify(mergedTemplateBuilder).withMailMergeField("LICENCE_REFERENCES", "P 123, P 456");
    verify(mergedTemplateBuilder).withMailMergeField("LICENSEE_NAME", LICENSEE_NAME);

    verify(emailService).sendEmail(
        eq(mergedTemplate),
        emailRecipientCaptor.capture(),
        domainReferenceCaptor.capture()
    );
    assertThat(emailRecipientCaptor.getValue().getEmailAddress()).isEqualTo(CONTACT_EMAIL);
    assertThat(domainReferenceCaptor.getValue().getDomainId()).isEqualTo(ORG_ID.toString());
    assertThat(domainReferenceCaptor.getValue().getDomainType())
        .isEqualTo(LicenceContactEmailService.DOMAIN_REFERENCE_TYPE);
  }

  @Test
  void sendContactUpdatedEmail_whenSendFails_doesNotThrow() {
    doThrow(new RuntimeException("notify unavailable"))
        .when(emailService)
        .sendEmail(eq(mergedTemplate), any(EmailRecipient.class), any(DomainReference.class));

    licenceContactEmailService.sendContactUpdatedEmail(
        CONTACT_EMAIL,
        LICENSEE_NAME,
        List.of("P 123"),
        ORG_ID
    );

    verify(emailService).sendEmail(eq(mergedTemplate), any(EmailRecipient.class), any(DomainReference.class));
  }
}
