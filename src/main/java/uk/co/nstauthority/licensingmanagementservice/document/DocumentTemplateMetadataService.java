package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentTemplateService;

@Service
public class DocumentTemplateMetadataService {

  private final DocumentTemplateMetadataRepository documentTemplateMetadataRepository;

  @Autowired
  public DocumentTemplateMetadataService(DocumentTemplateMetadataRepository documentTemplateMetadataRepository,
                                         DocumentTemplateService documentTemplateService) {
    this.documentTemplateMetadataRepository = documentTemplateMetadataRepository;
  }

  public List<DocumentTemplateMetadata> getAllDocumentTemplateMetadata() {
    return documentTemplateMetadataRepository.findAll();
  }
}