package uk.co.nstauthority.licensingmanagementservice.document;

import jakarta.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceType;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;

@Service
public class DocumentTemplateMetadataService {

  private final DocumentTemplateMetadataRepository documentTemplateMetadataRepository;

  @Autowired
  public DocumentTemplateMetadataService(DocumentTemplateMetadataRepository documentTemplateMetadataRepository) {
    this.documentTemplateMetadataRepository = documentTemplateMetadataRepository;
  }

  @Transactional
  public void createDocumentMetadata(UUID documentTemplateId, LicenceType licenceType, ApplicationType applicationType) {
    var documentTemplateMetadata = new DocumentTemplateMetadata();
    documentTemplateMetadata.setDocumentTemplateId(documentTemplateId);
    documentTemplateMetadata.setLicenceType(licenceType);
    documentTemplateMetadata.setApplicationType(applicationType);
    documentTemplateMetadataRepository.save(documentTemplateMetadata);
  }

  public List<DocumentTemplateMetadata> getAllDocumentTemplateMetadata() {
    return documentTemplateMetadataRepository.findAll();
  }

  public Optional<DocumentTemplateMetadata> getDocumentTemplateMetadata(UUID documentTemplateId) {
    return documentTemplateMetadataRepository.findByDocumentTemplateId(documentTemplateId);
  }
}