package uk.co.nstauthority.licensingmanagementservice.mockups.licencecorrection.operator;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.ModelAndView;
import uk.co.nstauthority.licensingmanagementservice.energyportal.organisations.OrganisationUnitRestController;
import uk.co.nstauthority.licensingmanagementservice.fds.searchselector.SearchSelectorService;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.CorrectionMarker;
import uk.co.nstauthority.licensingmanagementservice.licence.correction.position.change.AddPositionChangeType;
import uk.co.nstauthority.licensingmanagementservice.mvc.ReverseRouter;
import uk.co.nstauthority.licensingmanagementservice.util.enumutil.DisplayableEnumOptionUtil;

@Controller
@RequestMapping("/mockups/operator-correction/{operatorType}")
@Profile("mockups")
public class OperatorCorrectionController {

  private static final String OPERATOR_CHANGE_TYPE = "OPERATOR";

  private static final String WITHDRAWING_OPERATOR = "BP EXPLORATION (ALPHA) LIMITED";

  private static final String JOINING_OPERATOR = "SHELL U.K. LIMITED";

  private static final String CORRECTED_BLOCK_ID = "14/29B";

  private static final String CORRECTED_SUBAREA_ID = "SUBAREA 1";

  private static final Map<String, String> BLOCK_OPTIONS = blockOptions();


  @GetMapping
  ModelAndView renderPlayback(@PathVariable String operatorType) {
    var mockOperatorType = MockOperatorType.fromUrlSlug(operatorType);
    return new ModelAndView("lms/mockups/licencecorrection/operator/changeStatePlayback")
        .addObject("operatorType", mockOperatorType)
        .addObject("addedMarker", CorrectionMarker.CHANGE_ADDED)
        .addObject("correctedMarker", CorrectionMarker.CHANGE_CORRECTED)
        .addObject("addChangeUrl", ReverseRouter.route(on(this.getClass()).renderAddChange(operatorType)))
        .addObject("correctUrl", ReverseRouter.route(on(this.getClass()).renderCorrect(operatorType)))
        .addObject("removeUrl", ReverseRouter.route(on(this.getClass()).renderRemove(operatorType)));
  }

  @GetMapping("/add-change")
  ModelAndView renderAddChange(@PathVariable String operatorType) {
    var mockOperatorType = MockOperatorType.fromUrlSlug(operatorType);
    return new ModelAndView("lms/mockups/licencecorrection/operator/addChange")
        .addObject("operatorType", mockOperatorType)
        .addObject("form", new OperatorCorrectionForm())
        .addObject("changeTypeOptions", changeTypeOptions(mockOperatorType))
        .addObject("operatorChangeType", OPERATOR_CHANGE_TYPE)
        .addObject("blockOptions", BLOCK_OPTIONS)
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderPlayback(operatorType)));
  }

  @PostMapping("/add-change")
  ModelAndView submitAddChange(@PathVariable String operatorType, @ModelAttribute("form") OperatorCorrectionForm form) {
    if (!OPERATOR_CHANGE_TYPE.equals(form.getChangeType())) {
      return ReverseRouter.redirect(on(this.getClass()).renderPlayback(operatorType));
    }

    return ReverseRouter.redirect(
        on(this.getClass()).renderAdd(operatorType, form.getBlockId(), form.getSubareaId())
    );
  }

  @GetMapping("/add")
  ModelAndView renderAdd(
      @PathVariable String operatorType,
      @RequestParam(required = false) String blockId,
      @RequestParam(required = false) String subareaId
  ) {
    var mockOperatorType = MockOperatorType.fromUrlSlug(operatorType);
    return getCorrectionModelAndView(mockOperatorType, blockId, subareaId, new OperatorCorrectionForm(), Map.of(), false)
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderAddChange(operatorType)));
  }

  @PostMapping("/add")
  ModelAndView submitAdd(@PathVariable String operatorType) {
    return ReverseRouter.redirect(on(this.getClass()).renderPlayback(operatorType));
  }

  @GetMapping("/correct")
  ModelAndView renderCorrect(@PathVariable String operatorType) {
    var mockOperatorType = MockOperatorType.fromUrlSlug(operatorType);
    var form = new OperatorCorrectionForm();
    form.setOperatorId("140141");
    return getCorrectionModelAndView(
        mockOperatorType,
        CORRECTED_BLOCK_ID,
        CORRECTED_SUBAREA_ID,
        form,
        Map.of("140141", JOINING_OPERATOR),
        true
    )
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderPlayback(operatorType)));
  }

  @PostMapping("/correct")
  ModelAndView submitCorrect(@PathVariable String operatorType) {
    return ReverseRouter.redirect(on(this.getClass()).renderPlayback(operatorType));
  }

  @GetMapping("/remove")
  ModelAndView renderRemove(@PathVariable String operatorType) {
    var mockOperatorType = MockOperatorType.fromUrlSlug(operatorType);
    return new ModelAndView("lms/mockups/licencecorrection/operator/removeOperatorCorrection")
        .addObject("operatorType", mockOperatorType)
        .addObject("withdrawingOperator", WITHDRAWING_OPERATOR)
        .addObject("joiningOperator", JOINING_OPERATOR)
        .addObject("backUrl", ReverseRouter.route(on(this.getClass()).renderPlayback(operatorType)));
  }

  private ModelAndView getCorrectionModelAndView(
      MockOperatorType operatorType,
      String blockId,
      String subareaId,
      OperatorCorrectionForm form,
      Map<String, String> preselectedOperator,
      boolean isCorrectingExistingChange
  ) {
    return new ModelAndView("lms/mockups/licencecorrection/operator/operatorCorrection")
        .addObject("operatorType", operatorType)
        .addObject("form", form)
        .addObject("block", BLOCK_OPTIONS.getOrDefault(blockId, ""))
        .addObject("previousOperator", WITHDRAWING_OPERATOR)
        .addObject("preselectedOperator", preselectedOperator)
        .addObject("isCorrectingExistingChange", isCorrectingExistingChange)
        .addObject("organisationUnitsUrl",
            SearchSelectorService.route(on(OrganisationUnitRestController.class).searchOrganisationUnits(null)));
  }

  private static Map<String, String> changeTypeOptions(MockOperatorType operatorType) {
    var availableChangeTypes = Arrays.stream(AddPositionChangeType.values())
        .filter(changeType -> changeType.isAvailableFor(operatorType.getLicenceType()))
        .toList();
    var options = new LinkedHashMap<>(DisplayableEnumOptionUtil.getDisplayableOptions(availableChangeTypes));
    options.put(OPERATOR_CHANGE_TYPE, "%s change".formatted(operatorType.getDisplayName()));
    return options;
  }

  private static Map<String, String> blockOptions() {
    var options = new LinkedHashMap<String, String>();
    options.put("14/29A", "14/29a - SUBAREA 1");
    options.put("14/29B", "14/29b - SUBAREA 2");
    options.put("15/17C", "15/17c - SUBAREA 3");
    return options;
  }
}