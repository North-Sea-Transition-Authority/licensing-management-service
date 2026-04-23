package uk.co.fivium.gisframework.migration.oracle;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("gis-migration")
@Service
public class OracleService {

  private final OracleShapeRepository shapeRepository;
  private final OracleShapePolygonRepository polygonRepository;
  private final OraclePolygonBoundaryRepository boundaryRepository;
  private final OracleBoundaryLineRepository lineRepository;

  public OracleService(
      OracleShapeRepository shapeRepository,
      OracleShapePolygonRepository shapePolygonRepository,
      OraclePolygonBoundaryRepository polygonBoundaryRepository,
      OracleBoundaryLineRepository boundaryLineRepository
  ) {
    this.shapeRepository = shapeRepository;
    this.polygonRepository = shapePolygonRepository;
    this.boundaryRepository = polygonBoundaryRepository;
    this.lineRepository = boundaryLineRepository;
  }

  /**
   * This method gets a list of shapes and all associated entities, and maps them together in the EntityBackedOracleShape record.
   * @param ids the ids of the shapes.
   * @return a list of records that maps lines to boundaries, and boundaries to polygons for a given shape.
   */
  public List<EntityBackedOracleShape> getEntityBackedOracleShapesByIdsIn(Collection<OracleShapeCompositeKey> ids) {
    var oracleShapes = shapeRepository.findAllById(ids)
        .stream()
        .sorted(Comparator.comparing(OracleShape::getShapeSidId))
        .toList();

    return getEntityBackedOracleShapes(oracleShapes);
  }

  private List<EntityBackedOracleShape> getEntityBackedOracleShapes(Collection<OracleShape> oracleShapes) {
    var entityBackedOracleShapes = new ArrayList<EntityBackedOracleShape>();

    for (var oracleShape : oracleShapes) {
      var polygonToBoundary = new HashMap<OracleShapePolygon, List<OraclePolygonBoundary>>();
      var boundaryToLine = new HashMap<OraclePolygonBoundary, List<OracleBoundaryLine>>();

      var oraclePolygons = getPolygonsByShapeSidId(oracleShape.getShapeSidId());
      for (var oraclePolygon : oraclePolygons) {
        var oracleBoundaries = getBoundariesByPolygonSidId(oraclePolygon.getPolygonSidId().longValue());

        polygonToBoundary.put(oraclePolygon, oracleBoundaries);

        for (var oracleBoundary : oracleBoundaries) {
          var lines = getLinesByBoundarySidId(oracleBoundary.getBoundarySidId())
              .stream()
              .sorted(Comparator.comparing(OracleBoundaryLine::getConnectionOrder))
              .toList();
          boundaryToLine.put(oracleBoundary, lines);
        }
      }
      entityBackedOracleShapes.add(
          new EntityBackedOracleShape(
              oracleShape,
              polygonToBoundary,
              boundaryToLine
          )
      );
    }
    return entityBackedOracleShapes;
  }

  private List<OracleShapePolygon> getPolygonsByShapeSidId(Integer shapeSidId) {
    return polygonRepository.findAllByShapeSidId(shapeSidId);
  }

  private List<OraclePolygonBoundary> getBoundariesByPolygonSidId(Long polygonSidId) {
    return boundaryRepository.findAllByPolygonSidId(polygonSidId);
  }

  private List<OracleBoundaryLine> getLinesByBoundarySidId(Long boundarySidId) {
    return lineRepository.findAllByBoundarySidId(boundarySidId);
  }
}
