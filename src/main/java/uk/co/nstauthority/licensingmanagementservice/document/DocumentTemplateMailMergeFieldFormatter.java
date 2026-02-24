package uk.co.nstauthority.licensingmanagementservice.document;

import org.springframework.stereotype.Component;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldFormatter;

@Component
public class DocumentTemplateMailMergeFieldFormatter implements DocumentMailMergeFieldFormatter {

  @Override
  public String formatSuccess(String value) {
    return value;
  }

  @Override
  public String formatError(String value) {
    return value;
  }

  @Override
  public String formatFootnotes(String value) {
    return "<span class=\"govuk-tag--yellow\">%s</span>".formatted(value);
  }
}