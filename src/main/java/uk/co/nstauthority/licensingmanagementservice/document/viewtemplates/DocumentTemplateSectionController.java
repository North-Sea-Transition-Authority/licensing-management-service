package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.document.DocumentSectionUtil.getAddSectionPageTitle;

import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Autowired;
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
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldViewService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionConditionService;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionNotFoundException;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateSectionService;
import uk.co.nstauthority.licensingmanagementservice.authorisation.HasRolesInTeamType;
import uk.co.nstauthority.licensingmanagementservice.authorisation.RolesAndTeamType;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.document.AddSectionOption;
import uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateSearchController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("document-library/document-section/{documentSectionId}")
@HasRolesInTeamType(value = {
    @RolesAndTeamType(roles = {Role.DOCUMENT_TEMPLATE_MANAGER}, teamType = TeamType.LICENCE_MANAGEMENT)
})
public class DocumentTemplateSectionController {

  private static final Function<String, String> EDIT_PAGE_NAME = "Edit %s"::formatted;

  private final DocumentTemplateSectionService documentTemplateSectionService;
  private final DocumentMailMergeFieldViewService documentMailMergeFieldViewService;
  private final LmsDocumentTemplateSectionFormValidator documentTemplateSectionFormValidator;
  private final DocumentTemplateSectionConditionService documentTemplateSectionConditionService;

  @Autowired
  public DocumentTemplateSectionController(
      DocumentTemplateSectionService documentTemplateSectionService,
      DocumentMailMergeFieldViewService documentMailMergeFieldViewService,
      DocumentTemplateSectionConditionService documentTemplateSectionConditionService,
      LmsDocumentTemplateSectionFormValidator lmsDocumentTemplateSectionFormValidator
  ) {
    this.documentTemplateSectionService = documentTemplateSectionService;
    this.documentMailMergeFieldViewService = documentMailMergeFieldViewService;
    this.documentTemplateSectionFormValidator = lmsDocumentTemplateSectionFormValidator;
    this.documentTemplateSectionConditionService = documentTemplateSectionConditionService;
  }

  @GetMapping("/add")
  public ModelAndView renderAddSectionPage(
      @PathVariable UUID documentSectionId,
      @RequestParam(name = "section") AddSectionOption addSectionOption
  ) {
    var documentSectionDto = getDocumentTemplateSectionOrThrowNotFound(documentSectionId);
    return getAddOrEditSectionModelAndView(
        documentSectionDto,
        new LmsDocumentTemplateSectionForm(),
        getAddSectionPageTitle(documentSectionDto.title(), addSectionOption)
    );
  }

  @PostMapping("/add")
  public ModelAndView createSection(
      @PathVariable UUID documentSectionId,
      @RequestParam(name = "section") AddSectionOption addSectionOption,
      @ModelAttribute("form") LmsDocumentTemplateSectionForm form,
      BindingResult bindingResult
  ) {
    var documentSectionDto = getDocumentTemplateSectionOrThrowNotFound(documentSectionId);
    var documentTemplateDto = documentSectionDto.documentTemplateDto();

    documentTemplateSectionFormValidator.validate(form, bindingResult, documentTemplateDto);

    if (bindingResult.hasErrors()) {
      return getAddOrEditSectionModelAndView(
          documentSectionDto,
          form,
          getAddSectionPageTitle(documentSectionDto.title(), addSectionOption)
      );
    }

    var parentDto = switch (addSectionOption) {
      case ADD_BEFORE, ADD_AFTER -> documentSectionDto.parentId() != null
          ? documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(documentSectionDto.parentId())
          : null;
      case ADD_SUBSECTION -> documentSectionDto;
    };

    documentTemplateSectionService.createDocumentTemplateSection(
        documentTemplateDto,
        parentDto,
        form,
        AddSectionOption.getDisplayOrder(addSectionOption, documentSectionDto.displayOrder())
    );

    return ReverseRouter.redirect(on(DocumentTemplateController.class)
        .renderTemplateOverview(documentTemplateDto.id(), null));
  }

  @GetMapping("/edit")
  public ModelAndView renderEditSectionPage(@PathVariable UUID documentSectionId) {
    var documentSectionDto = getDocumentTemplateSectionOrThrowNotFound(documentSectionId);
    return getAddOrEditSectionModelAndView(
        documentSectionDto,
        LmsDocumentTemplateSectionForm.setDocumentTemplateProperties(documentSectionDto),
        EDIT_PAGE_NAME.apply(documentSectionDto.title())
    );
  }

  @PostMapping("/edit")
  ModelAndView updateSection(
      @PathVariable UUID documentSectionId,
      @ModelAttribute("form") LmsDocumentTemplateSectionForm form,
      BindingResult bindingResult
  ) {
    var documentSectionDto = getDocumentTemplateSectionOrThrowNotFound(documentSectionId);
    var documentTemplateDto = documentSectionDto.documentTemplateDto();

    documentTemplateSectionFormValidator.validate(form, bindingResult, documentTemplateDto);

    if (bindingResult.hasErrors()) {
      return getAddOrEditSectionModelAndView(
          documentSectionDto,
          form,
          EDIT_PAGE_NAME.apply(documentSectionDto.title())
      );
    }

    documentTemplateSectionService.editDocumentTemplateSection(documentSectionDto, form);

    return ReverseRouter.redirect(on(DocumentTemplateController.class)
        .renderTemplateOverview(documentTemplateDto.id(), null));
  }

