package uk.co.nstauthority.licensingmanagementservice.file;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;

@RestController
@RequestMapping("unlinked-files")
public class UnlinkedFileUploadController {

  private final FileControllerHelperService fileControllerHelperService;

  @Autowired
  public UnlinkedFileUploadController(FileControllerHelperService fileControllerHelperService) {
    this.fileControllerHelperService = fileControllerHelperService;
  }

  @PostMapping
  public ResponseEntity<FileUploadResponse> upload(MultipartFile file, ServiceUserDetail userDetail) {
    return fileControllerHelperService.upload(file, userDetail);
  }
}
