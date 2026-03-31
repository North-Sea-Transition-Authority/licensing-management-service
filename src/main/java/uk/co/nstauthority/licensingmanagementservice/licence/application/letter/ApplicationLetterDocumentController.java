package uk.co.nstauthority.licensingmanagementservice.licence.application.letter;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.document.DocumentSectionUtil.getAddSectionPageTitle;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionForm;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionFormValidator;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldViewService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.continuationapplication.ContinuationApplicationHasStatus;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.continuation.LicenceContinuationApplicationStatus;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("/application/{applicationType}/{applicationId}/document/{documentInstanceSectionId}")
@ContinuationApplicationHasStatus(value = LicenceContinuationApplicationStatus.ISSUE_DECISION)
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.CONTINUATION_ISSUER}, teamType = TeamType.REGULATIONS_LICENSING)
})
public class ApplicationLetterDocumentController {

  private final DocumentInstanceSectionService documentInstanceSectionService;
  private final DocumentMailMergeFieldViewService documentMailMergeFieldViewService;
  private final DocumentInstanceSectionFormValidator documentInstanceSectionFormValidator;
  private final ApplicationLetterService applicationLetterService;
  private final ApplicationService applicationService;
  private final ApplicationLetterValidationService applicationLetterValidationService;

  public ApplicationLetterDocumentController(
      DocumentInstanceSectionService documentInstanceSectionService,
      DocumentMailMergeFieldViewService documentMailMergeFieldViewService,
      DocumentInstanceSectionFormValidator documentInstanceSectionFormValidator,
      ApplicationLetterService applicationLetterService,
      ApplicationService applicationService,
      ApplicationLetterValidationService applicationLetterValidationService
  ) {
    this.documentInstanceSectionService = documentInstanceSectionService;
    this.documentMailMergeFieldViewService = documentMailMergeFieldViewService;
    this.documentInstanceSectionFormValidator = documentInstanceSectionFormValidator;
    this.applicationLetterService = applicationLetterService;
    this.applicationService = applicationService;
    this.applicationLetterValidationService = applicationLetterValidationService;
  }

  @GetMapping("/add")
  public ModelAndView renderAddSectionPage(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceSectionId") UUID documentSectionId,
      @RequestParam(name = "section") AddSectionOption addSectionOption
  ) {
    var application = applicationService.getApplication(applicationType, applicationId);
    var documentSectionDto = getDocumentInstanceSectionOrThrowNotFound(documentSectionId);

    return getAddOrEditSectionModelAndView(
        documentSectionDto,
        DocumentInstanceSectionForm.empty(),
        getAddSectionPageTitle(documentSectionDto.title(), addSectionOption),
        application
    );
  }

  @PostMapping("/add")
  public ModelAndView createSection(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceSectionId") UUID documentSectionId,
      @RequestParam(name = "section") AddSectionOption addSectionOption,
      @ModelAttribute("form") DocumentInstanceSectionForm form,
      BindingResult bindingResult
  ) {
    var application = applicationService.getApplication(applicationType, applicationId);
    var documentSectionDto = getDocumentInstanceSectionOrThrowNotFound(documentSectionId);
    var documentInstance = documentSectionDto.documentInstanceDto();

    documentInstanceSectionFormValidator.validate(
        form,
        documentInstance,
        bindingResult
    );

    if (bindingResult.hasErrors()) {
      return getAddOrEditSectionModelAndView(
          documentSectionDto,
          form,
          getAddSectionPageTitle(documentSectionDto.title(), addSectionOption),
          application
      );
    }

    documentInstanceSectionService.createDocumentInstanceSection(
        documentInstance,
        getParentDocumentSectionDto(addSectionOption, documentSectionDto),
        form,
        AddSectionOption.getDisplayOrder(addSectionOption, documentSectionDto.displayOrder())
    );

    return ReverseRouter.redirect(on(ApplicationLetterController.class).renderEditLetterOverview(applicationType, applicationId));
  }

  @GetMapping("/edit")
  public ModelAndView renderEditSectionPage(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceSectionId") UUID documentSectionId
  ) {
    var application = applicationService.getApplication(
        applicationType,
        applicationId
    );
    var documentSectionDto = getDocumentInstanceSectionOrThrowNotFound(documentSectionId);
    var form = DocumentInstanceSectionForm.from(documentSectionDto);
    var bindingResult = applicationLetterValidationService.getDocumentSectionSpecificErrors(form, documentSectionDto);

    return getAddOrEditSectionModelAndView(
        documentSectionDto,
        form,
        "Edit %s".formatted(documentSectionDto.title()),
        application
    )
        .addObject("%sform".formatted(BindingResult.MODEL_KEY_PREFIX), bindingResult);
  }

  @PostMapping("/edit")
  ModelAndView updateSection(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceSectionId") UUID documentSectionId,
      @ModelAttribute("form") DocumentInstanceSectionForm form,
      BindingResult bindingResult
  ) {
    var application = applicationService.getApplication(applicationType, applicationId);
    var documentSectionDto = getDocumentInstanceSectionOrThrowNotFound(documentSectionId);
    var documentInstance = documentSectionDto.documentInstanceDto();

    documentInstanceSectionFormValidator.validate(
        form,
        documentInstance,
        bindingResult
    );

    if (bindingResult.hasErrors()) {
      return getAddOrEditSectionModelAndView(
          documentSectionDto,
          form,
          "Edit %s".formatted(documentSectionDto.title()),
          application
      );
    }

    documentInstanceSectionService.editDocumentInstanceSection(documentSectionDto, form);

    return ReverseRouter.redirect(on(ApplicationLetterController.class).renderEditLetterOverview(applicationType, applicationId));
  }

