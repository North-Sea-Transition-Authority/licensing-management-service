package uk.co.nstauthority.licensingmanagementservice.licence.correction.position;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.HandlerMapping;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.LicenceCorrectionTestUtil;
import uk.co.nstauthority.licensingmanagementservice.licence.position.LicencePositionTestUtil;

@ExtendWith(MockitoExtension.class)
class CorrectionPositionArgumentResolverTest {

  private static final UUID CORRECTION_ID = UUID.randomUUID();
  private static final UUID POSITION_ID = UUID.randomUUID();

  @Mock
  private LicenceCorrectionService licenceCorrectionService;

  @Mock
  private CorrectionPositionService correctionPositionService;

  @InjectMocks
  private CorrectionPositionArgumentResolver correctionPositionArgumentResolver;

  @Test
  void resolveArgument_whenPositionOnCorrection_thenCorrectionPosition() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).build();
    var correctionPosition = new CorrectionPosition.Executed(
        correction,
        LicencePositionTestUtil.newBuilder().withId(POSITION_ID).build()
    );

    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(correctionPositionService.findCorrectionPosition(correction, POSITION_ID))
        .thenReturn(Optional.of(correctionPosition));

    var result = correctionPositionArgumentResolver.resolveArgument(
        null, null, request(CORRECTION_ID.toString(), POSITION_ID.toString()), null);

    assertThat(result).isEqualTo(correctionPosition);
  }

  @Test
  void resolveArgument_whenPositionNotOnCorrection_thenNotFound() {
    var correction = LicenceCorrectionTestUtil.newBuilder().withId(CORRECTION_ID).build();

    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.of(correction));
    when(correctionPositionService.findCorrectionPosition(correction, POSITION_ID)).thenReturn(Optional.empty());

    var request = request(CORRECTION_ID.toString(), POSITION_ID.toString());

    assertThatThrownBy(() -> correctionPositionArgumentResolver.resolveArgument(null, null, request, null))
        .isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void resolveArgument_whenCorrectionNotFound_thenNotFound() {
    when(licenceCorrectionService.findById(CORRECTION_ID)).thenReturn(Optional.empty());

    var request = request(CORRECTION_ID.toString(), POSITION_ID.toString());

    assertThatThrownBy(() -> correctionPositionArgumentResolver.resolveArgument(null, null, request, null))
        .isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
  }

  @Test
  void resolveArgument_whenIdIsNotAUuid_thenBadRequest() {
    var request = request(CORRECTION_ID.toString(), "not-a-uuid");

    assertThatThrownBy(() -> correctionPositionArgumentResolver.resolveArgument(null, null, request, null))
        .isInstanceOfSatisfying(ResponseStatusException.class,
            exception -> assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
  }

  private static ServletWebRequest request(String correctionId, String licencePositionId) {
    var request = new MockHttpServletRequest();
    request.setAttribute(HandlerMapping.URI_TEMPLATE_VARIABLES_ATTRIBUTE, Map.of(
        CorrectionPositionArgumentResolver.CORRECTION_ID, correctionId,
        CorrectionPositionArgumentResolver.LICENCE_POSITION_ID, licencePositionId
    ));
    return new ServletWebRequest(request);
  }
}
