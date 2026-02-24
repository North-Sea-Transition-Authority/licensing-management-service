package uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CurrentDateMailMergeField.DESCRIPTION;
import static uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.mailmerge.mailmergefields.CurrentDateMailMergeField.MNEMONIC;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.digitaldocumentlibrary.document.DocumentMailMergeFieldResolveResult;
import uk.co.fivium.formlibrary.validator.date.DateUtils;
import uk.co.nstauthority.licensingmanagementservice.document.DocumentTemplateDtoTestUtil;
import uk.co.nstauthority.licensingmanagementservice.document.viewtemplates.DocumentInstanceDtoTestUtil;

@ExtendWith(MockitoExtension.class)
class CurrentDateMailMergeFieldTest {

  @Mock
  private Clock clock;

  @InjectMocks
  private CurrentDateMailMergeField mailMergeField;

  @Test
  void getMnemonic() {
    assertThat(mailMergeField.getMnemonic()).isEqualTo(MNEMONIC);
  }

  @Test
  void getDescription() {
    assertThat(mailMergeField.getDescription()).isEqualTo(DESCRIPTION);
  }

  @Test
  void isApplicable() {
    var documentTemplateDto = DocumentTemplateDtoTestUtil.newBuilder().build();
    assertThat(mailMergeField.isApplicable(documentTemplateDto)).isTrue();
  }

  @Test
  void resolve() {
    var currentDate = Instant.now();
    var zoneId = ZoneId.systemDefault();

    when(clock.instant()).thenReturn(currentDate);
    when(clock.getZone()).thenReturn(zoneId);
    var expectedMailMergeValue = DateUtils.format(LocalDate.now(clock));
    var documentInstanceDto = DocumentInstanceDtoTestUtil.newBuilder().build();

    assertThat(mailMergeField.resolve(documentInstanceDto)).isEqualTo(
        DocumentMailMergeFieldResolveResult.success(expectedMailMergeValue)
    );
  }
  }