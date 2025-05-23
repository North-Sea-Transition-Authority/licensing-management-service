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
public class XyzApplicationFileService {

  private static final Logger LOGGER = LoggerFactory.getLogger(XyzApplicationFileService.class);

  private final FileService fileService;

  public XyzApplicationFileService(FileService fileService) {
    this.fileService = fileService;
  }

  @Transactional
  public void saveDocuments(XyzApplicationFileUsage fileUsage, Collection<UploadedFileForm> uploadedFileForms) {
    var fileIds = uploadedFileForms.stream().map(UploadedFileForm::getFileId).toList();
    var uploadedFiles = fileService.findAll(fileIds);

    uploadedFiles.forEach(uploadedFile -> throwIfFileDoesNotBelongToUsage(uploadedFile, fileUsage));

    var descriptionByFileId = FileUploadLibraryUtils.getFileDescriptionsByFileId(uploadedFileForms);

    for (var uploadedFile : uploadedFiles) {
      fileService.updateUsageAndDescription(
          uploadedFile,
          builder -> buildFileUsage(builder, fileUsage),
          descriptionByFileId.get(uploadedFile.getId())
      );
    }
  }

  public List<UploadedFile> getUploadedFiles(XyzApplicationFileUsage fileUsage) {
    return fileService.findAll(fileUsage.usageId(), fileUsage.usageType(), fileUsage.documentType());
  }

  public Map<String, List<UploadedFile>> getUploadedFilesGroupedByUsageId(List<String> fileUsageIds, String filedUsageType) {
    return fileService.findAllByUsageIdsWithUsageType(fileUsageIds, filedUsageType)
        .stream()
        .collect(Collectors.groupingBy(
            UploadedFile::getUsageId
        ));
  }

  @Transactional
  public void deleteFiles(XyzApplicationFileUsage fileUsage) {
    var uploadedFiles = getUploadedFiles(fileUsage);
    uploadedFiles.forEach(fileService::delete);
  }

  public List<UploadedFileForm> getUploadedFileForms(Collection<UUID> fileIds) {
    return fileService
        .findAll(fileIds)
        .stream()
        .map(FileUploadLibraryUtils::asForm)
        .toList();
  }

  public ResponseStatusException getFileNotFoundException(UUID fileId, XyzApplicationFileUsage fileUsage) {
    return new ResponseStatusException(NOT_FOUND,
        "File [%s] does not exist for %s [%s]".formatted(fileId, fileUsage.usageType(), fileUsage.usageId()));
  }

  public void throwIfFileDoesNotBelongToUsage(UploadedFile uploadedFile, XyzApplicationFileUsage fileUsage) {
    if (!doesFileHaveUsage(uploadedFile)) {
      return;
    }

    if (!Objects.equals(uploadedFile.getUsageId(), fileUsage.usageId())
        || !Objects.equals(uploadedFile.getUsageType(), fileUsage.usageType())
        || !Objects.equals(uploadedFile.getDocumentType(), fileUsage.documentType())) {
      var usageType = fileUsage.usageType();
      LOGGER.warn("Access was attempted to a file not linked to the correct {}", usageType);
      throw getFileNotFoundException(uploadedFile.getId(), fileUsage);
    }
  }

  public boolean doesFileHaveUsage(UploadedFile uploadedFile) {
    return Objects.nonNull(uploadedFile.getUsageId())
        || Objects.nonNull(uploadedFile.getUsageType())
        || Objects.nonNull(uploadedFile.getDocumentType());
  }

  private FileUsage buildFileUsage(FileUsage.Builder fileUsageBuilder, XyzApplicationFileUsage fileUsage) {
    return fileUsageBuilder
        .withUsageId(fileUsage.usageId())
        .withUsageType(fileUsage.usageType())
        .withDocumentType(fileUsage.documentType())
        .build();
  }

  public boolean fileBelongsToUser(UploadedFile uploadedFile, ServiceUserDetail userDetail) {
    return Objects.equals(uploadedFile.getUploadedBy(), userDetail.wuaId().toString());
  }
}
