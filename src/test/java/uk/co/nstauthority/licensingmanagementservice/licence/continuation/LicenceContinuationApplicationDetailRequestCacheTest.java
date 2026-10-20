package uk.co.nstauthority.licensingmanagementservice.licence.continuation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LicenceContinuationApplicationDetailRequestCacheTest {

  @Mock
  private LicenceContinuationApplicationDetailRepository licenceContinuationApplicationDetailRepository;

  @InjectMocks
  private LicenceContinuationApplicationDetailRequestCache licenceContinuationApplicationDetailRequestCache;

  @Test
  void findById() {
    var detail = new LicenceContinuationApplicationDetail(UUID.randomUUID());
    when(licenceContinuationApplicationDetailRepository.findById(detail.getId())).thenReturn(Optional.of(detail));

    var result = licenceContinuationApplicationDetailRequestCache.findById(detail.getId());

    assertThat(result).containsSame(detail);
  }
}
