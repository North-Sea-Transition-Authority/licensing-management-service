package uk.co.nstauthority.licensingmanagementservice.licence.overview.responsibleteam;

import static java.util.Map.entry;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Set;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.validation.ValidatorTestingUtil;

@ExtendWith(MockitoExtension.class)
class LicenceResponsibleTeamValidatorTest {

  @InjectMocks
  private LicenceResponsibleTeamValidator licenceResponsibleTeamValidator;

  @Test
  void isValid() {
    var form = new LicenceResponsibleTeamForm();
    form.setResponsibleTeam(LicenceTeam.CS_NEW_VENTURES);

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceResponsibleTeamValidator.isValid(form, bindingResult)).isTrue();
  }

  @Test
  void isValid_invalidForm_noResponsibleTeam() {
    var form = new LicenceResponsibleTeamForm();

    var bindingResult = ValidatorTestingUtil.getBindingResult(form);

    assertThat(licenceResponsibleTeamValidator.isValid(form, bindingResult)).isFalse();

    assertThat(ValidatorTestingUtil.extractErrors(bindingResult))
        .containsExactly(entry("responsibleTeam", Set.of("responsibleTeam.required")));
  }
}