  @GetMapping("/remove")
  public ModelAndView renderRemoveSectionPage(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceSectionId") UUID documentSectionId
  ) {
    var application = applicationService.getApplication(applicationType, applicationId);
    var documentSectionDto = getDocumentInstanceSectionOrThrowNotFound(documentSectionId);
    var documentInstance = documentSectionDto.documentInstanceDto();

    throwForbiddenIfSectionIsLastSection(
        documentInstance,
        documentSectionId
    );
    return getRemoveModelAndView(
        documentSectionDto,
        application
    );
  }

  @PostMapping("/remove")
  ModelAndView removeSectionPage(
      @PathVariable ApplicationType applicationType,
      @PathVariable UUID applicationId,
      @PathVariable("documentInstanceSectionId") UUID documentSectionId
  ) {
    var documentSectionDto = getDocumentInstanceSectionOrThrowNotFound(documentSectionId);
    var documentInstanceDto = documentSectionDto.documentInstanceDto();

    throwForbiddenIfSectionIsLastSection(documentInstanceDto, documentSectionId);

    documentInstanceSectionService.deleteDocumentInstanceSection(documentSectionDto);

    return ReverseRouter.redirect(on(ApplicationLetterController.class).renderEditLetterOverview(applicationType, applicationId));
  }

  private ModelAndView getAddOrEditSectionModelAndView(
      DocumentInstanceSectionDto documentInstanceSectionDto,
      DocumentInstanceSectionForm form,
      String pageTitle,
      LicenceApplication application
  ) {
    var documentInstance = documentInstanceSectionDto.documentInstanceDto();
    var modelAndView = new ModelAndView("lms/licence/application/documents/addOrEditLetterSection")
        .addObject("documentInstanceSectionDto", documentInstanceSectionDto)
        .addObject("form", form)
        .addObject("pageTitle", pageTitle)
        .addObject("mailMergeFieldViews",
            documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(documentInstance.documentTemplateDto()))
        .addObject("cancelUrl",
            ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(
                application.getApplicationType(),
                application.getId()
            ))
        );

    var breadcrumbs = Breadcrumbs.builder(pageTitle);
    return addBreadcrumbs(
        modelAndView,
        breadcrumbs,
        application
    );
  }

  private DocumentInstanceSectionDto getDocumentInstanceSectionOrThrowNotFound(UUID documentSectionId) {
    try {
      return documentInstanceSectionService.getDocumentInstanceSectionDtoOrThrow(documentSectionId);
    } catch (DocumentInstanceSectionNotFoundException e) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Cannot find document section with id %s".formatted(documentSectionId)
      );
    }
  }

  private DocumentInstanceSectionDto getParentDocumentSectionDto(
      AddSectionOption addSectionOption,
      DocumentInstanceSectionDto currentDocumentSection
  ) {
    try {
      return applicationLetterService.getParentDocumentSectionDto(
          addSectionOption,
          currentDocumentSection
      );
    } catch (DocumentInstanceSectionNotFoundException e) {
      throw new ResponseStatusException(
          HttpStatus.NOT_FOUND,
          "Cannot find document section with id %s".formatted(currentDocumentSection.id())
      );
    }
  }

  private void throwForbiddenIfSectionIsLastSection(
      DocumentInstanceDto documentInstanceDto,
      UUID documentSectionId
  ) {
    if (documentInstanceSectionService.getTopLevelDocumentInstanceSectionDtos(documentInstanceDto).size() <= 1) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Cannot remove last section with id %s from document instance, there must be at least 1 section per template".formatted(
              documentSectionId
          )
      );
    }
  }

  private ModelAndView getRemoveModelAndView(
      DocumentInstanceSectionDto documentSectionDto,
      LicenceApplication application
  ) {
    var modelAndView = new ModelAndView("lms/licence/application/documents/removeLetterSection")
        .addObject("documentSectionDto", documentSectionDto)
        .addObject("cancelUrl",
            ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(
                application.getApplicationType(),
                application.getId()
            ))
        );

    var breadcrumbs = Breadcrumbs.builder("Remove %s".formatted(documentSectionDto.title()));
    return addBreadcrumbs(
        modelAndView,
        breadcrumbs,
        application
    );
  }

  private ModelAndView addBreadcrumbs(
      ModelAndView modelAndView,
      Breadcrumbs.Builder breadcrumbsBuilder,
      LicenceApplication application
  ) {
    var breadcrumbs = breadcrumbsBuilder
        .addBreadcrumb(
            ApplicationLetterController.APPLICATION_LETTERS_PAGE_TITLE,
            ReverseRouter.route(on(ApplicationLetterController.class).renderEditLetterOverview(
                application.getApplicationType(),
                application.getId()
            ))
        )
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(
        modelAndView,
        breadcrumbs
    );

    return modelAndView;
  }
}