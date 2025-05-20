package uk.co.nstauthority.template.file;

import java.util.UUID;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.core.UploadedFileTestUtil;

public class XyzApplicationFileTestUtil {

  static final String FILE_DESCRIPTION = "file description";

  public static UploadedFile createUploadFile() {
    return UploadedFileTestUtil.newBuilder()
        .withId(UUID.randomUUID())
        .withDescription(FILE_DESCRIPTION)
        .build();
  }

  public static UploadedFile createdUploadedFile(XyzApplicationFileUsage fileUsage) {
    var uploadedFile = createUploadFile();
    uploadedFile.setUsageId(fileUsage.usageId());
    uploadedFile.setUsageType(fileUsage.usageType());
    uploadedFile.setDocumentType(fileUsage.documentType());
    return uploadedFile;
  }
}
