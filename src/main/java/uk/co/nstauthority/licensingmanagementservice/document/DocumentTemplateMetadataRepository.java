package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.Optional;
import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;
import uk.co.nstauthority.licensingmanagementservice.duplication.NotDuplicationSource;

@Repository
public interface DocumentTemplateMetadataRepository
    extends ListCrudRepository<DocumentTemplateMetadata, UUID>, NotDuplicationSource {
  Optional<DocumentTemplateMetadata> findByDocumentTemplateId(UUID documentTemplateId);
}