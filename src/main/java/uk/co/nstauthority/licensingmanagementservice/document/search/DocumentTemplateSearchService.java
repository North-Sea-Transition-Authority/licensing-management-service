package uk.co.nstauthority.licensingmanagementservice.document.search;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateFilterUtils.filterDocumentTitle;
import static uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateFilterUtils.filterLicenceType;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadata;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadataService;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentTemplateController;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;

@Service
public class DocumentTemplateSearchService {

  private final DocumentTemplateService documentTemplateService;
  private final DocumentTemplateMetadataService documentTemplateMetadataService;

  @Autowired
  public DocumentTemplateSearchService(
      DocumentTemplateService documentTemplateService,
      DocumentTemplateMetadataService documentTemplateMetadataService
  ) {
    this.documentTemplateService = documentTemplateService;
    this.documentTemplateMetadataService = documentTemplateMetadataService;
  }

  public List<LmsDocumentTemplateDto> getDocumentTemplateSearchItems(DocumentTemplateSearchFilterForm form) {
    return filterDocumentTemplateResults(form);
  }

  private List<LmsDocumentTemplateDto> filterDocumentTemplateResults(DocumentTemplateSearchFilterForm form) {
    var allDocumentTemplateMetadata = documentTemplateMetadataService.getAllDocumentTemplateMetadata();

    return convertToLmsDocumentTemplateDtos(allDocumentTemplateMetadata)
        .stream()
        .filter(template -> filterLicenceType(template, form.getLicenceTypes()))
        .filter(template -> filterDocumentTitle(template, form.getDocumentTemplateTitle()))
        .sorted(Comparator.comparing(documentTemplateDto -> documentTemplateDto.title().toLowerCase()))
        .toList();
  }

  private List<LmsDocumentTemplateDto> convertToLmsDocumentTemplateDtos(
      List<DocumentTemplateMetadata> documentTemplateMetadatas
  ) {
    var documentIdToTemplateMap = documentTemplateService.getDocumentTemplateDtos()
        .stream()
        .collect(Collectors.toMap(
            DocumentTemplateDto::id,
            template -> template
        ));

    return documentTemplateMetadatas.stream()
        .map(
            documentTemplateMetadata -> {
              var template = documentIdToTemplateMap.get(documentTemplateMetadata.getDocumentTemplateId());
              return LmsDocumentTemplateDto.from(
                  documentTemplateMetadata,
                  template,
                  ReverseRouter.route(on(DocumentTemplateController.class).renderTemplateOverview(template.id(), null))
              );
            }
        )
        .toList();
  }
}