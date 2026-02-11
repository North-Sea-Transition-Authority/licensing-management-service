package uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.requestpurpose;

import jakarta.transaction.Transactional;
import java.util.EnumSet;
import java.util.Set;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.ScheduleWorkProgrammeApplicationService;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.extendjourney.LicenceScheduleExtensionRepository;
import uk.co.nstauthority.licensingmanagementservice.licence.scheduleworkprogrammeapplication.overallrequest.LicenceScheduleSupportingInformationService;

@Service
public class SwpApplicationRequestPurposeService {

  private final LicenceTypeRulesResolver licenceTypeRulesResolver;
  private final SwpApplicationRequestPurposeRepository swpApplicationRequestPurposeRepository;
  private final LicenceScheduleExtensionRepository licenceScheduleExtensionRepository;
  private final LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService;
  private final ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService;

  public SwpApplicationRequestPurposeService(
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      SwpApplicationRequestPurposeRepository swpApplicationRequestPurposeRepository,
      LicenceScheduleExtensionRepository licenceScheduleExtensionRepository,
      LicenceScheduleSupportingInformationService licenceScheduleSupportingInformationService,
      ScheduleWorkProgrammeApplicationService scheduleWorkProgrammeApplicationService) {
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
    this.swpApplicationRequestPurposeRepository = swpApplicationRequestPurposeRepository;
    this.licenceScheduleExtensionRepository = licenceScheduleExtensionRepository;
    this.licenceScheduleSupportingInformationService = licenceScheduleSupportingInformationService;
    this.scheduleWorkProgrammeApplicationService = scheduleWorkProgrammeApplicationService;
  }

  public Set<SwpApplicationRequestPurposeOption> getPageOptions(
      ScheduleWorkProgrammeApplicationDetail applicationDetail) {

    var licence = scheduleWorkProgrammeApplicationService.getLicenceFromScheduleWorkProgrammeApplicationDetail(applicationDetail);
    var licenceType = licence.getType();

    var hasTerms = licenceTypeRulesResolver.hasTerms(licenceType);
    var phasesCaptured = licenceTypeRulesResolver.arePhasesCaptured(licenceType);
    var hasWorkProgramme = licenceTypeRulesResolver.hasWorkProgramme(licenceType);

    var options = EnumSet.noneOf(SwpApplicationRequestPurposeOption.class);

    if (hasTerms && phasesCaptured) {
      options.add(SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM);
    } else if (hasTerms) {
      options.add(SwpApplicationRequestPurposeOption.EXTEND_A_TERM);
    }

    if (hasWorkProgramme) {
      options.add(SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME);
    }

    return options;
  }

  @Transactional
  public SwpApplicationRequestPurpose saveOrUpdateRequestPurpose(
      ScheduleWorkProgrammeApplicationDetail applicationDetail,
      SwpApplicationRequestPurposeForm form) {

    var swpApplicationRequestPurpose = swpApplicationRequestPurposeRepository
        .getByScheduleWorkProgrammeApplicationDetail(applicationDetail)
        .orElseGet(() -> {
          var purpose = new SwpApplicationRequestPurpose();
          purpose.setScheduleWorkProgrammeApplicationDetail(applicationDetail);
          return purpose;
        });

    var requestPurposes = form.getRequestPurposes();

    boolean extendOptionSelected =
        requestPurposes.contains(SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM)
        || requestPurposes.contains(SwpApplicationRequestPurposeOption.EXTEND_A_TERM);

    if (!extendOptionSelected) {
      licenceScheduleExtensionRepository.deleteByScheduleWorkProgrammeApplicationDetails(applicationDetail);

      licenceScheduleSupportingInformationService.handleSupportingInformationExtensionRemoval(applicationDetail);
    }

    setRequestPurposes(swpApplicationRequestPurpose, requestPurposes);

    swpApplicationRequestPurposeRepository.save(swpApplicationRequestPurpose);

    return swpApplicationRequestPurpose;
  }

  void setRequestPurposes(SwpApplicationRequestPurpose swpApplicationRequestPurpose,
                          Set<SwpApplicationRequestPurposeOption> requestPurposes) {
    swpApplicationRequestPurpose.setExtendPhaseOrTerm(false);
    swpApplicationRequestPurpose.setExtendTerm(false);
    swpApplicationRequestPurpose.setAmendWorkProgramme(false);

    for (SwpApplicationRequestPurposeOption requestPurposeOption : requestPurposes) {
      switch (requestPurposeOption) {
        case EXTEND_A_PHASE_OR_TERM -> swpApplicationRequestPurpose.setExtendPhaseOrTerm(true);
        case EXTEND_A_TERM -> swpApplicationRequestPurpose.setExtendTerm(true);
        case AMEND_THE_WORK_PROGRAMME -> swpApplicationRequestPurpose.setAmendWorkProgramme(true);
        default -> throw new IllegalStateException("Unexpected value: " + requestPurposeOption);
      }
    }
  }

  public SwpApplicationRequestPurposeForm getFilledSwpApplicationRequestPurposeForm(
      ScheduleWorkProgrammeApplicationDetail scheduleWorkProgrammeApplicationDetail) {
    var form = new SwpApplicationRequestPurposeForm();

    return swpApplicationRequestPurposeRepository
        .getByScheduleWorkProgrammeApplicationDetail(scheduleWorkProgrammeApplicationDetail)
        .map(swpApplicationRequestPurpose -> {
          form.setRequestPurposes(getPersistedRequestPurposeOptions(swpApplicationRequestPurpose));
          return form;
        })
        .orElse(form);
  }

  EnumSet<SwpApplicationRequestPurposeOption> getPersistedRequestPurposeOptions(
      SwpApplicationRequestPurpose swpApplicationRequestPurpose) {

    var requestPurposes = EnumSet.noneOf(SwpApplicationRequestPurposeOption.class);
    if (swpApplicationRequestPurpose.getExtendPhaseOrTerm()) {
      requestPurposes.add(SwpApplicationRequestPurposeOption.EXTEND_A_PHASE_OR_TERM);
    }
    if (swpApplicationRequestPurpose.getExtendTerm()) {
      requestPurposes.add(SwpApplicationRequestPurposeOption.EXTEND_A_TERM);
    }
    if (swpApplicationRequestPurpose.getAmendWorkProgramme()) {
      requestPurposes.add(SwpApplicationRequestPurposeOption.AMEND_THE_WORK_PROGRAMME);
    }
    return requestPurposes;
  }
}