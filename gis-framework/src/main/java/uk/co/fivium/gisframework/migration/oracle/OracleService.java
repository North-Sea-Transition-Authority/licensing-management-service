package uk.co.fivium.gisframework.migration.oracle;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("gis-migration")
@Service
public class OracleService {
  private final OracleShapeRepository shapeRepository;
  private final OracleShapeLinkRepository shapeLinkRepository;
  private final OracleMigrationTrackerRepository trackerRepository;
  private final OracleShapePolygonRepository polygonRepository;
  private final OraclePolygonBoundaryRepository boundaryRepository;
  private final OracleBoundaryLineRepository lineRepository;
  private final OracleAttributeRepository attributeRepository;

  public OracleService(
      OracleShapeRepository shapeRepository,
      OracleShapeLinkRepository shapeLinkRepository,
      OracleMigrationTrackerRepository trackerRepository,
      OracleShapePolygonRepository shapePolygonRepository,
      OraclePolygonBoundaryRepository polygonBoundaryRepository,
      OracleBoundaryLineRepository boundaryLineRepository,
      OracleAttributeRepository attributeRepository
  ) {
    this.shapeRepository = shapeRepository;
    this.shapeLinkRepository = shapeLinkRepository;
    this.trackerRepository = trackerRepository;
    this.polygonRepository = shapePolygonRepository;
    this.boundaryRepository = polygonBoundaryRepository;
    this.lineRepository = boundaryLineRepository;
    this.attributeRepository = attributeRepository;
  }

  /**
   * This method gets a list of shapes and all associated entities, and maps them together in the EntityBackedOracleShape record.
   *
   * @param ids the ids of the shapes.
   * @return a list of records that maps lines to boundaries, and boundaries to polygons for a given shape.
   */
  public List<EntityBackedOracleShape> getEntityBackedOracleShapesByIdsIn(Collection<Integer> ids) {
    return getEntityBackedOracleShapes(shapeRepository.findAllById(ids));
  }

  public List<EntityBackedOracleShape> getEntityBackedOracleShapesForMigrationOrderNumber(Integer orderNumber) {
    var oracleShapeSiIds = trackerRepository.findAllByOrderNumber(orderNumber)
        .stream()
        .map(OracleMigrationTracker::getShapeSiId)
        .toList();

    return getEntityBackedOracleShapes(shapeRepository.findAllById(oracleShapeSiIds));
  }

  private List<EntityBackedOracleShape> getEntityBackedOracleShapes(Collection<OracleShape> oracleShapes) {
    var polygons = getAllPolygonsForShapesIn(oracleShapes);
    var boundaries = getAllBoundariesForPolygonsIn(polygons);
    var lines = getAllLinesForBoundariesIn(boundaries);

    var polygonsByShapeId = polygons
        .stream()
        .collect(Collectors.groupingBy(OracleShapePolygon::getOracleShapeId));

    var boundariesByShapeId = boundaries
        .stream()
        .collect(Collectors.groupingBy(OraclePolygonBoundary::getShapeSiId));

    var boundariesByPolygonId = boundaries
        .stream()
        .collect(Collectors.groupingBy(OraclePolygonBoundary::getOracleShapePolygonId));

    var linesByBoundaryId = lines.stream()
        .collect(Collectors.groupingBy(
            OracleBoundaryLine::getOraclePolygonBoundaryId,
            Collectors.collectingAndThen(
                Collectors.toList(),
                list -> {
                  list.sort(Comparator.comparing(OracleBoundaryLine::getConnectionOrder));
                  return list;
                }
            )
        ));

    return oracleShapes.stream()
        .map(oracleShape -> toEntityBackedOracleShape(
            oracleShape,
            polygonsByShapeId,
            boundariesByShapeId,
            boundariesByPolygonId,
            linesByBoundaryId
        ))
        .sorted(Comparator.comparing(entityBackedOracleShape -> entityBackedOracleShape.shape().getShapeStartDate()))
        .toList();
  }

  public Optional<Integer> getLinkedParentShapeSiId(Integer childShapeSiId) {
    return shapeLinkRepository.findByChildShapeId(childShapeSiId)
        .map(OracleShapeLink::getParentShapeId);
  }

  public Map<String, Object> getAttributeMapForSiIdAndLevel(Integer siId, AttributeLevel level) {
    return attributeRepository.findAllByAssociatedSiIdAndAttributeLevel(siId, level)
        .stream()
        .filter(attribute -> attribute.getAttributeName() != null && attribute.getAttributeValue() != null)
        .collect(
            Collectors.toMap(
                OracleAttribute::getAttributeName,
                OracleAttribute::getAttributeValue
            )
        );
  }

  public Map<Integer, Map<String, String>> getIdToAttributeMapForSiIdAndLevel(Collection<Integer> siIds, AttributeLevel level) {
    return attributeRepository.findAllByAssociatedSiIdInAndAttributeLevel(siIds, level)
        .stream()
        .filter(attribute -> attribute.getAttributeName() != null && attribute.getAttributeValue() != null)
        .collect(Collectors.groupingBy(
            OracleAttribute::getAssociatedSiId,
            Collectors.toMap(
                OracleAttribute::getAttributeName,
                OracleAttribute::getAttributeValue
            )
        ));
  }

  private EntityBackedOracleShape toEntityBackedOracleShape(
      OracleShape oracleShape,
      Map<Integer, List<OracleShapePolygon>> polygonsByShapeId,
      Map<Integer, List<OraclePolygonBoundary>> boundariesByShapeId,
      Map<Integer, List<OraclePolygonBoundary>> boundariesByPolygonId,
      Map<Integer, List<OracleBoundaryLine>> linesByBoundaryId
  ) {
    var shapeId = oracleShape.getShapeSiId();
    var shapePolygons = polygonsByShapeId.get(shapeId);
    var shapeBoundaries = boundariesByShapeId.get(shapeId);

    var polygonToBoundary = shapePolygons.stream()
        .collect(Collectors.toMap(
            Function.identity(),
            polygon -> boundariesByPolygonId.get(polygon.getPolygonSidId())
        ));

    var boundaryToLine = shapeBoundaries.stream()
        .collect(Collectors.toMap(
            Function.identity(),
            boundary -> linesByBoundaryId.get(boundary.getBoundarySidId())
        ));

    return new EntityBackedOracleShape(oracleShape, polygonToBoundary, boundaryToLine);
  }

  private List<OracleShapePolygon> getAllPolygonsForShapesIn(Collection<OracleShape> oracleShapes) {
    var shapeIds = oracleShapes.stream()
        .map(OracleShape::getShapeSiId)
        .toList();

    return polygonRepository.findAllByOracleShapeIdIn(shapeIds);
  }

  private List<OraclePolygonBoundary> getAllBoundariesForPolygonsIn(Collection<OracleShapePolygon> polygons) {
    var polygonIds = polygons.stream()
        .map(OracleShapePolygon::getPolygonSidId)
        .toList();

    return boundaryRepository.findAllByOracleShapePolygonIdIn(polygonIds);
  }

  private List<OracleBoundaryLine> getAllLinesForBoundariesIn(Collection<OraclePolygonBoundary> boundaries) {
    var boundaryIds = boundaries.stream()
        .map(OraclePolygonBoundary::getBoundarySidId)
        .toList();

    return lineRepository.findAllByOraclePolygonBoundaryIdIn(boundaryIds);
  }
}
