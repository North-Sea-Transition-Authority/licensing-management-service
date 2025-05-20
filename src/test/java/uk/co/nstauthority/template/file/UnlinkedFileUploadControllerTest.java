package uk.co.nstauthority.template.file;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;
import static uk.co.nstauthority.template.authentication.TestUserProvider.user;
import static uk.co.nstauthority.template.file.FileUploadTestUtil.CONTENT_TYPE;
import static uk.co.nstauthority.template.file.FileUploadTestUtil.FILE_NAME_1;
import static uk.co.nstauthority.template.util.RedirectedToLoginUrlMatcher.redirectionToLoginUrl;

import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import uk.co.nstauthority.template.AbstractControllerTest;
import uk.co.nstauthority.template.mvc.ReverseRouter;
import uk.co.nstauthority.template.util.SecurityTest;

@ContextConfiguration(classes = UnlinkedFileUploadController.class)
class UnlinkedFileUploadControllerTest extends AbstractControllerTest {

  private static final Class<UnlinkedFileUploadController> CONTROLLER = UnlinkedFileUploadController.class;

  @MockitoBean
  private FileControllerHelperService fileControllerHelperService;

  @SecurityTest
  void upload_whenNotLoggedIn_thenRedirectToLoginUrl() throws Exception{
    mockMvc.perform(multipart(ReverseRouter.route(on(CONTROLLER)
            .upload(null, null)))
            .with(csrf()))
        .andExpect(redirectionToLoginUrl());
  }

  @Test
  void upload() throws Exception {
    var file = new MockMultipartFile("file", FILE_NAME_1, CONTENT_TYPE, new byte[]{1,2, 3, 4, 5});

    when(fileControllerHelperService.upload(file, regulatorUser)).thenReturn(ResponseEntity.ok().build());

    mockMvc.perform(multipart(ReverseRouter.route(on(CONTROLLER)
            .upload(null, null)))
            .file(file)
            .with(csrf())
            .with(user(regulatorUser)))
        .andExpect(status().isOk());
  }
}
