package uk.co.nstauthority.template.file;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.util.unit.DataSize;
import uk.co.fivium.fileuploadlibrary.FileUploadLibraryUtils;
import uk.co.fivium.fileuploadlibrary.core.FileService;
import uk.co.fivium.fileuploadlibrary.core.FileSource;
import uk.co.fivium.fileuploadlibrary.core.FileUploadRequest;
import uk.co.fivium.fileuploadlibrary.core.UploadedFile;
import uk.co.fivium.fileuploadlibrary.fds.FileDeleteResponse;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadComponentAttributes;
import uk.co.fivium.fileuploadlibrary.fds.FileUploadResponse;
import uk.co.fivium.fileuploadlibrary.fds.UploadedFileForm;
import uk.co.nstauthority.template.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.template.mvc.ReverseRouter;

@SuppressWarnings("unchecked")
@ExtendWith(MockitoExtension.class)
class FileControllerHelperServiceTest {

  @Mock
  private FileService fileService;

  @Mock
  private XyzApplicationFileService xyzApplicationFileService;

  @Spy
  @InjectMocks
  private FileControllerHelperService fileControllerHelperService;

  private final XyzApplicationFileUsage testFileUsage = new TestFileUsage("usageId", "usageType", "documentType");

  @Test
  void fileUploadComponentAttributes() {
    var uploadedFileForms = List.of(new UploadedFileForm(), new UploadedFileForm(), new UploadedFileForm());
    var fileUploadAttributesBuilder = FileUploadComponentAttributes.newBuilder()
        .withMaximumSize(DataSize.ofMegabytes(1))
        .withAllowedExtensions(Set.of("pdf"));

    when(fileService.getFileUploadAttributes()).thenReturn(fileUploadAttributesBuilder);

    assertThat(fileControllerHelperService.fileUploadComponentAttributes(
        uploadedFileForms,
        TestFileController.class,
        controller -> controller.download(null),
        controller -> controller.delete(null)
    ))
        .extracting(
            FileUploadComponentAttributes::path,
            FileUploadComponentAttributes::uploadUrl,
            FileUploadComponentAttributes::downloadUrl,
            FileUploadComponentAttributes::deleteUrl,
            FileUploadComponentAttributes::existingFiles
        ).containsExactly(
            "form.documents",
            ReverseRouter.route(on(UnlinkedFileUploadController.class).upload(null, null)),
            ReverseRouter.route(on(TestFileController.class).download(null)),
            ReverseRouter.route(on(TestFileController.class).delete(null)),
            uploadedFileForms
        );
  }

  @Test
  void fileUploadComponentAttributes_withPathParam() {
    var path = "form.supportingInformation";
    var uploadedFileForms = List.of(new UploadedFileForm(), new UploadedFileForm(), new UploadedFileForm());
    var fileUploadAttributesBuilder = FileUploadComponentAttributes.newBuilder()
        .withMaximumSize(DataSize.ofMegabytes(1))
        .withAllowedExtensions(Set.of("pdf"))
        .withPath(path);

    when(fileService.getFileUploadAttributes()).thenReturn(fileUploadAttributesBuilder);

    assertThat(fileControllerHelperService.fileUploadComponentAttributes(
        uploadedFileForms,
        TestFileController.class,
        controller -> controller.download(null),
        controller -> controller.delete(null),
        path
    ))
        .extracting(
            FileUploadComponentAttributes::path,
            FileUploadComponentAttributes::uploadUrl,
            FileUploadComponentAttributes::downloadUrl,
            FileUploadComponentAttributes::deleteUrl,
            FileUploadComponentAttributes::existingFiles
        ).containsExactly(
            path,
            ReverseRouter.route(on(UnlinkedFileUploadController.class).upload(null, null)),
            ReverseRouter.route(on(TestFileController.class).download(null)),
            ReverseRouter.route(on(TestFileController.class).delete(null)),
            uploadedFileForms
        );
  }

