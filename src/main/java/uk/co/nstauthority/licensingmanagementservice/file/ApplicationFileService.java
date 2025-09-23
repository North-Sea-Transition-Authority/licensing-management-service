package uk.co.nstauthority.licensingmanagementservice.file;


import static org.springframework.http.HttpStatus.NOT_FOUND;

import jakarta.transaction.Transactional;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.FileUsage;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;

@Service
public class ApplicationFileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(ApplicationFileService.class);

  private final FileService fileService;

  public ApplicationFileService(FileService fileService) {
    this.fileService = fileService;
  }

  @Transactional
  public void saveDocuments(ApplicationFileUsage applicationFileUsage, Collection<UploadedFileForm> uploadedFileForms) {
    var fileIds = uploadedFileForms.stream().map(UploadedFileForm::getFileId).toList();
    var uploadedFiles = fileService.findAll(fileIds);

    uploadedFiles.forEach(uploadedFile -> throwIfFileDoesNotBelongToUsage(uploadedFile, applicationFileUsage));

    var descriptionByFileId = FileUploadLibraryUtils.getFileDescriptionsByFileId(uploadedFileForms);

    for (var uploadedFile : uploadedFiles) {
      fileService.updateUsageAndDescription(
          uploadedFile,
          builder -> buildFileUsage(builder, applicationFileUsage),
          descriptionByFileId.get(uploadedFile.getId())
      );
    }
  }

  public List<UploadedFile> getUploadedFiles(ApplicationFileUsage applicationFileUsage) {
    return fileService.findAll(
        applicationFileUsage.usageId(),
        applicationFileUsage.usageType(),
        applicationFileUsage.documentType()
    );
  }

  public Map<String, List<UploadedFile>> getUploadedFilesGroupedByUsageId(List<String> fileUsageIds, String filedUsageType) {
    return fileService.findAllByUsageIdsWithUsageType(fileUsageIds, filedUsageType)
        .stream()
        .collect(Collectors.groupingBy(
            UploadedFile::getUsageId
        ));
  }

  @Transactional
  public void deleteFiles(ApplicationFileUsage applicationFileUsage) {
    var uploadedFiles = getUploadedFiles(applicationFileUsage);
    uploadedFiles.forEach(fileService::delete);
  }

  public List<UploadedFileForm> getUploadedFileForms(Collection<UUID> fileIds) {
    return fileService
        .findAll(fileIds)
        .stream()
        .map(FileUploadLibraryUtils::asForm)
        .toList();
  }

  public ResponseStatusException getFileNotFoundException(UUID fileId, ApplicationFileUsage applicationFileUsage) {
    return new ResponseStatusException(NOT_FOUND,
        "File [%s] does not exist for %s [%s]".formatted(
            fileId,
            applicationFileUsage.usageType(),
            applicationFileUsage.usageId())
    );
  }

  public void throwIfFileDoesNotBelongToUsage(UploadedFile uploadedFile, ApplicationFileUsage applicationFileUsage) {
    if (!doesFileHaveUsage(uploadedFile)) {
      return;
    }

    if (!Objects.equals(uploadedFile.getUsageId(), applicationFileUsage.usageId())
        || !Objects.equals(uploadedFile.getUsageType(), applicationFileUsage.usageType())
        || !Objects.equals(uploadedFile.getDocumentType(), applicationFileUsage.documentType())) {
      var usageType = applicationFileUsage.usageType();
      LOGGER.warn("Access was attempted to a file not linked to the correct {}", usageType);
      throw getFileNotFoundException(uploadedFile.getId(), applicationFileUsage);
    }
  }

  public boolean doesFileHaveUsage(UploadedFile uploadedFile) {
    return Objects.nonNull(uploadedFile.getUsageId())
        || Objects.nonNull(uploadedFile.getUsageType())
        || Objects.nonNull(uploadedFile.getDocumentType());
  }

  private FileUsage buildFileUsage(FileUsage.Builder fileUsageBuilder, ApplicationFileUsage applicationFileUsage) {
    return fileUsageBuilder
        .withUsageId(applicationFileUsage.usageId())
        .withUsageType(applicationFileUsage.usageType())
        .withDocumentType(applicationFileUsage.documentType())
        .build();
  }

  public boolean fileBelongsToUser(UploadedFile uploadedFile, ServiceUserDetail userDetail) {
    return Objects.equals(uploadedFile.getUploadedBy(), userDetail.wuaId().toString());
  }
}
