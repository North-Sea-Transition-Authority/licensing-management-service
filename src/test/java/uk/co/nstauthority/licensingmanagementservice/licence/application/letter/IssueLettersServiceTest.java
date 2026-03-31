package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;
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
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.Team;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@ExtendWith(MockitoExtension.class)
class IssueLettersServiceTest {

  @Mock
  private EmailService emailService;

  @Mock
  private OrganisationUnitQueryService organisationUnitQueryService;

  @Mock
  private TeamManagementService teamManagementService;

  @Mock
  private LicenceApplication licenceApplication;

  @Mock
  private LicenceContinuationApplicationDetail licenceContinuationApplicationDetail;

  @Mock
  private Team team;

  @Mock
  private MergedTemplate.MergedTemplateBuilder mergedTemplateBuilder;

  @Mock
  private MergedTemplate mergedTemplateMock;

  @InjectMocks
  private IssueLettersService issueLettersService;

  private UUID applicationId;
  private final int orgUnitId = 100;
  private final int orgGroupId = 200;

  @BeforeEach
  void setUp() {
    applicationId = UUID.randomUUID();
  }

  private void setupEmailTemplateMocks() {
    when(emailService.getTemplate(GovukNotifyTemplate.CONTINUATION_LETTER_ISSUED)).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.withMailMergeField(anyString(), anyString())).thenReturn(mergedTemplateBuilder);
    when(mergedTemplateBuilder.merge()).thenReturn(mergedTemplateMock);
  }

  @Test
  void sendContinuationIssuanceEmails_whenValidSubmittersExist() {
    setupEmailTemplateMocks();
    when(licenceApplication.getId()).thenReturn(applicationId);
    when(licenceContinuationApplicationDetail.getResponsibleOrganisationUnitId()).thenReturn(orgUnitId);
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(orgUnitId)).thenReturn(Optional.of(orgGroupId));
    when(teamManagementService.getScopedTeam(eq(TeamType.ORGANISATION), any(TeamScopeReference.class))).thenReturn(Optional.of(team));

    var submitter1 = new TeamMemberView(1L, "Mr", "test", "test", "test@test.com", "123", UUID.randomUUID(), List.of(Role.APPLICATION_SUBMITTER), false);
    var submitter2 = new TeamMemberView(2L, "Ms", "test", "test", "test@test.com", "456", UUID.randomUUID(), List.of(Role.APPLICATION_SUBMITTER), false);

    when(teamManagementService.getActiveTeamMembersViewsForTeamAndRole(team, Role.APPLICATION_SUBMITTER))
        .thenReturn(List.of(submitter1, submitter2));

    issueLettersService.sendContinuationIssuanceEmails(
        licenceApplication,
        licenceContinuationApplicationDetail
    );

    verify(emailService, times(2)).sendEmail(
        eq(mergedTemplateMock),
        any(EmailRecipient.class),
        any(DomainReference.class)
    );
  }

  @Test
  void sendContinuationIssuanceEmails_whenOrgGroupNotFound_returnsEarly() {
    when(licenceContinuationApplicationDetail.getResponsibleOrganisationUnitId()).thenReturn(orgUnitId);
    when(organisationUnitQueryService.findOrganisationGroupIdByUnitId(orgUnitId)).thenReturn(Optional.empty());

    issueLettersService.sendContinuationIssuanceEmails(
        licenceApplication,
        licenceContinuationApplicationDetail
    );

    verifyNoInteractions(emailService);
    verify(teamManagementService, never()).getScopedTeam(any(), any());
  }

}