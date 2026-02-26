package uk.co.nstauthority.licensingmanagementservice.document;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class AddSectionOptionConverterTest {

  @InjectMocks
  private AddSectionOptionConverter converter;

  @ParameterizedTest
  @EnumSource(AddSectionOption.class)
  void convert_whenTabAnchorExists_thenReturnTabEnum(AddSectionOption option) {
    assertThat(converter.convert(option.name())).isEqualTo(option);
  }

  @ParameterizedTest
  @NullAndEmptySource
  @ValueSource(strings = {"not a tab"})
  void convert_whenTabAnchorDoesNotExists_thenThrowError(String option) {
    assertThatThrownBy(() -> converter.convert(option))
        .isInstanceOf(ResponseStatusException.class)
        .hasMessageContaining("Missing required request parameter 'section'");
  }
}
