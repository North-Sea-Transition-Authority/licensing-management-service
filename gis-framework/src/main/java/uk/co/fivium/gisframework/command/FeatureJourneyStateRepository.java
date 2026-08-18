package uk.co.fivium.gisframework.command;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.ListCrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import uk.co.fivium.gisframework.feature.Feature;
import uk.co.fivium.grpc.gis.CoordinateSystem;

@Repository
interface FeatureJourneyStateRepository extends ListCrudRepository<FeatureJourneyState, UUID> {

  @Query("SELECT state.feature FROM FeatureJourneyState state " +
      "WHERE state.commandJourney = :commandJourney AND state.active = true")
  List<Feature> findActiveFeaturesByCommandJourney(@Param("commandJourney") CommandJourney commandJourney);

  List<FeatureJourneyState> findAllByCreatedByCommand(OperatorCommand createdByCommand);

  List<FeatureJourneyState> findAllByCreatedByCommandIn(Collection<OperatorCommand> createdByCommands);

  List<FeatureJourneyState> findAllByFeature_IdIn(Set<UUID> featureIds);

  /**
   * Used for GIS test page, will not be needed in the future.
   */
  @Query("SELECT f FROM Feature f WHERE f.coordinateSystem = :coordinateSystem " +
      "AND NOT EXISTS (SELECT 1 FROM FeatureJourneyState state WHERE state.feature = f)")
  List<Feature> findFeaturesWithNoJourneyState(@Param("coordinateSystem") CoordinateSystem coordinateSystem, Limit limit);
}