  @Test
  void fileUploadComponentAttributes_withPathParam_andAllowedExtensionsParam() {
    var path = "form.supportingInformation";
    var allowedExtensions = Set.of("png", "jpg", "jpeg", "pdf");
    var uploadedFileForms = List.of(new UploadedFileForm(), new UploadedFileForm(), new UploadedFileForm());
    var fileUploadAttributesBuilder = FileUploadComponentAttributes.newBuilder()
        .withMaximumSize(DataSize.ofMegabytes(1))
        .withAllowedExtensions(Set.of("pdf"))
        .withPath(path)
        .withAllowedExtensions(allowedExtensions);

    when(fileService.getFileUploadAttributes()).thenReturn(fileUploadAttributesBuilder);

    assertThat(fileControllerHelperService.fileUploadComponentAttributes(
        uploadedFileForms,
        TestFileController.class,
        controller -> controller.download(null),
        controller -> controller.delete(null),
        path,
        allowedExtensions
    ))
        .extracting(
            FileUploadComponentAttributes::path,
            FileUploadComponentAttributes::uploadUrl,
            FileUploadComponentAttributes::downloadUrl,
            FileUploadComponentAttributes::deleteUrl,
            FileUploadComponentAttributes::existingFiles,
            FileUploadComponentAttributes::allowedExtensions
        ).containsExactly(
            path,
            ReverseRouter.route(on(UnlinkedFileUploadController.class).upload(null, null)),
            ReverseRouter.route(on(TestFileController.class).download(null)),
            ReverseRouter.route(on(TestFileController.class).delete(null)),
            uploadedFileForms,
            String.join(", ", FileUploadLibraryUtils.getFdsCompatibleFileExtensions(allowedExtensions))
        );
  }

  @Test
  void upload() {
    var multipartFile = new MockMultipartFile("document.pdf", new byte[]{1, 2, 3, 4, 5});
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();

    var fileId = UUID.randomUUID();
    var fileSource = FileSource.fromMultipartFile(multipartFile);
    var fileUploadResponse = FileUploadResponse.success(fileId, fileSource);

    ArgumentCaptor<Function<FileUploadRequest.Builder, FileUploadRequest>> fileUploadRequestBuilderFunctionCaptor = ArgumentCaptor.forClass(Function.class);

    when(fileService.upload(fileUploadRequestBuilderFunctionCaptor.capture())).thenReturn(fileUploadResponse);

    var responseEntity = fileControllerHelperService.upload(multipartFile, userDetail);
    assertThat(responseEntity)
        .extracting(ResponseEntity::getStatusCode, ResponseEntity::getBody)
        .containsExactly(HttpStatus.OK, fileUploadResponse);

    var fileUploadRequestBuilder = FileUploadRequest.newBuilder()
        .withBucket("bucket")
        .withMaximumSize(DataSize.ofMegabytes(1))
        .withFileExtensions(Set.of("pdf"));

    fileUploadRequestBuilderFunctionCaptor.getValue().apply(fileUploadRequestBuilder);

    assertThat(fileUploadRequestBuilder.build())
        .extracting(
            FileUploadRequest::usageId,
            FileUploadRequest::usageType,
            FileUploadRequest::documentType,
            FileUploadRequest::uploadedBy,
            FileUploadRequest::fileSource
        ).containsExactly(
            null, // this is set on form submission rather than file upload, so should be null
            null, // same as above
            null, // same as above
            userDetail.wuaId().toString(),
            fileSource
        );
  }

  @Test
  void download() {
    var fileId = UUID.randomUUID();
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    Supplier<XyzApplicationFileUsage> usageSupplier = () -> testFileUsage;
    ArgumentCaptor<Function<UploadedFile, ResponseEntity<InputStreamResource>>> fileServiceDownloadFunctionCaptor = ArgumentCaptor.forClass(Function.class);
    ResponseEntity<InputStreamResource> responseEntity = ResponseEntity.ok().build();

    doReturn(responseEntity)
        .when(fileControllerHelperService)
        .findFileAndThen(eq(fileId), eq(usageSupplier), eq(userDetail), fileServiceDownloadFunctionCaptor.capture());

    when(fileService.download(uploadedFile)).thenReturn(responseEntity);

    assertThat(fileControllerHelperService.download(fileId, usageSupplier, userDetail)).isEqualTo(responseEntity);
    assertThat(fileServiceDownloadFunctionCaptor.getValue().apply(uploadedFile)).isEqualTo(responseEntity);
  }

  @Test
  void delete() {
    var fileId = UUID.randomUUID();
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    var deleteResponse = FileDeleteResponse.success(fileId);
    Supplier<XyzApplicationFileUsage> usageSupplier = () -> testFileUsage;
    ArgumentCaptor<Function<UploadedFile, ResponseEntity<FileDeleteResponse>>> fileServiceDeleteFunctionCaptor = ArgumentCaptor.forClass(Function.class);
    ResponseEntity<FileDeleteResponse> responseEntity = ResponseEntity.ok(deleteResponse);

    doReturn(responseEntity)
        .when(fileControllerHelperService)
        .findFileAndThen(eq(fileId), eq(usageSupplier), eq(userDetail), fileServiceDeleteFunctionCaptor.capture());

    when(fileService.delete(uploadedFile)).thenReturn(deleteResponse);

    assertThat(fileControllerHelperService.delete(fileId, usageSupplier, userDetail)).isEqualTo(responseEntity);
    assertThat(fileServiceDeleteFunctionCaptor.getValue().apply(uploadedFile)).isEqualTo(responseEntity);
  }

