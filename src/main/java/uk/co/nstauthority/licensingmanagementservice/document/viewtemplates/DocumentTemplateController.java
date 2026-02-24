package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.authorisation.rules.HasAnyRoleInTeamTypeInterceptorRule;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.Breadcrumbs;
import uk.co.nstauthority.licensingmanagementservice.breadcrumbs.BreadcrumbsUtil;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMailMergeFieldFormatter;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateSearchController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamType;

@Controller
@RequestMapping("document-library/{documentTemplateId}")
@HasAnyRoleInTeamTypeInterceptorRule.HasAnyRoleInTeamType(TeamType.LICENCE_MANAGEMENT)

public class DocumentTemplateController {

  private final LmsDocumentTemplateService lmsDocumentTemplateService;
  private final DocumentTemplateMailMergeFieldFormatter documentTemplateMailMergeFieldFormatter;

  @Autowired
  public DocumentTemplateController(
      LmsDocumentTemplateService lmsDocumentTemplateService,
      DocumentTemplateMailMergeFieldFormatter documentTemplateMailMergeFieldFormatter
  ) {
    this.lmsDocumentTemplateService = lmsDocumentTemplateService;
    this.documentTemplateMailMergeFieldFormatter = documentTemplateMailMergeFieldFormatter;
  }

  @GetMapping("document-overview")
  public ModelAndView renderTemplateOverview(
      @PathVariable UUID documentTemplateId,
      DocumentTemplateDto documentTemplateDto
  ) {
    return getOverviewModelAndView(documentTemplateDto);
  }

  private ModelAndView getOverviewModelAndView(DocumentTemplateDto documentTemplateDto) {
    var documentTemplateSectionsSummaryView = lmsDocumentTemplateService.getDocumentTemplateSectionsSummaryView(
        documentTemplateDto, documentTemplateMailMergeFieldFormatter);

    var modelAndView = new ModelAndView("lms/document/templateOverview")
        .addObject("documentTemplateDto", documentTemplateDto)
        .addObject("accordionId", documentTemplateDto.id())
        .addObject("documentSectionsSummaryView", documentTemplateSectionsSummaryView);

    var breadcrumbs = Breadcrumbs.builder("%s".formatted(documentTemplateDto.title()))
        .addBreadcrumb(
            "Document library",
            ReverseRouter.route(on(DocumentTemplateSearchController.class).renderDocumentTemplateSearch(null, null, null))
        )
        .build();

    BreadcrumbsUtil.addBreadcrumbsToModel(modelAndView, breadcrumbs);

    return modelAndView;
  }
}
