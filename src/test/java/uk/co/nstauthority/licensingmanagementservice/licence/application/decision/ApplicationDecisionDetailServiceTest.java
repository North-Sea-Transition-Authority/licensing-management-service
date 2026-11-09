package uk.co.nstauthority.licensingmanagementservice.licence.application.decision;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetailTestUtil;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.application.ApplicationType;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplication;

@ExtendWith(MockitoExtension.class)
class ApplicationDecisionDetailServiceTest {

  private static final Instant NOW = Instant.parse("2026-11-05T10:15:30Z");
  private static final UUID APPLICATION_ID = UUID.randomUUID();

  @Mock
  private ApplicationDecisionDetailRepository applicationDecisionDetailRepository;

  @Captor
  private ArgumentCaptor<ApplicationDecisionDetail> decisionDetailCaptor;

  private ApplicationDecisionDetailService applicationDecisionDetailService;

  private ScheduleWorkProgrammeApplication application;

  @BeforeEach
  void setUp() {
    applicationDecisionDetailService = new ApplicationDecisionDetailService(
        applicationDecisionDetailRepository,
        Clock.fixed(NOW, ZoneId.of("UTC"))
    );

    application = new ScheduleWorkProgrammeApplication();
    application.setId(APPLICATION_ID);
  }

  @Test
  void findDraftDecision() {
    var draftDecision = new ApplicationDecisionDetail(UUID.randomUUID());
    when(applicationDecisionDetailRepository.findByApplicationTypeAndApplicationIdAndStatus(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        APPLICATION_ID,
        ApplicationDecisionStatus.DRAFT
    )).thenReturn(Optional.of(draftDecision));

    var result = applicationDecisionDetailService.findDraftDecision(application);

    assertThat(result).contains(draftDecision);
  }

  @Test
  void createDraftDecision_whenNoPreviousDecision_thenCreatesFirstVersionDraft() {
    var user = ServiceUserDetailTestUtil.newBuilder().withWuaId(10L).build();
    when(applicationDecisionDetailRepository.findFirstByApplicationTypeAndApplicationIdOrderByVersionNumberDesc(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        APPLICATION_ID
    )).thenReturn(Optional.empty());
    when(applicationDecisionDetailRepository.save(decisionDetailCaptor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    var result = applicationDecisionDetailService.createDraftDecision(application, user);

    var expected = new ApplicationDecisionDetail();
    expected.setApplicationType(ApplicationType.SCHEDULE_AMENDMENT_APPLICATION);
    expected.setApplicationId(APPLICATION_ID);
    expected.setVersionNumber(1);
    expected.setStatus(ApplicationDecisionStatus.DRAFT);
    expected.setCreatedDatetime(NOW);
    expected.setCreatedByWuaId(10L);

    assertThat(decisionDetailCaptor.getValue()).usingRecursiveComparison().isEqualTo(expected);
    assertThat(result).isSameAs(decisionDetailCaptor.getValue());
  }

  @Test
  void createDraftDecision_whenPreviousDecisionExists_thenCreatesNextVersionDraft() {
    var user = ServiceUserDetailTestUtil.newBuilder().withWuaId(10L).build();
    var previousDecision = new ApplicationDecisionDetail(UUID.randomUUID());
    previousDecision.setVersionNumber(2);
    when(applicationDecisionDetailRepository.findFirstByApplicationTypeAndApplicationIdOrderByVersionNumberDesc(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        APPLICATION_ID
    )).thenReturn(Optional.of(previousDecision));
    when(applicationDecisionDetailRepository.save(decisionDetailCaptor.capture()))
        .thenAnswer(invocation -> invocation.getArgument(0));

    applicationDecisionDetailService.createDraftDecision(application, user);

    assertThat(decisionDetailCaptor.getValue().getVersionNumber()).isEqualTo(3);
  }

  @Test
  void completeDraftDecision_whenDraftExists_thenSetsStatusToCompleted() {
    var draftDecision = new ApplicationDecisionDetail(UUID.randomUUID());
    draftDecision.setStatus(ApplicationDecisionStatus.DRAFT);
    when(applicationDecisionDetailRepository.findByApplicationTypeAndApplicationIdAndStatus(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        APPLICATION_ID,
        ApplicationDecisionStatus.DRAFT
    )).thenReturn(Optional.of(draftDecision));

    applicationDecisionDetailService.completeDraftDecision(application);

    verify(applicationDecisionDetailRepository).save(decisionDetailCaptor.capture());
    assertThat(decisionDetailCaptor.getValue().getStatus()).isEqualTo(ApplicationDecisionStatus.COMPLETED);
  }

  @Test
  void completeDraftDecision_whenNoDraftExists_thenThrows() {
    when(applicationDecisionDetailRepository.findByApplicationTypeAndApplicationIdAndStatus(
        ApplicationType.SCHEDULE_AMENDMENT_APPLICATION,
        APPLICATION_ID,
        ApplicationDecisionStatus.DRAFT
    )).thenReturn(Optional.empty());

    assertThatThrownBy(() -> applicationDecisionDetailService.completeDraftDecision(application))
        .isInstanceOf(LmsEntityNotFoundException.class);
    verify(applicationDecisionDetailRepository, never()).save(any(ApplicationDecisionDetail.class));
  }
}
