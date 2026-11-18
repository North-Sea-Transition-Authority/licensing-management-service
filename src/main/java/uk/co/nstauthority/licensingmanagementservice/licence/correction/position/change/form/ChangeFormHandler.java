package uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.form;

import java.util.Optional;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.licence.operation.VisibleLicenceOperation;

/**
 * The parts of adding or correcting a change through a single form page that differ by the kind of change. Routing,
 * access checks, form binding, the cancel link, banners and redirects are shared by {@link ChangeFormController}.
 *
 * @param <F> the form the page binds
 */
public interface ChangeFormHandler<F> {

  /**
   * The change type that selects this handler when adding a change.
   */
  AddPositionChangeType changeType();

  /**
   * The operation a change carries for this handler to correct it.
   */
  Class<? extends VisibleLicenceOperation> operationType();

  /**
   * The change as it reads at the start of a banner, e.g. "Licensee change".
   */
  String displayName();

  /**
   * An empty form for the submitted request to be bound to.
   */
  F newForm();

  /**
   * The form page, with the form pre-filled from what the target already holds. The controller adds
   * {@code cancelUrl}.
   */
  ModelAndView formPage(ChangeFormTarget target);

  /**
   * The form page showing the submitted form's errors, or empty when the form is valid. The controller adds
   * {@code cancelUrl}.
   */
  Optional<ModelAndView> formPageIfInvalid(ChangeFormTarget target, F form, BindingResult bindingResult);

  /**
   * Stages the change the valid form describes.
   */
  void save(ChangeFormTarget target, F form);
}
