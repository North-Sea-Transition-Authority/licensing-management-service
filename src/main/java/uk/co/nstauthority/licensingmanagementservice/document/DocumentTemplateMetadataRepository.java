package uk.co.nstauthority.licensingmanagementservice.document;

import java.util.UUID;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface DocumentTemplateMetadataRepository extends ListCrudRepository<DocumentTemplateMetadata, UUID> {
}