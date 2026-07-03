package uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulerate;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.authentication.ServiceUserDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.eventcomments.EventCommentService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Service
public class LicenceScheduleRateFormService {

  private final LicenceScheduleRateRepository licenceScheduleRateRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;
  private final LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;
  private final EventCommentService eventCommentService;

  public LicenceScheduleRateFormService(
      LicenceScheduleRateRepository licenceScheduleRateRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService,
      LicenceScheduleCalculationService licenceScheduleCalculationService,
      EventCommentService eventCommentService
  ) {
    this.licenceScheduleRateRepository = licenceScheduleRateRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
    this.licenceScheduleRelativeOptionsService = licenceScheduleRelativeOptionsService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
    this.eventCommentService = eventCommentService;
  }

  @Transactional
  void saveRateFromForm(
      LicenceScheduleRateForm form,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleRate licenceScheduleRate,
      ServiceUserDetail serviceUserDetail
  ) {
    licenceScheduleRate.setLicenceScheduleDetail(licenceScheduleDetail);
    licenceScheduleRate.setRateDefinitionOption(form.getRateDefinitionOption());

    if (form.getRateDefinitionOption().equals(RateDefinitionOption.TERM)) {
      licenceScheduleRate.setLicenceScheduleTerm(
          licenceScheduleTermService.getTermByIdOrThrow(UUID.fromString(form.getLicenceScheduleTermId()))
      );
      licenceScheduleRate.setStartDate(null);
    } else {
      licenceScheduleRate.setLicenceScheduleTerm(null);
    }

    if (form.getRateDefinitionOption().equals(RateDefinitionOption.PHASE)) {
      licenceScheduleRate.setLicenceSchedulePhase(
          licenceSchedulePhaseService.getPhaseByIdOrThrow(UUID.fromString(form.getLicenceSchedulePhaseId()))
      );
      licenceScheduleRate.setStartDate(null);
    } else {
      licenceScheduleRate.setLicenceSchedulePhase(null);
    }

    if (form.getRateDefinitionOption().equals(RateDefinitionOption.CUSTOM_PERIOD)) {
      setRelativeEvent(form, licenceScheduleDetail, licenceScheduleRate);
      licenceScheduleRate.setRateRelativeDateOption(form.getRateRelativeDateOption());
      if (form.getRateRelativeDateOption().equals(RateRelativeDateOption.RELATIVE_TO_START_DATE)) {
        licenceScheduleRate.setRelativeDuration(form.getRelativeDuration().toThreeFieldDuration());
      }
    } else {
      licenceScheduleRate.setRateRelativeDateOption(null);
      licenceScheduleRate.setRelativeDuration(null);
    }

    licenceScheduleRate.setRentalRate(form.getRentalRate().getAsBigDecimal().orElse(null));

    if (licenceScheduleRate.getLicenceSchedule() == null) {
      licenceScheduleRate.setLicenceSchedule(licenceScheduleDetail.getLicenceSchedule());
    }

    licenceScheduleRateRepository.save(licenceScheduleRate);
    eventCommentService.addOrUpdatePendingComment(form.getComments(), licenceScheduleRate, serviceUserDetail);
    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  public LicenceScheduleRateForm getFormFromRate(LicenceScheduleRate licenceScheduleRate) {
    LicenceScheduleRateForm form = new LicenceScheduleRateForm();
    form.getRentalRate().setInputValue(licenceScheduleRate.getRentalRate().toString());
    if (licenceScheduleRate.getLicenceSchedule() != null) {
      eventCommentService.findPendingCommentForScheduleEvent(licenceScheduleRate)
          .ifPresent(comment -> form.setComments(comment.getComment()));
    }

    var definitionOption = licenceScheduleRate.getRateDefinitionOption();
    form.setRateDefinitionOption(definitionOption);

    var termIdString = licenceScheduleRate.getLicenceScheduleTerm() != null
        ? String.valueOf(licenceScheduleRate.getLicenceScheduleTerm().getId())
        : null;

    var phaseIdString = licenceScheduleRate.getLicenceSchedulePhase() != null
        ? String.valueOf(licenceScheduleRate.getLicenceSchedulePhase().getId())
        : null;

    if (definitionOption.equals(RateDefinitionOption.TERM)) {
      form.setLicenceScheduleTermId(termIdString);
    }

    if (definitionOption.equals(RateDefinitionOption.PHASE)) {
      form.setLicenceSchedulePhaseId(phaseIdString);
    }

    if (definitionOption.equals(RateDefinitionOption.CUSTOM_PERIOD)) {
      form.setRateRelativeDateOption(licenceScheduleRate.getRateRelativeDateOption());

      var relativeIdString = termIdString != null
          ? termIdString
          : phaseIdString;

      form.setRelativeEventId(relativeIdString);

      if (licenceScheduleRate.getRateRelativeDateOption().equals(RateRelativeDateOption.RELATIVE_TO_START_DATE)) {
        form.getRelativeDuration().setFromThreeFieldDuration(licenceScheduleRate.getRelativeDuration());
      }
    }

    return form;
  }

  private void setRelativeEvent(
      LicenceScheduleRateForm form,
      LicenceScheduleDetail licenceScheduleDetail,
      LicenceScheduleRate rate
  ) {
    var eventId = UUID.fromString(form.getRelativeEventId());

    var termMap = licenceScheduleTermService.getTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceScheduleTerm::getId, Function.identity()));

    if (termMap.containsKey(eventId)) {
      rate.setLicenceScheduleTerm(termMap.get(eventId));
      rate.setLicenceSchedulePhase(null);
    } else {
      rate.setLicenceScheduleTerm(null);
      rate.setLicenceSchedulePhase(licenceSchedulePhaseService.getPhaseByIdOrThrow(eventId));
    }
  }

  public Map<String, String> getRateDefinitionOptions(LicenceScheduleDetail licenceScheduleDetail) {
    var options = new ArrayList<>(Arrays.asList(RateDefinitionOption.values()));

    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    if (!licenceTypeRulesResolver.arePhasesCaptured(licenceType)
        || licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail).isEmpty()) {
      options.remove(RateDefinitionOption.PHASE);
    }

    return DisplayableEnumOptionUtil.getDisplayableOptions(options);
  }

}
