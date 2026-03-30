package uk.co.nstauthority.licensingmanagementservice.document.signing;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

import java.io.IOException;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import uk.co.fivium.ftss.client.FtssVisualSignatureProperties;

class SignaturePlaceholderLocatorTest {

  private SignaturePlaceholderLocator locator;

  @BeforeEach
  void setUp() throws IOException {
    locator = new SignaturePlaceholderLocator();
  }

  @Test
  void getSignaturePlaceholderLocation_page1_top() throws IOException {
    var testDoc = getClassPathDocument("signing/signature-placeholder-page1-top.pdf");

    var expectedCoordinates = new FtssVisualSignatureProperties.SignatureCoordinates(
        new FtssVisualSignatureProperties.Coordinate(
            0,
            67,
            730
        ),
        new FtssVisualSignatureProperties.Coordinate(
            0,
            290,
            730
        ),
        new FtssVisualSignatureProperties.Coordinate(
            0,
            67,
            700
        ),
        new FtssVisualSignatureProperties.Coordinate(
            0,
            290,
            700
        )
    );

    var actualCoordinates = locator.getSignaturePlaceholderLocation(testDoc);

    assertThat(actualCoordinates).isEqualTo(expectedCoordinates);
  }

  @Test
  void getSignaturePlaceholderLocation_page1_bottom() throws IOException {
    var testDoc = getClassPathDocument("signing/signature-placeholder-page1-bottom.pdf");

    var expectedCoordinates = new FtssVisualSignatureProperties.SignatureCoordinates(
        new FtssVisualSignatureProperties.Coordinate(
            0,
            67,
            212
        ),
        new FtssVisualSignatureProperties.Coordinate(
            0,
            290,
            212
        ),
        new FtssVisualSignatureProperties.Coordinate(
            0,
            67,
            182
        ),
        new FtssVisualSignatureProperties.Coordinate(
            0,
            290,
            182
        )
    );

    var actualCoordinates = locator.getSignaturePlaceholderLocation(testDoc);

    assertThat(actualCoordinates).isEqualTo(expectedCoordinates);
  }

  @Test
  void getSignaturePlaceholderLocation_page2_top() throws IOException {
    var testDoc = getClassPathDocument("signing/signature-placeholder-page2-top.pdf");

    var expectedCoordinates = new FtssVisualSignatureProperties.SignatureCoordinates(
        new FtssVisualSignatureProperties.Coordinate(
            1,
            67,
            707
        ),
        new FtssVisualSignatureProperties.Coordinate(
            1,
            290,
            707
        ),
        new FtssVisualSignatureProperties.Coordinate(
            1,
            67,
            677
        ),
        new FtssVisualSignatureProperties.Coordinate(
            1,
            290,
            677
        )
    );

    var actualCoordinates = locator.getSignaturePlaceholderLocation(testDoc);

    assertThat(actualCoordinates).isEqualTo(expectedCoordinates);
  }

  @Test
  void getSignaturePlaceholderLocation_no_placeholder() throws IOException {
    var testDoc = getClassPathDocument("signing/signature-placeholder-missing.pdf");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> locator.getSignaturePlaceholderLocation(testDoc));
  }

  @Test
  void getSignaturePlaceholderLocation_multiple_placeholders() throws IOException {
    var testDoc = getClassPathDocument("signing/signature-placeholder-multiple.pdf");
    assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> locator.getSignaturePlaceholderLocation(testDoc));
  }

  private PDDocument getClassPathDocument(String path) throws IOException {
    return PDDocument.load(new ClassPathResource(path).getInputStream());
  }
}