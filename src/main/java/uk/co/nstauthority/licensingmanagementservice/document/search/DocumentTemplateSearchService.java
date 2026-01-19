package uk.co.nstauthority.licensingmanagementservice.document.search;

import static uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateFilterUtils.filterDocumentTitle;
import static uk.co.nstauthority.licensingmanagementservice.document.search.DocumentTemplateFilterUtils.filterLicenceType;

import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateDto;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadata;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateMetadataService;
import uk.co.nstauthority.licensingmanagementservice.document.LmsDocumentTemplateDto;
import uk.co.nstauthority.licensingmanagementservice.teams.Role;
import uk.co.nstauthority.licensingmanagementservice.teams.TeamQueryService;

@Service
public class DocumentTemplateSearchService {

  private final DocumentTemplateService documentTemplateService;
  private final DocumentTemplateMetadataService documentTemplateMetadataService;
  private final TeamQueryService teamQueryService;

  @Autowired
  public DocumentTemplateSearchService(DocumentTemplateService documentTemplateService,
                                       DocumentTemplateMetadataService documentTemplateMetadataService,
                                       TeamQueryService teamQueryService) {
    this.documentTemplateService = documentTemplateService;
    this.documentTemplateMetadataService = documentTemplateMetadataService;
    this.teamQueryService = teamQueryService;
  }

  public List<LmsDocumentTemplateDto> getDocumentTemplateSearchItems(DocumentTemplateSearchFilterForm form,
                                                                     ServiceUserDetail serviceUserDetail) {
    return filterDocumentTemplateResults(form, serviceUserDetail);
  }

  private List<LmsDocumentTemplateDto> filterDocumentTemplateResults(DocumentTemplateSearchFilterForm form,
                                                                     ServiceUserDetail serviceUserDetail) {
    var allDocumentTemplateMetadata = documentTemplateMetadataService.getAllDocumentTemplateMetadata();

    var isUserDocumentTemplateManager = teamQueryService.userHasAtLeastOneRoleIn(
        serviceUserDetail.wuaId(), Set.of(Role.DOCUMENT_TEMPLATE_MANAGER));

    return convertToLmsDocumentTemplateDtos(allDocumentTemplateMetadata, isUserDocumentTemplateManager)
        .stream()
        .filter(template -> filterLicenceType(template, form.getLicenceTypes()))
        .filter(template -> filterDocumentTitle(template, form.getDocumentTemplateTitle()))
        .sorted(Comparator.comparing(documentTemplateDto -> documentTemplateDto.title().toLowerCase()))
        .toList();
  }

  private List<LmsDocumentTemplateDto> convertToLmsDocumentTemplateDtos(
      List<DocumentTemplateMetadata> documentTemplateMetadatas,
      boolean isUserDocumentTemplateManager) {
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
                  "" //TODO LMS1-58 user should be taken to the correct screen based on isUserDocumentTemplateManager.
              );
            }
        )
        .toList();
  }
}