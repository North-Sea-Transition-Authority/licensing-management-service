package uk.co.fivium.gisframework.migration.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OracleServiceTest {

  private static final Integer SHAPE_1_SID = 1;
  private static final OracleShapeCompositeKey KEY_1 = new OracleShapeCompositeKey(SHAPE_1_SID, "TC1");
  private static final OracleShape SHAPE_1 = OracleShapeTestUtil.newBuilder()
      .withShapeSidId(SHAPE_1_SID)
      .withTestCase("TC1")
      .build();

  private static final Long POLYGON_1_SID = 10L;
  private static final OracleShapePolygon POLYGON_1 = OracleShapePolygonTestUtil.newBuilder()
      .withPolygonSidId(POLYGON_1_SID.intValue())
      .withShapeSidId(SHAPE_1_SID)
      .build();

  private static final Long BOUNDARY_1_SID = 100L;
  private static final OraclePolygonBoundary BOUNDARY_1 = OraclePolygonBoundaryTestUtil.newBuilder()
      .withBoundarySidId(BOUNDARY_1_SID)
      .withPolygonSidId(POLYGON_1_SID)
      .build();

  private static final OracleBoundaryLine LINE_ORDER_1 = OracleBoundaryLineTestUtil.newBuilder()
      .withLineSidId(1001L)
      .withBoundarySidId(BOUNDARY_1_SID)
      .withConnectionOrder(1L)
      .build();
  private static final OracleBoundaryLine LINE_ORDER_2 = OracleBoundaryLineTestUtil.newBuilder()
      .withLineSidId(1002L)
      .withBoundarySidId(BOUNDARY_1_SID)
      .withConnectionOrder(2L)
      .build();

  @Mock
  private OracleShapeRepository shapeRepository;

  @Mock
  private OracleShapePolygonRepository polygonRepository;

  @Mock
  private OraclePolygonBoundaryRepository boundaryRepository;

  @Mock
  private OracleBoundaryLineRepository lineRepository;

  @InjectMocks
  private OracleService oracleService;

  @Test
  void getEntityBackedOracleShapesByIdsIn_emptyIds_returnsEmptyList() {
    when(shapeRepository.findAllById(List.of())).thenReturn(List.of());

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of());

    assertThat(result).isEmpty();
  }

  @Test
  void getEntityBackedOracleShapesByIdsIn_singleShapeNoPolygons_returnsShapeWithEmptyMaps() {
    when(shapeRepository.findAllById(List.of(KEY_1))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByShapeSidId(SHAPE_1_SID)).thenReturn(List.of());

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(KEY_1));

    var expected = List.of(new EntityBackedOracleShape(SHAPE_1, Map.of(), Map.of()));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedOracleShapesByIdsIn_singleShapeWithPolygonAndNoBoundaries() {
    when(shapeRepository.findAllById(List.of(KEY_1))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByShapeSidId(SHAPE_1_SID)).thenReturn(List.of(POLYGON_1));
    when(boundaryRepository.findAllByPolygonSidId(POLYGON_1_SID)).thenReturn(List.of());

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(KEY_1));

    var expected = List.of(new EntityBackedOracleShape(SHAPE_1, Map.of(POLYGON_1, List.of()), Map.of()));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedOracleShapesByIdsIn_fullHierarchy_mapsCorrectly() {
    when(shapeRepository.findAllById(List.of(KEY_1))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByShapeSidId(SHAPE_1_SID)).thenReturn(List.of(POLYGON_1));
    when(boundaryRepository.findAllByPolygonSidId(POLYGON_1_SID)).thenReturn(List.of(BOUNDARY_1));
    when(lineRepository.findAllByBoundarySidId(BOUNDARY_1_SID)).thenReturn(List.of(LINE_ORDER_2, LINE_ORDER_1));

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(KEY_1));

    var expected = List.of(new EntityBackedOracleShape(
        SHAPE_1,
        Map.of(POLYGON_1, List.of(BOUNDARY_1)),
        Map.of(BOUNDARY_1, List.of(LINE_ORDER_1, LINE_ORDER_2))
    ));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedOracleShapesByIdsIn_multipleShapes_returnedSortedByShapeSidId() {
    var key2 = new OracleShapeCompositeKey(2, "TC1");
    var shape2 = OracleShapeTestUtil.newBuilder().withShapeSidId(2).withTestCase("TC1").build();

    when(shapeRepository.findAllById(List.of(key2, KEY_1))).thenReturn(List.of(shape2, SHAPE_1));
    when(polygonRepository.findAllByShapeSidId(key2.getShapeSidId())).thenReturn(List.of());
    when(polygonRepository.findAllByShapeSidId(SHAPE_1_SID)).thenReturn(List.of());

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(key2, KEY_1));

    var expected = List.of(
        new EntityBackedOracleShape(SHAPE_1, Map.of(), Map.of()),
        new EntityBackedOracleShape(shape2, Map.of(), Map.of())
    );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedOracleShapesByIdsIn_multiplePolygonsAndBoundaries() {
    var polygon2 = OracleShapePolygonTestUtil.newBuilder()
        .withPolygonSidId(20)
        .withShapeSidId(SHAPE_1_SID)
        .build();
    var boundary2 = OraclePolygonBoundaryTestUtil.newBuilder()
        .withBoundarySidId(200L)
        .withPolygonSidId(20L)
        .build();
    var lineBoundary2 = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(2000L)
        .withBoundarySidId(200L)
        .withConnectionOrder(1L).build();

    when(shapeRepository.findAllById(List.of(KEY_1))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByShapeSidId(SHAPE_1_SID)).thenReturn(List.of(POLYGON_1, polygon2));
    when(boundaryRepository.findAllByPolygonSidId(POLYGON_1_SID)).thenReturn(List.of(BOUNDARY_1));
    when(boundaryRepository.findAllByPolygonSidId(20L)).thenReturn(List.of(boundary2));
    when(lineRepository.findAllByBoundarySidId(BOUNDARY_1_SID)).thenReturn(List.of(LINE_ORDER_1));
    when(lineRepository.findAllByBoundarySidId(200L)).thenReturn(List.of(lineBoundary2));

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(KEY_1));

    var expected = List.of(new EntityBackedOracleShape(
        SHAPE_1,
        Map.of(POLYGON_1, List.of(BOUNDARY_1), polygon2, List.of(boundary2)),
        Map.of(BOUNDARY_1, List.of(LINE_ORDER_1), boundary2, List.of(lineBoundary2))
    ));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedOracleShapesByIdsIn_linesAreSortedByConnectionOrder() {
    var lineOrder3 = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(1003L)
        .withBoundarySidId(100L)
        .withConnectionOrder(3L)
        .build();

    when(shapeRepository.findAllById(List.of(KEY_1))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByShapeSidId(SHAPE_1_SID)).thenReturn(List.of(POLYGON_1));
    when(boundaryRepository.findAllByPolygonSidId(POLYGON_1_SID)).thenReturn(List.of(BOUNDARY_1));
    when(lineRepository.findAllByBoundarySidId(BOUNDARY_1_SID)).thenReturn(List.of(lineOrder3, LINE_ORDER_1, LINE_ORDER_2));

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(KEY_1));

    var expected = List.of(new EntityBackedOracleShape(
        SHAPE_1,
        Map.of(POLYGON_1, List.of(BOUNDARY_1)),
        Map.of(BOUNDARY_1, List.of(LINE_ORDER_1, LINE_ORDER_2, lineOrder3))
    ));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }
}
