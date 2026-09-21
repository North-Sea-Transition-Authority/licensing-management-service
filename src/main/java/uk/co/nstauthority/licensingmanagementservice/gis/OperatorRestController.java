package uk.co.nstauthority.licensingmanagementservice.gis;

import static java.time.Duration.between;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.fivium.gisframework.command.CommandJourneyService;
import uk.co.fivium.gisframework.command.OperatorCommandService;
import uk.co.fivium.gisframework.operator.JsonHistoryStatus;
import uk.co.fivium.gisframework.operator.JsonMergeResponse;
import uk.co.fivium.gisframework.operator.JsonSplitResponse;
import uk.co.fivium.gisframework.operator.MergeFromMapRequest;
import uk.co.fivium.gisframework.operator.OperatorCommandReceiver;
import uk.co.fivium.gisframework.operator.SplitFromMapRequest;

@RestController
@RequestMapping("/api/gis-framework")
class OperatorRestController {

  private static final Logger LOGGER = LoggerFactory.getLogger(OperatorRestController.class);

  private final OperatorCommandReceiver operatorCommandReceiver;
  private final CommandJourneyService commandJourneyService;
  private final OperatorCommandService operatorCommandService;

  OperatorRestController(
      OperatorCommandReceiver operatorCommandReceiver,
      CommandJourneyService commandJourneyService,
      OperatorCommandService operatorCommandService) {
    this.operatorCommandReceiver = operatorCommandReceiver;
    this.commandJourneyService = commandJourneyService;
    this.operatorCommandService = operatorCommandService;
  }

  @PostMapping("/split")
  ResponseEntity<JsonSplitResponse> split(@RequestBody SplitFromMapRequest request) {
    LOGGER.info("Received split request for '{}'", request);
    var startingInstant = Instant.now();
    var outputFeatures = operatorCommandReceiver.executeSplit(request);
    List<String> outputFeatureIds = outputFeatures.stream().map(feature -> feature.getId().toString()).toList();
    LOGGER.info("Split request completed successfully for '{} took {}ms created {} output features'",
        request,
        between(startingInstant, Instant.now()).toMillis(),
        outputFeatureIds.size()
    );
    return ResponseEntity.ok(
        new JsonSplitResponse(outputFeatureIds)
    );
  }

  @PostMapping("/merge")
  ResponseEntity<JsonMergeResponse> merge(@RequestBody MergeFromMapRequest request) {
    LOGGER.info("Received merge request for '{}'", request);
    var startingInstant = Instant.now();
    var outputFeature = operatorCommandReceiver.executeMerge(request);
    if (outputFeature.isEmpty()) {
      LOGGER.warn("Merge request '{}' produced no output feature", request);
      return ResponseEntity.badRequest().build();
    }

    String outputFeatureId = outputFeature.get().getId().toString();
    LOGGER.info("Merge request completed successfully for '{}' took {}ms",
        request,
        between(startingInstant, Instant.now()).toMillis()
    );
    return ResponseEntity.ok(new JsonMergeResponse(outputFeatureId));
  }

  @GetMapping("/history/{commandJourneyId}")
  ResponseEntity<JsonHistoryStatus> getHistory(@PathVariable UUID commandJourneyId) {
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(commandJourneyId);
    return ResponseEntity.ok(new JsonHistoryStatus(
        operatorCommandService.canUndo(commandJourney),
        operatorCommandService.canRedo(commandJourney)
    ));
  }

  @PostMapping("/undo/{commandJourneyId}")
  ResponseEntity<Void> undo(@PathVariable UUID commandJourneyId) {
    LOGGER.info("Received undo request for command journey '{}'", commandJourneyId);
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(commandJourneyId);
    var reactivatedFeatures = operatorCommandReceiver.undo(commandJourney);
    List<String> reactivatedFeatureIds = reactivatedFeatures.stream().map(feature -> feature.getId().toString()).toList();
    LOGGER.info("Undo request completed successfully for command journey '{}', reactivated {} features",
        commandJourneyId, reactivatedFeatureIds.size());
    return ResponseEntity.ok().build();
  }

  @PostMapping("/redo/{commandJourneyId}")
  ResponseEntity<Void> redo(@PathVariable UUID commandJourneyId) {
    LOGGER.info("Received redo request for command journey '{}'", commandJourneyId);
    var commandJourney = commandJourneyService.getCommandJourneyOrThrow(commandJourneyId);
    var reactivatedFeatures = operatorCommandReceiver.redo(commandJourney);
    List<String> reactivatedFeatureIds = reactivatedFeatures.stream().map(feature -> feature.getId().toString()).toList();
    LOGGER.info("Redo request completed successfully for command journey '{}', reactivated {} features",
        commandJourneyId, reactivatedFeatureIds.size());
    return ResponseEntity.ok().build();
  }
}
