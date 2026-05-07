package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentLinkingService;
import uk.co.nstauthority.licensingmanagementservice.document.instance.LmsDocumentInstanceService;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.letter.ContinuationApplicationDocumentActionsController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/application/{applicationType}/{applicationId}")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.ISSUE_DECISION)
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.CONTINUATION_ISSUER}, teamType = TeamType.REGULATIONS_LICENSING)
})
public class ApplicationLetterController {

  static final String APPLICATION_LETTERS_PAGE_TITLE = "Application letters";

  private final LmsDocumentInstanceService lmsDocumentInstanceService;
  private final ApplicationLetterService applicationLetterService;
  private final ApplicationService applicationService;
  private final DocumentLinkingService documentLinkingService;
  private final ApplicationLetterValidationService applicationLetterValidationService;

  @Autowired
  ApplicationLetterController(
      LmsDocumentInstanceService lmsDocumentInstanceService,
      ApplicationLetterService applicationLetterService,
      ApplicationService applicationService,
      DocumentLinkingService documentLinkingService,
      ApplicationLetterValidationService applicationLetterValidationService
  ) {
    this.lmsDocumentInstanceService = lmsDocumentInstanceService;
    this.applicationLetterService = applicationLetterService;
    this.applicationService = applicationService;
    this.documentLinkingService = documentLinkingService;
    this.applicationLetterValidationService = applicationLetterValidationService;
  }

  @GetMapping("/letter/edit")
  public ModelAndView renderEditLetterOverview(
      ApplicationType applicationType,
      @PathVariable UUID applicationId
  ) {
    LicenceApplication application = applicationService.getApplication(applicationType, applicationId);
    DocumentInstanceDto documentInstance = applicationLetterService.getDocumentInstance(application);
    var documentInstanceSectionsSummaryView = lmsDocumentInstanceService.getDocumentInstanceSectionsSummaryView(
        documentInstance,
        true,
        application
    );

    return getOverviewModelAndView(
        applicationId,
        applicationType,
        documentInstance,
        documentInstanceSectionsSummaryView)
            .addObject("errorList",
                       applicationLetterValidationService.getDocumentSectionOverviewError(documentInstanceSectionsSummaryView));
  }

  private ModelAndView getOverviewModelAndView(
      UUID applicationId,
      ApplicationType applicationType,
      DocumentInstanceDto documentInstanceDto,
      DocumentInstanceSectionsSummaryView documentInstanceSectionsSummaryView
  ) {
    var modelAndView = new ModelAndView("lms/licence/application/letter/editLetterOverview");

    var hasMoreThanOneSection = documentInstanceSectionsSummaryView.topLevelDocumentInstanceSectionSummaryViews().size() > 1;
    var organisationName = documentLinkingService.getApplicationCompanyNameFromDto(documentInstanceDto);

    var pageTitle = "%s for %s".formatted(
        documentInstanceDto.title(),
        organisationName
    );

    var breadcrumbs = Breadcrumbs.builder("%s".formatted(documentInstanceDto.title()))
        .addWorkAreaBreadcrumb()
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView
        .addObject("documentInstanceDto", documentInstanceDto)
        .addObject("pageTitle", pageTitle)
        .addObject("accordionId", documentInstanceDto.id())
        .addObject("documentInstanceSectionsSummaryView", documentInstanceSectionsSummaryView)
        .addObject("hasMoreThanOneSection", hasMoreThanOneSection)
        .addObject("userHasValidPermission", true)
        .addObject(
            "reloadUrl",
            ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderReloadDocumentPage(
                applicationType,
                applicationId,
                documentInstanceDto.id()
            ))
        )
        .addObject(
            "previewUrl",
            ReverseRouter.route(on(ApplicationDocumentActionsController.class).renderPreviewPdf(
                applicationType,
                applicationId,
                documentInstanceDto.id(),
                null
            ))
        )
        .addObject(
            "signUrl",
            ReverseRouter.route(on(ContinuationApplicationDocumentActionsController.class).approveAndSignDocument(
                applicationType,
                applicationId,
                documentInstanceDto.id(),
                null,
                null
            ))
        );
  }
}