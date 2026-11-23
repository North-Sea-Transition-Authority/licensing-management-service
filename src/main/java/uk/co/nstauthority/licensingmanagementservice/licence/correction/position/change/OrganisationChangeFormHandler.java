package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Optional;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitQueryService;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormHandler;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form.ChangeFormTarget;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.LicenceOperation;
import uk.co.nstauthority.licensingmanagementservice.licence.position.change.OrganisationChangeContext;

/**
 * A change form that replaces the single organisation a position holds in some role, such as its licence administrator
 * or storage operator. Everything but the role and the operation is shared.
 *
 * @param <F> the form the page binds
 */
public abstract class OrganisationChangeFormHandler<F extends OrganisationChangeForm> implements ChangeFormHandler<F> {

  static final String TEMPLATE_NAME = "lms/licence/correction/change/addOrganisationChange";

  private final OrganisationChangeService organisationChangeService;
  private final OrganisationChangeFormValidator organisationChangeFormValidator;
  private final OrganisationUnitQueryService organisationUnitQueryService;

  protected OrganisationChangeFormHandler(
      OrganisationChangeService organisationChangeService,
      OrganisationChangeFormValidator organisationChangeFormValidator,
      OrganisationUnitQueryService organisationUnitQueryService
  ) {
    this.organisationChangeService = organisationChangeService;
    this.organisationChangeFormValidator = organisationChangeFormValidator;
    this.organisationUnitQueryService = organisationUnitQueryService;
  }

  /**
   * The role the organisation holds, as it reads mid-sentence, e.g. "licence administrator".
   */
  protected abstract String operatorType();

  /**
   * The operation giving the role to the organisation.
   */
  protected abstract LicenceOperation operation(Integer organisationId);

  /**
   * Stages a correction of the live change, so it gives the role to the organisation instead.
   */
  protected void correctLiveChange(ChangeFormTarget.LiveChange liveChange, Integer organisationId) {
    organisationChangeService.correctOrganisationChange(
        liveChange.position(),
        liveChange.correction(),
        operationType(),
        liveChange.change().getId().toString(),
        operation(organisationId)
    );
  }

  @Override
  public ModelAndView formPage(ChangeFormTarget target) {
    var context = changeContext(target);

    var form = switch (target) {
      case ChangeFormTarget.ExecutedPosition(var correction, var position) -> organisationChangeService
          .populateExistingOrganisationForm(context.currentId(), correction, position, newForm());
      case ChangeFormTarget.AddedPosition(var positionCorrection) -> organisationChangeService
          .populateAddedOrganisationForm(context.currentId(), positionCorrection, newForm());
      case ChangeFormTarget.LiveChange ignored -> formForCorrectingChange(context);
    };

    return formPage(form, context);
  }

  @Override
  public Optional<ModelAndView> formPageIfInvalid(ChangeFormTarget target, F form, BindingResult bindingResult) {
    var context = changeContext(target);

    if (organisationChangeFormValidator.hasErrors(form, bindingResult, context.previousId(), operatorType())) {
      return Optional.of(formPage(form, context));
    }

    return Optional.empty();
  }

  @Override
  public void save(ChangeFormTarget target, F form) {
    var organisationId = Integer.parseInt(form.getId().getInputValue());

    switch (target) {
      case ChangeFormTarget.ExecutedPosition(var correction, var position) -> organisationChangeService
          .addOrganisationChangeForExistingLicencePosition(
              position,
              correction,
              operationType(),
              operation(organisationId)
          );
      case ChangeFormTarget.AddedPosition(var positionCorrection) -> organisationChangeService
          .addOrganisationChangeForAddedLicencePosition(positionCorrection, operationType(), operation(organisationId));
      case ChangeFormTarget.LiveChange liveChange -> correctLiveChange(liveChange, organisationId);
    }
  }

  /**
   * Correcting a live change starts from the organisation the change currently gives the role to.
   */
  private F formForCorrectingChange(OrganisationChangeContext context) {
    var form = newForm();
    if (context.currentId() != null) {
      form.getId().setInputValue(String.valueOf(context.currentId()));
    }
    return form;
  }

  private OrganisationChangeContext changeContext(ChangeFormTarget target) {
    return organisationChangeService.getOrganisationChangeContext(
        target.correction(),
        target.positionId(),
        operationType()
    );
  }

  private ModelAndView formPage(F form, OrganisationChangeContext context) {
    return new ModelAndView(TEMPLATE_NAME)
        .addObject("pageTitle", "Change %s".formatted(operatorType()))
        .addObject("operatorType", operatorType())
        .addObject("previousOperatorName", context.previousName())
        .addObject("form", form)
        .addObject(
            "preselectedOperator",
            organisationUnitQueryService.getOrganisationUnitSelectOption(form.getId().getInputValue())
        )
        .addObject(
            "organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null))
        );
  }
}
