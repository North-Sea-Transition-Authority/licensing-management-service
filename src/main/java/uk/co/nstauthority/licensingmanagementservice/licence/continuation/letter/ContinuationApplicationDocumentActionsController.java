package uk.co.nstauthority.licensingmanagementservice.licence.continuation.letter;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentItemType;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.fds.notificationbanner.NotificationBanner;
import uk.co.nstauthority.licensingmanagementservice.file.FileUsageType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.letter.IssueLettersService;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationService;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;
import uk.co.nstauthority.licensingmanagementservice.workarea.WorkAreaController;

@Controller
@RequestMapping("/application/{applicationType}/{applicationId}/document/{documentInstanceId}")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.ISSUE_DECISION)
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.CONTINUATION_ISSUER}, teamType = TeamType.REGULATIONS_LICENSING)
})
public class ContinuationApplicationDocumentActionsController {

  private final ApplicationService applicationService;
  private final DocumentInstanceService documentInstanceService;
  private final LmsDocumentInstanceService lmsDocumentInstanceService;
  private final IssueLettersService issueLettersService;
  private final LicenceContinuationService licenceContinuationService;

  @Autowired
  public ContinuationApplicationDocumentActionsController(
      ApplicationService applicationService,
      DocumentInstanceService documentInstanceService,
      LmsDocumentInstanceService lmsDocumentInstanceService,
      IssueLettersService issueLettersService,
      LicenceContinuationService licenceContinuationService
  ) {
    this.applicationService = applicationService;
    this.documentInstanceService = documentInstanceService;
    this.lmsDocumentInstanceService = lmsDocumentInstanceService;
    this.issueLettersService = issueLettersService;
    this.licenceContinuationService = licenceContinuationService;
  }

  @PostMapping("approve")
  public ModelAndView approveAndSignDocument(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceId") UUID documentInstanceId,
      RedirectAttributes redirectAttributes,
      ServiceUserDetail user
  ) {
    var application = applicationService.getApplication(applicationType, applicationId);
    var documentInstance = getDocumentInstanceDtoOrThrowNotFound(documentInstanceId);

    issueLettersService.saveApplicationLetterToS3(
        documentInstance,
        application,
        user,
        false,
        lmsDocumentInstanceService,
        FileUsageType.APPLICATION_CONTINUATION_LETTER.getUsageType(),
        DocumentItemType.CONTINUATION_LETTER.name()
    );

    NotificationBanner.newSuccessBannerWithHeader(
        "Successfully approved the document %s".formatted(documentInstance.title()),
        redirectAttributes
    );

    licenceContinuationService.issueContinuationLetter(application);

    return ReverseRouter.redirect(on(WorkAreaController.class).getWorkArea(null, null));
  }

  private DocumentInstanceDto getDocumentInstanceDtoOrThrowNotFound(UUID documentInstanceId) {
    try {
      return documentInstanceService.getDocumentInstanceDtoOrThrow(documentInstanceId);
    } catch (DocumentInstanceNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }
}