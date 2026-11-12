package uk.co.nstauthority.licensingmanagementservice.licence.application.requestupdate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.Month;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.formatting.DateFormatUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.Licence;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.contact.LicenceContactService;
import uk.co.nstauthority.licensingmanagementservice.teams.IndustryTeamService;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@ExtendWith(MockitoExtension.class)
class RequestUpdateEmailServiceTest {

  private static final int ORG_UNIT_ID = 10;
  private static final int ORG_GROUP_ID = 20;
  private static final LocalDate DEADLINE = LocalDate.of(2026, Month.DECEMBER, 1);

  @Mock
  private EmailService emailService;

  @Mock
  private LicenceContactService licenceContactService;

  @Mock
  private IndustryTeamService industryTeamService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private MergedTemplate.MergedTemplateBuilder mergedTemplateBuilder;

  @Mock
  private MergedTemplate mergedTemplate;

  @InjectMocks
  private RequestUpdateEmailService service;

  private LicenceApplicationDetail updateDetail;
  private Licence licence;
  private UUID applicationId;

  @BeforeEach
  void setUp() {
    var application = mock(LicenceApplication.class);
    applicationId = UUID.randomUUID();
    when(application.getId()).thenReturn(applicationId);
    lenient().when(application.getApplicationType()).thenReturn(ApplicationType.CONTINUATION_APPLICATION);

    licence = mock(Licence.class);

    updateDetail = mock(LicenceApplicationDetail.class);
    when(updateDetail.getLicenceApplication()).thenReturn(application);
    when(updateDetail.getLicence()).thenReturn(licence);
    when(updateDetail.getResponsibleOrganisationUnitId()).thenReturn(ORG_UNIT_ID);
  }

  @Test
  void sendUpdateRequestedEmails_sendsToSubmittersAndLicenceContact() {
    givenTemplateMocks();
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(ORG_UNIT_ID)).thenReturn(Optional.of(ORG_GROUP_ID));
    when(industryTeamService.getSubmitterDetails(ORG_GROUP_ID)).thenReturn(List.of(submitter("Submitter@Example.com")));
    when(licenceContactService.getContactEmailForLicensee(licence, ORG_UNIT_ID))
        .thenReturn(Optional.of("contact@example.com"));

    service.sendUpdateRequestedEmails(updateDetail);

    var recipientCaptor = ArgumentCaptor.forClass(EmailRecipient.class);
    var domainReferenceCaptor = ArgumentCaptor.forClass(DomainReference.class);
    verify(emailService, times(2)).sendEmail(eq(mergedTemplate), recipientCaptor.capture(), domainReferenceCaptor.capture());
    assertThat(recipientCaptor.getAllValues())
        .extracting(EmailRecipient::getEmailAddress)
        .containsExactlyInAnyOrder("submitter@example.com", "contact@example.com");
    assertThat(domainReferenceCaptor.getAllValues())
        .extracting(DomainReference::getDomainId, DomainReference::getDomainType)
        .containsOnly(tuple(applicationId.toString(), "LicenceApplication"));
    verify(mergedTemplateBuilder).withMailMergeField("REQUEST_DEADLINE", DateFormatUtil.convertToDisplayText(DEADLINE));
    verify(mergedTemplateBuilder).withMailMergeField("REQUEST_REASON", "Further information is needed");
  }

  @Test
  void sendUpdateRequestedEmails_whenLicenceContactIsAlsoSubmitter_sendsOnlyOnce() {
    givenTemplateMocks();
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(ORG_UNIT_ID)).thenReturn(Optional.of(ORG_GROUP_ID));
    when(industryTeamService.getSubmitterDetails(ORG_GROUP_ID)).thenReturn(List.of(submitter("Submitter@Example.com")));
    when(licenceContactService.getContactEmailForLicensee(licence, ORG_UNIT_ID))
        .thenReturn(Optional.of("submitter@example.com"));

    service.sendUpdateRequestedEmails(updateDetail);

    var recipientCaptor = ArgumentCaptor.forClass(EmailRecipient.class);
    verify(emailService, times(1)).sendEmail(eq(mergedTemplate), recipientCaptor.capture(), any(DomainReference.class));
    assertThat(recipientCaptor.getValue().getEmailAddress()).isEqualTo("submitter@example.com");
  }

  @Test
  void sendUpdateRequestedEmails_whenNoOrganisationGroup_stillSendsToLicenceContact() {
    givenTemplateMocks();
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(ORG_UNIT_ID)).thenReturn(Optional.empty());
    when(licenceContactService.getContactEmailForLicensee(licence, ORG_UNIT_ID)).thenReturn(Optional.of("contact@example.com"));

    service.sendUpdateRequestedEmails(updateDetail);

    var recipientCaptor = ArgumentCaptor.forClass(EmailRecipient.class);
    verify(emailService, times(1)).sendEmail(eq(mergedTemplate), recipientCaptor.capture(), any(DomainReference.class));
    assertThat(recipientCaptor.getValue().getEmailAddress()).isEqualTo("contact@example.com");
  }

  @Test
  void sendUpdateRequestedEmails_whenNoRecipients_sendsNothing() {
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(ORG_UNIT_ID)).thenReturn(Optional.of(ORG_GROUP_ID));
    when(industryTeamService.getSubmitterDetails(ORG_GROUP_ID)).thenReturn(List.of());
    when(licenceContactService.getContactEmailForLicensee(licence, ORG_UNIT_ID)).thenReturn(Optional.empty());

    service.sendUpdateRequestedEmails(updateDetail);

    verifyNoInteractions(emailService);
  }

  private void givenTemplateMocks() {
    when(updateDetail.getUpdateDeadlineDate()).thenReturn(DEADLINE);
    when(updateDetail.getUpdateReason()).thenReturn("Further information is needed");
    when(updateDetail.getLicenceApplication().getApplicationReference()).thenReturn("LMS/CA/2026/1/1");
    when(emailService.getTemplate(GovukNotifyTemplate.APPLICATION_UPDATE_REQUESTED_V1)).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplate);
  }

  private TeamMemberView submitter(String email) {
    return new TeamMemberView(
        1L, "Mr", "Sub", "Mitter", email, "123", UUID.randomUUID(), List.of(Role.APPLICATION_SUBMITTER), false
    );
  }
}
