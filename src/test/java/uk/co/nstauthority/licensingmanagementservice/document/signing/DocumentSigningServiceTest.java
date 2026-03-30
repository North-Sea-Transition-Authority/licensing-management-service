package uk.co.nstauthority.licensingmanagementservice.document.signing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.ClassPathResource;
import uk.co.fivium.ftss.client.FtssClient;
import uk.co.fivium.ftss.client.FtssSignerProperties;
import uk.co.fivium.ftss.client.FtssVisualSignatureProperties;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;

@ExtendWith(MockitoExtension.class)
class DocumentSigningServiceTest {

  @Mock
  private FtssClient ftssClient;

  @Captor
  private ArgumentCaptor<ByteArrayInputStream> inputStreamCaptor;

  private final DigitalSignatureProperties digitalSignatureProperties = new DigitalSignatureProperties(
      "Lms",
      "Fivium Ltd",
      "tech@fivium.co.uk",
      "London",
      "GB",
      "London",
      "test signing",
      "unit tests",
      "test line 1",
      "test line 3"
  );

  private final FtssSignerProperties expectedSignerProperties = new FtssSignerProperties(
      "Lms",
      "Fivium Ltd",
      "tech@fivium.co.uk",
      "London",
      "GB",
      "London",
      "test signing",
      "unit tests");

  private final ServiceUserDetail signingUser = ServiceUserDetailTestUtil
      .newBuilder()
      .withForename("Test")
      .withSurname("User")
      .build();

  private ByteArrayResource document;

  private final ByteArrayResource signedPdf = new ByteArrayResource(new byte[] {1, 2, 3});

  private DocumentSigningService documentSigningService;

  @BeforeEach
  void setUp() {
    documentSigningService = new DocumentSigningService(ftssClient, digitalSignatureProperties);
  }

  @Test
  void previewPdfSignature() throws IOException {
    var expectedVisualSignatureProperties = new FtssVisualSignatureProperties(
        new ClassPathResource("document-assets/blank-1px.png"),
        new FtssVisualSignatureProperties.SignatureCoordinates(
            new FtssVisualSignatureProperties.Coordinate(0, 67, 730),
            new FtssVisualSignatureProperties.Coordinate(0, 290, 730),
            new FtssVisualSignatureProperties.Coordinate(0,67,700),
            new FtssVisualSignatureProperties.Coordinate(0, 290, 700)
        ),
        "test line 1",
        "((SIGNATORY_NAME))",
        "test line 3"
    );

    document = new ByteArrayResource(new ClassPathResource("signing/signature-placeholder-page1-top.pdf").getContentAsByteArray());

    when(ftssClient.previewPdf(inputStreamCaptor.capture(), eq(expectedSignerProperties), eq(expectedVisualSignatureProperties)))
        .thenReturn(signedPdf.getInputStream());

    var result = documentSigningService.previewPdfSignature(document);

    assertThat(result.getContentAsByteArray()).isEqualTo(signedPdf.getContentAsByteArray());
    assertThat(inputStreamCaptor.getValue().readAllBytes()).isEqualTo(document.getContentAsByteArray());
  }

  @Test
  void signPdf() throws IOException {
    var expectedSignerProps = new FtssSignerProperties(
        "Lms",
        "Fivium Ltd",
        "tech@fivium.co.uk",
        "London",
        "GB",
        "London",
        "test signing",
        "unit tests");

    var expectedVisualSignatureProperties = new FtssVisualSignatureProperties(
        new ClassPathResource("document-assets/blank-1px.png"),
        new FtssVisualSignatureProperties.SignatureCoordinates(
            new FtssVisualSignatureProperties.Coordinate(0, 67, 730),
            new FtssVisualSignatureProperties.Coordinate(0, 290, 730),
            new FtssVisualSignatureProperties.Coordinate(0,67,700),
            new FtssVisualSignatureProperties.Coordinate(0, 290, 700)
        ),
        "test line 1",
        "Test User",
        "test line 3"
    );

    document = new ByteArrayResource(new ClassPathResource("signing/signature-placeholder-page1-top.pdf").getContentAsByteArray());

    when(ftssClient.signPdf(inputStreamCaptor.capture(), eq(expectedSignerProps), eq(expectedVisualSignatureProperties)))
        .thenReturn(signedPdf.getInputStream());

    var result = documentSigningService.signPdf(document, signingUser);

    assertThat(result.getContentAsByteArray()).isEqualTo(signedPdf.getContentAsByteArray());
    assertThat(inputStreamCaptor.getValue().readAllBytes()).isEqualTo(document.getContentAsByteArray());
  }
}
