package uk.co.nstauthority.licensingmanagementservice.document;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class DocumentTemplateMailMergeFieldFormatterTest {

  @InjectMocks
  private DocumentTemplateMailMergeFieldFormatter formatter;

  @Test
  void formatSuccess() {
    assertThat(formatter.formatSuccess("value")).isEqualTo("value");
  }

  @Test
  void formatError() {
    assertThat(formatter.formatError("value")).isEqualTo("value");
  }

  @Test
  void formatFootnotes() {
    assertThat(formatter.formatFootnotes("value"))
        .isEqualTo("<span class=\"govuk-tag--yellow\">value</span>");
  }
}