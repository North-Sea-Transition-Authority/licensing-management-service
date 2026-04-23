package uk.co.fivium.gisframework.migration;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.FeatureService;
import uk.co.fivium.gisframework.feature.LineService;
import uk.co.fivium.gisframework.feature.PolygonService;
import uk.co.fivium.gisframework.migration.oracle.OracleService;

@Profile("gis-migration")
@Service
public class MigrationService {

  private final FeatureService featureService;
  private final PolygonService polygonService;
  private final LineService lineService;

  private final OracleService oracleService;

  public MigrationService(
      FeatureService featureService,
      PolygonService polygonService,
      LineService lineService,
      OracleService oracleService
  ) {
    this.featureService = featureService;
    this.polygonService = polygonService;
    this.lineService = lineService;
    this.oracleService = oracleService;
  }

  void migrate() {
    // TODO EPGF-16 migrate
  }
}