  @Test
  void findFileAndThen_fileExists_hasUsage_valid() {
    var fileId = UUID.randomUUID();
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    var result = ResponseEntity.ok(fileId);
    Supplier<XyzApplicationFileUsage> usageSupplier = () -> testFileUsage;
    Function<UploadedFile, ResponseEntity<UUID>> getOkResponseWithFileIdFunction = file -> result;

    when(fileService.find(fileId)).thenReturn(Optional.of(uploadedFile));
    when(xyzApplicationFileService.doesFileHaveUsage(uploadedFile)).thenReturn(true);

    assertThat(fileControllerHelperService.findFileAndThen(
        fileId,
        usageSupplier,
        userDetail,
        getOkResponseWithFileIdFunction
    )).isEqualTo(result);

    verify(xyzApplicationFileService).throwIfFileDoesNotBelongToUsage(uploadedFile, testFileUsage);
  }

  @Test
  void findFileAndThen_fileDoesNotExist() {
    var fileId = UUID.randomUUID();
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();
    Supplier<XyzApplicationFileUsage> usageSupplier = () -> testFileUsage;
    Function<UploadedFile, ResponseEntity<UUID>> getOkResponseWithFileIdFunction = mock(Function.class);

    when(fileService.find(fileId)).thenReturn(Optional.empty());

    assertThat(fileControllerHelperService.findFileAndThen(
        fileId,
        usageSupplier,
        userDetail,
        getOkResponseWithFileIdFunction
    ))
        .extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.NOT_FOUND);

    verify(getOkResponseWithFileIdFunction, never()).apply(any());
    verify(xyzApplicationFileService, never()).throwIfFileDoesNotBelongToUsage(any(), any());
  }

  @Test
  void findFileAndThen_fileExists_doesNotHaveUsage_doesBelongToUser() {
    var fileId = UUID.randomUUID();
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    var result = ResponseEntity.ok(fileId);
    Supplier<XyzApplicationFileUsage> usageSupplier = () -> testFileUsage;
    Function<UploadedFile, ResponseEntity<UUID>> getOkResponseWithFileIdFunction = file -> result;

    when(fileService.find(fileId)).thenReturn(Optional.of(uploadedFile));
    when(xyzApplicationFileService.doesFileHaveUsage(uploadedFile)).thenReturn(false);
    when(xyzApplicationFileService.fileBelongsToUser(uploadedFile, userDetail)).thenReturn(true);

    assertThat(fileControllerHelperService.findFileAndThen(
        fileId,
        usageSupplier,
        userDetail,
        getOkResponseWithFileIdFunction
    )).isEqualTo(result);

    verify(xyzApplicationFileService, never()).throwIfFileDoesNotBelongToUsage(any(), any());
  }

  @Test
  void findFileAndThen_fileExists_doesNotHaveUsage_doesNotBelongToUser() {
    var fileId = UUID.randomUUID();
    var userDetail = ServiceUserDetailTestUtil.newBuilder().build();
    var uploadedFile = new UploadedFile();
    Supplier<XyzApplicationFileUsage> usageSupplier = () -> testFileUsage;
    Function<UploadedFile, ResponseEntity<UUID>> getOkResponseWithFileIdFunction = mock(Function.class);

    when(fileService.find(fileId)).thenReturn(Optional.of(uploadedFile));
    when(xyzApplicationFileService.doesFileHaveUsage(uploadedFile)).thenReturn(false);
    when(xyzApplicationFileService.fileBelongsToUser(uploadedFile, userDetail)).thenReturn(false);

    assertThat(fileControllerHelperService.findFileAndThen(
        fileId,
        usageSupplier,
        userDetail,
        getOkResponseWithFileIdFunction
    ))
        .extracting(ResponseEntity::getStatusCode)
        .isEqualTo(HttpStatus.NOT_FOUND);

    verify(getOkResponseWithFileIdFunction, never()).apply(any());
    verify(xyzApplicationFileService, never()).throwIfFileDoesNotBelongToUsage(any(), any());
  }
}