package uk.co.nstauthority.licensingmanagementservice.file;

import java.util.UUID;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileTestUtil;

public class ApplicationFileTestUtil {

  static final String FILE_DESCRIPTION = "file description";

  public static UploadedFile createUploadFile() {
    return UploadedFileTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withDescription(FILE_DESCRIPTION)
        .build();
  }

  public static UploadedFile createdUploadedFile(ApplicationFileUsage applicationFileUsage) {
    var uploadedFile = createUploadFile();
    uploadedFile.setUsageId(applicationFileUsage.usageId());
    uploadedFile.setUsageType(applicationFileUsage.usageType());
    uploadedFile.setDocumentType(applicationFileUsage.documentType());
    return uploadedFile;
  }
}
