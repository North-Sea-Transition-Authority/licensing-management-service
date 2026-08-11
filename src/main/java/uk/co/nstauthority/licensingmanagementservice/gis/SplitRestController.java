package uk.co.nstauthority.licensingmanagementservice.gis;

import static java.time.Duration.between;

import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import uk.co.fivium.gisframework.operator.JsonSplitResponse;
import uk.co.fivium.gisframework.operator.OperatorCommandReceiver;
import uk.co.fivium.gisframework.operator.SplitFromMapRequest;

@RestController
@RequestMapping("/api/gis-framework")
class SplitRestController {

  private static final Logger LOGGER = LoggerFactory.getLogger(SplitRestController.class);

  private final OperatorCommandReceiver operatorCommandReceiver;

  SplitRestController(OperatorCommandReceiver operatorCommandReceiver) {
    this.operatorCommandReceiver = operatorCommandReceiver;
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
}
