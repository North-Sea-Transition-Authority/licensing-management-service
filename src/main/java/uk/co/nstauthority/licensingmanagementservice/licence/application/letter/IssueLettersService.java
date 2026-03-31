package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitalnotificationlibrary.core.notification.DomainReference;
import uk.co.fivium.digitalnotificationlibrary.core.notification.MergedTemplate;
import uk.co.fivium.digitalnotificationlibrary.core.notification.email.EmailRecipient;
import uk.co.fivium.energyportal.serviceproviders.epmq.ScopeType;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.FileSource;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentItemType;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.email.EmailService;
import uk.co.nstauthority.licensingmanagementservice.email.GovukNotifyTemplate;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.caseprocessing.OverviewTab;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.overview.LicenceContinuationApplicationOverviewController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamScopeReference;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.teams.management.TeamManagementService;
import uk.co.nstauthority.licensingmanagementservice.teams.management.view.TeamMemberView;

@Service
public class IssueLettersService {

  private static final Logger LOGGER = LoggerFactory.getLogger(IssueLettersService.class);

  private final FileService fileService;
  private final EmailService emailService;
  private final OrganisationUnitQueryService organisationUnitQueryService;
  private final TeamManagementService teamManagementService;

  public IssueLettersService(
      FileService fileService,
      EmailService emailService,
      OrganisationUnitQueryService organisationUnitQueryService,
      TeamManagementService teamManagementService
  ) {
    this.fileService = fileService;
    this.emailService = emailService;
    this.organisationUnitQueryService = organisationUnitQueryService;
    this.teamManagementService = teamManagementService;
  }

  @Transactional
  public void saveApplicationLetterToS3(
      DocumentInstanceDto documentInstanceDto,
      LicenceApplication licenceApplication,
      ServiceUserDetail serviceUserDetail,
      boolean isPreview,
      LmsDocumentInstanceService lmsDocumentInstanceService
  ) {
    var sectionsSummaryViews = lmsDocumentInstanceService.getDocumentInstanceSectionsSummaryView(
            documentInstanceDto,
            false,
            licenceApplication
        )
        .topLevelDocumentInstanceSectionSummaryViews();

    var renderResult = lmsDocumentInstanceService.renderAndSignPdf(
        licenceApplication,
        isPreview,
        documentInstanceDto,
        sectionsSummaryViews,
        serviceUserDetail
    );

    var pdfContent = renderResult.pdfContent();
    var prefix = isPreview ? "PREVIEW " : "";
    var title = "%s%s.pdf".formatted(prefix, documentInstanceDto.title());

    fileService.upload(builder -> builder
        .withUsage(
            licenceApplication.getId().toString(),
            FileUsageType.APPLICATION_CONTINUATION_LETTER.getUsageType(),
            DocumentItemType.CONTINUATION_LETTER.name()
        )
        .withFileSource(FileSource.fromInputStreamSource(
            pdfContent::getInputStream,
            title,
            "application/pdf",
            pdfContent.contentLength()
        ))
        .withUploadedBy(serviceUserDetail.wuaId().toString())
        .build());
  }

  public void sendContinuationIssuanceEmails(
      LicenceApplication application,
      LicenceContinuationApplicationDetail licenceContinuationApplicationDetail
  ) {
    List<TeamMemberView> submitters = getSubmitterDetails(licenceContinuationApplicationDetail);

    if (submitters.isEmpty()) {
      return;
    }

    var applicationId = application.getId();
    MergedTemplate.MergedTemplateBuilder template = emailService.getTemplate(GovukNotifyTemplate.CONTINUATION_LETTER_ISSUED)
        .withMailMergeField("APPLICATION_REFERENCE", application.getId().toString())
        .withMailMergeField(
            "DOCUMENT_OVERVIEW_LINK",
            ReverseRouter.route(on(LicenceContinuationApplicationOverviewController.class).renderOverview(
                applicationId,
                licenceContinuationApplicationDetail,
                null,
                OverviewTab.DECISION
            ))
        );

    for (TeamMemberView submitter : submitters) {
      var mergedTemplate =  template.withMailMergeField("USER_NAME", submitter.getDisplayName()).merge();

      try {
        emailService.sendEmail(
            mergedTemplate,
            EmailRecipient.directEmailAddress(submitter.email()),
            DomainReference.from(application.getId().toString(), DocumentItemType.CONTINUATION_LETTER.name())
        );
      } catch (Exception e) {
        LOGGER.error("Failed to send Continuation issuance email to {} for Application ID: {}",
                     submitter.email(), application.getId(), e);
      }
    }
  }

  private List<TeamMemberView> getSubmitterDetails(LicenceContinuationApplicationDetail continuationDetail) {
    var organisationGroupId = organisationUnitQueryService.findOrganisationGroupIdByUnitId(
        continuationDetail.getResponsibleOrganisationUnitId()
    );

    if (organisationGroupId.isEmpty()) {
      return List.of();
    }

    var scopeRef = TeamScopeReference.from(
        String.valueOf(organisationGroupId.get()),
        ScopeType.ORGANISATION_GROUP.name()
    );

    var teamOptional = teamManagementService.getScopedTeam(TeamType.ORGANISATION, scopeRef);

    if (teamOptional.isEmpty()) {
      return List.of();
    }

    return teamManagementService.getActiveTeamMembersViewsForTeamAndRole(
        teamOptional.get(),
        Role.APPLICATION_SUBMITTER
    );
  }

}
