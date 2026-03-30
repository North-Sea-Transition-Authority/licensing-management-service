package uk.co.nstauthority.licensingmanagementservice.energyportal.subarea;

import java.util.List;
import org.springframework.stereotype.Service;
import uk.co.fivium.energyportalapi.client.RequestPurpose;
import uk.co.fivium.energyportalapi.client.subarea.SubareaApi;
import uk.co.fivium.energyportalapi.generated.client.SubareasProjectionRoot;
import uk.co.fivium.energyportalapi.generated.types.Subarea;
import uk.co.nstauthority.licensingmanagementservice.correlationid.CorrelationIdUtil;

@Service
public class SubareaQueryService {

  public static final SubareasProjectionRoot SUBAREAS_PROJECTION_ROOT =
      new SubareasProjectionRoot().id().name().operator().organisationUnitId().name().getParent()
          .licenceBlock().reference().root();

  private final SubareaApi subareaApi;

  public SubareaQueryService(SubareaApi subareaApi) {
    this.subareaApi = subareaApi;
  }

  public List<Subarea> searchSubareasByLicenceIds(List<Integer> licenceIds) {
    return subareaApi.searchExtantSubareasByLicenceIds(
        licenceIds,
        SUBAREAS_PROJECTION_ROOT,
        new RequestPurpose("Search extant subareas by licence IDs"),
        CorrelationIdUtil.getLogCorrelationId()
    );
  }
}
