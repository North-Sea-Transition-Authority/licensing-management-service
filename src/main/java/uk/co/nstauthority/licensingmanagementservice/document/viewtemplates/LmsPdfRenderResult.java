package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates;

import java.util.Map;
import org.springframework.core.io.ByteArrayResource;

public record LmsPdfRenderResult(
    ByteArrayResource pdfContent,
    String pdfHtml,
    Map<String, String> mailMergeResolvedValuesByMnemonic
) {
}