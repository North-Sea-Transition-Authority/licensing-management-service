package uk.co.nstauthority.licensingmanagementservice.licence.application.decision;

import java.time.Clock;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.exception.LmsEntityNotFoundException;
import uk.co.nstauthority.licensingmanagementservice.licence.LicenceApplication;

@Service
public class ApplicationDecisionDetailService {

  private final ApplicationDecisionDetailRepository applicationDecisionDetailRepository;
  private final Clock clock;

  public ApplicationDecisionDetailService(
      ApplicationDecisionDetailRepository applicationDecisionDetailRepository,
      Clock clock
  ) {
    this.applicationDecisionDetailRepository = applicationDecisionDetailRepository;
    this.clock = clock;
  }

  public Optional<ApplicationDecisionDetail> findDraftDecision(LicenceApplication application) {
    return applicationDecisionDetailRepository.findByApplicationTypeAndApplicationIdAndStatus(
        application.getApplicationType(),
        application.getId(),
        ApplicationDecisionStatus.DRAFT
    );
  }

  @Transactional
  public ApplicationDecisionDetail createDraftDecision(LicenceApplication application, ServiceUserDetail user) {
    var nextVersionNumber = applicationDecisionDetailRepository
        .findFirstByApplicationTypeAndApplicationIdOrderByVersionNumberDesc(
            application.getApplicationType(),
            application.getId()
        )
        .map(latestDecisionDetail -> latestDecisionDetail.getVersionNumber() + 1)
        .orElse(1);

    var decisionDetail = new ApplicationDecisionDetail();
    decisionDetail.setApplicationType(application.getApplicationType());
    decisionDetail.setApplicationId(application.getId());
    decisionDetail.setVersionNumber(nextVersionNumber);
    decisionDetail.setStatus(ApplicationDecisionStatus.DRAFT);
    decisionDetail.setCreatedDatetime(Instant.now(clock));
    decisionDetail.setCreatedByWuaId(user.wuaId());
    return applicationDecisionDetailRepository.save(decisionDetail);
  }

  @Transactional
  public void completeDraftDecision(LicenceApplication application) {
    var decisionDetail = findDraftDecision(application)
        .orElseThrow(() -> new LmsEntityNotFoundException(
            "Draft application decision for %s application %s".formatted(
                application.getApplicationType(),
                application.getId()
            )
        ));
    decisionDetail.setStatus(ApplicationDecisionStatus.COMPLETED);
    applicationDecisionDetailRepository.save(decisionDetail);
  }
}
