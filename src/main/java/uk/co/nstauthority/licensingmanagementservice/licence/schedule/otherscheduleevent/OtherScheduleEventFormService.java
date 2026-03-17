package uk.co.nstauthority.licensingmanagementservice.licence.schedule.otherscheduleevent;

import jakarta.transaction.Transactional;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.stereotype.Service;
import uk.co.nstauthority.licensingmanagementservice.licence.rules.LicenceTypeRulesResolver;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.LicenceScheduleEventStatus;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.calculation.LicenceScheduleCalculationService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.common.LicenceScheduleRelativeOptionsService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduledetail.LicenceScheduleDetail;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licenceschedulephase.LicenceSchedulePhaseService;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTerm;
import uk.co.nstauthority.licensingmanagementservice.licence.schedule.licencescheduleterm.LicenceScheduleTermService;
import uk.co.nstauthority.licensingmanagementservice.util.StreamUtil;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Service
public class OtherScheduleEventFormService {

  private final OtherScheduleEventRepository otherScheduleEventRepository;
  private final LicenceScheduleTermService licenceScheduleTermService;
  private final LicenceSchedulePhaseService licenceSchedulePhaseService;
  private final LicenceTypeRulesResolver licenceTypeRulesResolver;
  private final LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService;
  private final LicenceScheduleCalculationService licenceScheduleCalculationService;

  public OtherScheduleEventFormService(
      OtherScheduleEventRepository otherScheduleEventRepository,
      LicenceScheduleTermService licenceScheduleTermService,
      LicenceSchedulePhaseService licenceSchedulePhaseService,
      LicenceTypeRulesResolver licenceTypeRulesResolver,
      LicenceScheduleRelativeOptionsService licenceScheduleRelativeOptionsService,
      LicenceScheduleCalculationService licenceScheduleCalculationService
  ) {
    this.otherScheduleEventRepository = otherScheduleEventRepository;
    this.licenceScheduleTermService = licenceScheduleTermService;
    this.licenceSchedulePhaseService = licenceSchedulePhaseService;
    this.licenceTypeRulesResolver = licenceTypeRulesResolver;
    this.licenceScheduleRelativeOptionsService = licenceScheduleRelativeOptionsService;
    this.licenceScheduleCalculationService = licenceScheduleCalculationService;
  }

  public Map<String, String> getDateOptions(LicenceScheduleDetail licenceScheduleDetail) {
    var options = new ArrayList<>(Arrays.asList(OtherScheduleEventDateOption.values()));

    var licenceType = licenceScheduleDetail.getLicenceSchedule().getLicence().getType();

    if (!licenceTypeRulesResolver.arePhasesCaptured(licenceType)
        || licenceScheduleRelativeOptionsService.getSchedulePhaseOptions(licenceScheduleDetail).isEmpty()) {
      options.remove(OtherScheduleEventDateOption.WITHIN_A_PHASE);
    }

    return DisplayableEnumOptionUtil.getDisplayableOptions(options);
  }

  @Transactional
  public void saveEventFromForm(
      OtherScheduleEventForm form,
      LicenceScheduleDetail licenceScheduleDetail,
      OtherScheduleEvent event
  ) {
    event.setLicenceScheduleDetail(licenceScheduleDetail);
    event.setCategory(form.getOtherScheduleEventCategory());
    event.setOtherCategoryName(form.getOtherCategoryName());
    event.setDescription(form.getDescription());
    event.setStatus(LicenceScheduleEventStatus.ACTIVE);

    var dateOption = form.getOtherScheduleEventDateOption();

    event.setDateOption(dateOption);

    if (dateOption.equals(OtherScheduleEventDateOption.WITHIN_A_TERM)) {
      event.setLicenceScheduleTerm(
          licenceScheduleTermService.getTermByIdOrThrow(UUID.fromString(form.getLicenceScheduleTermId()))
      );
    } else {
      event.setLicenceScheduleTerm(null);
    }

    if (dateOption.equals(OtherScheduleEventDateOption.WITHIN_A_PHASE)) {
      event.setLicenceSchedulePhase(
          licenceSchedulePhaseService.getPhaseByIdOrThrow(UUID.fromString(form.getLicenceSchedulePhaseId()))
      );
    } else {
      event.setLicenceSchedulePhase(null);
    }

    if (dateOption.equals(OtherScheduleEventDateOption.RELATIVE_DATE)) {
      setRelativeEvent(form, licenceScheduleDetail, event);
      event.setRelativeDuration(form.getRelativeDuration().toThreeFieldDuration());
    } else {
      event.setRelativeDuration(null);
    }

    event.setComments(form.getComments());

    if (event.getEventReference() == null) {
      event.setEventReference(UUID.randomUUID());
    }
    
    otherScheduleEventRepository.save(event);
    licenceScheduleCalculationService.calculateAndSaveLicenceScheduleDates(licenceScheduleDetail);
  }

  public OtherScheduleEventForm getEventForm(OtherScheduleEvent otherScheduleEvent) {
    var form = new OtherScheduleEventForm();
    form.setOtherScheduleEventCategory(otherScheduleEvent.getCategory());
    form.setOtherCategoryName(otherScheduleEvent.getOtherCategoryName());
    form.setDescription(otherScheduleEvent.getDescription());
    form.setComments(otherScheduleEvent.getComments());

    var dateOption = otherScheduleEvent.getDateOption();

    form.setOtherScheduleEventDateOption(dateOption);

    var termIdString = otherScheduleEvent.getLicenceScheduleTerm() != null
        ? String.valueOf(otherScheduleEvent.getLicenceScheduleTerm().getId())
        : null;

    var phaseIdString = otherScheduleEvent.getLicenceSchedulePhase() != null
        ? String.valueOf(otherScheduleEvent.getLicenceSchedulePhase().getId())
        : null;

    if (dateOption.equals(OtherScheduleEventDateOption.WITHIN_A_TERM)) {
      form.setLicenceScheduleTermId(termIdString);
    }

    if (dateOption.equals(OtherScheduleEventDateOption.WITHIN_A_PHASE)) {
      form.setLicenceSchedulePhaseId(phaseIdString);
    }

    if (dateOption.equals(OtherScheduleEventDateOption.RELATIVE_DATE)) {
      form.getRelativeDuration().setFromThreeFieldDuration(otherScheduleEvent.getRelativeDuration());

      var relativeIdString = termIdString != null
          ? termIdString
          : phaseIdString;

      form.setRelativeEventId(relativeIdString);
    }

    return form;
  }

  private void setRelativeEvent(
      OtherScheduleEventForm form,
      LicenceScheduleDetail licenceScheduleDetail,
      OtherScheduleEvent event
  ) {
    var eventId = UUID.fromString(form.getRelativeEventId());

    var termMap = licenceScheduleTermService.getActiveTermsByLicenceScheduleDetail(licenceScheduleDetail).stream()
        .collect(StreamUtil.toLinkedHashMap(LicenceScheduleTerm::getId, Function.identity()));

    if (termMap.containsKey(eventId)) {
      event.setLicenceScheduleTerm(termMap.get(eventId));
      event.setLicenceSchedulePhase(null);
    } else {
      event.setLicenceScheduleTerm(null);
      event.setLicenceSchedulePhase(licenceSchedulePhaseService.getPhaseByIdOrThrow(eventId));
    }
  }
}