  @GetMapping("/remove")
  public ModelAndView renderRemoveSectionPage(@PathVariable UUID documentSectionId) {
    var documentSectionDto = getDocumentTemplateSectionOrThrowNotFound(documentSectionId);
    var documentTemplateDto = documentSectionDto.documentTemplateDto();

    throwForbiddenIfSectionIsLastSection(documentTemplateDto, documentSectionId);
    return getRemoveModelAndView(documentSectionDto, new LmsDocumentTemplateSectionForm());
  }

  @PostMapping("/remove")
  ModelAndView removeSectionPage(
      @PathVariable UUID documentSectionId,
      @ModelAttribute("form") LmsDocumentTemplateSectionForm form,
      BindingResult bindingResult
  ) {
    var documentSectionDto = getDocumentTemplateSectionOrThrowNotFound(documentSectionId);
    var documentTemplateDto = documentSectionDto.documentTemplateDto();

    throwForbiddenIfSectionIsLastSection(documentTemplateDto, documentSectionId);

    if (bindingResult.hasErrors()) {
      return getRemoveModelAndView(documentSectionDto, form);
    }

    documentTemplateSectionService.deleteDocumentTemplateSection(documentSectionDto);

    return ReverseRouter.redirect(on(DocumentTemplateController.class)
        .renderTemplateOverview(documentSectionDto.documentTemplateDto().id(), null));
  }

  private ModelAndView getAddOrEditSectionModelAndView(
      DocumentTemplateSectionDto documentTemplateSectionDto,
      LmsDocumentTemplateSectionForm form,
      String pageTitle
  ) {

    var documentTemplateDto = documentTemplateSectionDto.documentTemplateDto();
    var modelAndView = new ModelAndView("lms/document/sections/addOrEditSection")
        .addObject("documentTemplateSectionDto", documentTemplateSectionDto)
        .addObject("form", form)
        .addObject("pageTitle", pageTitle)
        .addObject(
            "mailMergeFieldViews",
            documentMailMergeFieldViewService.getApplicableDocumentMailMergeFieldViews(documentTemplateDto)
        )
        .addObject("cancelUrl", ReverseRouter.route(on(DocumentTemplateController.class)
            .renderTemplateOverview(documentTemplateDto.id(), null)))
        .addObject(
            "conditionsFdsSelectMap",
            documentTemplateSectionConditionService.getConditionsFdsSelectMap(documentTemplateDto)
        );

    var breadcrumbs = Breadcrumbs.builder(pageTitle);
    return addDocumentLibraryBreadcrumbs(modelAndView, documentTemplateDto, breadcrumbs);
  }

  private ModelAndView getRemoveModelAndView(
      DocumentTemplateSectionDto documentSectionDto,
      LmsDocumentTemplateSectionForm form
  ) {
    var modelAndView = new ModelAndView("lms/document/sections/removeSection")
        .addObject("documentSectionDto", documentSectionDto)
        .addObject("form", form)
        .addObject("cancelUrl", ReverseRouter.route(on(DocumentTemplateController.class)
            .renderTemplateOverview(documentSectionDto.documentTemplateDto().id(), null)));

    var breadcrumbs = Breadcrumbs.builder("Remove %s".formatted(documentSectionDto.title()));
    return addDocumentLibraryBreadcrumbs(modelAndView, documentSectionDto.documentTemplateDto(), breadcrumbs);
  }

  private DocumentTemplateSectionDto getDocumentTemplateSectionOrThrowNotFound(UUID documentSectionId) {
    try {
      return documentTemplateSectionService.getDocumentTemplateSectionDtoOrThrow(documentSectionId);
    } catch (DocumentTemplateSectionNotFoundException e) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND,
          "Cannot find document section with id %s".formatted(documentSectionId));
    }
  }

  private ModelAndView addDocumentLibraryBreadcrumbs(
      ModelAndView modelAndView,
      DocumentTemplateDto documentTemplateDto,
      Breadcrumbs.Builder breadcrumbsBuilder
  ) {

    var breadcrumbs = breadcrumbsBuilder
        .addBreadcrumb(
            "Document library",
            ReverseRouter.route(on(DocumentTemplateSearchController.class).renderDocumentTemplateSearch(null, null, null))
        )
        .addBreadcrumb(
            "%s".formatted(documentTemplateDto.title()),
            ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(documentTemplateDto.id(), null))
        )
        .build();
    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }

  private void throwForbiddenIfSectionIsLastSection(DocumentTemplateDto documentTemplateDto, UUID documentSectionId) {
    if (documentTemplateSectionService.getTopLevelDocumentTemplateSectionDtos(documentTemplateDto).size() <= 1) {
      throw new ResponseStatusException(
          HttpStatus.FORBIDDEN,
          "Cannot remove last section with id %s from document template, there must be at least 1 section per template"
              .formatted(documentSectionId)
      );
    }
  }
}
