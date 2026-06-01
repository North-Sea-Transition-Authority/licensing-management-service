package uk.co.fivium.gisframework.migration.oracle;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class OracleServiceTest {

  private static final Integer SHAPE_1_SID = 1;
  private static final Integer SHAPE_1_SI_ID = 1;
  private static final OracleShape SHAPE_1 = OracleShapeTestUtil.newBuilder()
      .withShapeSidId(SHAPE_1_SID)
      .withShapeSiId(SHAPE_1_SI_ID)
      .build();

  private static final Long POLYGON_1_SID = 10L;
  private static final OracleShapePolygon POLYGON_1 = OracleShapePolygonTestUtil.newBuilder()
      .withPolygonSidId(POLYGON_1_SID.intValue())
      .withShapeSidId(SHAPE_1_SID)
      .withOracleShapeId(SHAPE_1_SI_ID)
      .build();

  private static final Long BOUNDARY_1_SID = 100L;
  private static final OraclePolygonBoundary BOUNDARY_1 = OraclePolygonBoundaryTestUtil.newBuilder()
      .withBoundarySidId(BOUNDARY_1_SID.intValue())
      .withOracleShapePolygonId(POLYGON_1_SID.intValue())
      .build();

  private static final OracleBoundaryLine LINE_ORDER_1 = OracleBoundaryLineTestUtil.newBuilder()
      .withLineSidId(1001)
      .withOraclePolygonBoundaryId(BOUNDARY_1_SID.intValue())
      .withConnectionOrder(1L)
      .build();
  private static final OracleBoundaryLine LINE_ORDER_2 = OracleBoundaryLineTestUtil.newBuilder()
      .withLineSidId(1002)
      .withOraclePolygonBoundaryId(BOUNDARY_1_SID.intValue())
      .withConnectionOrder(2L)
      .build();

  @Mock
  private OracleShapeRepository shapeRepository;

  @Mock
  private OracleShapeLinkRepository shapeLinkRepository;

  @Mock
  private OracleMigrationTrackerRepository trackerRepository;

  @Mock
  private OracleShapePolygonRepository polygonRepository;

  @Mock
  private OraclePolygonBoundaryRepository boundaryRepository;

  @Mock
  private OracleBoundaryLineRepository lineRepository;

  @Mock
  private OracleAttributeRepository attributeRepository;

  @InjectMocks
  private OracleService oracleService;

  @Test
  void getEntityBackedOracleShapesByIdsIn_fullHierarchy_mapsCorrectly() {
    when(shapeRepository.findAllById(List.of(SHAPE_1_SI_ID))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByOracleShapeIdIn(List.of(SHAPE_1_SI_ID))).thenReturn(List.of(POLYGON_1));
    when(boundaryRepository.findAllByOracleShapePolygonIdIn(List.of(POLYGON_1_SID.intValue())))
        .thenReturn(List.of(BOUNDARY_1));
    when(lineRepository.findAllByOraclePolygonBoundaryIdIn(List.of(BOUNDARY_1_SID.intValue())))
        .thenReturn(List.of(LINE_ORDER_2, LINE_ORDER_1));

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(SHAPE_1_SI_ID));

    var expected = List.of(new EntityBackedOracleShape(
        SHAPE_1,
        Map.of(POLYGON_1, List.of(BOUNDARY_1)),
        Map.of(BOUNDARY_1, List.of(LINE_ORDER_1, LINE_ORDER_2))
    ));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedOracleShapesForMigrationOrderNumber_fullHierarchy_mapsCorrectly() {
    var orderNumber = 20;
    var tracker = OracleMigrationTrackerTestUtil.newBuilder()
        .withShapeSiId(SHAPE_1_SI_ID)
        .withOrderNumber(orderNumber)
        .build();

    when(trackerRepository.findAllByOrderNumber(orderNumber)).thenReturn(List.of(tracker));
    when(shapeRepository.findAllById(List.of(SHAPE_1_SI_ID))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByOracleShapeIdIn(List.of(SHAPE_1_SI_ID))).thenReturn(List.of(POLYGON_1));
    when(boundaryRepository.findAllByOracleShapePolygonIdIn(List.of(POLYGON_1_SID.intValue())))
        .thenReturn(List.of(BOUNDARY_1));
    when(lineRepository.findAllByOraclePolygonBoundaryIdIn(List.of(BOUNDARY_1_SID.intValue())))
        .thenReturn(List.of(LINE_ORDER_2, LINE_ORDER_1));

    var result = oracleService.getEntityBackedOracleShapesForMigrationOrderNumber(orderNumber);

    var expected = List.of(new EntityBackedOracleShape(
        SHAPE_1,
        Map.of(POLYGON_1, List.of(BOUNDARY_1)),
        Map.of(BOUNDARY_1, List.of(LINE_ORDER_1, LINE_ORDER_2))
    ));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedOracleShapesByIdsIn_multipleShapesCorrectlySorted() {
    var shape1 = OracleShapeTestUtil.newBuilder()
        .withShapeSidId(SHAPE_1_SID)
        .withShapeSiId(SHAPE_1_SI_ID)
        .withShapeStartDate(Date.from(LocalDate.of(2021, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()))
        .build();
    var shape2 = OracleShapeTestUtil.newBuilder()
        .withShapeSidId(2)
        .withShapeSiId(2)
        .withShapeStartDate(Date.from(LocalDate.of(2020, 1, 1).atStartOfDay(ZoneOffset.UTC).toInstant()))
        .build();
    var polygon2 = OracleShapePolygonTestUtil.newBuilder()
        .withPolygonSidId(20)
        .withShapeSidId(2)
        .withOracleShapeId(2)
        .build();
    var boundary2 = OraclePolygonBoundaryTestUtil.newBuilder()
        .withBoundarySidId(200)
        .withOracleShapePolygonId(20)
        .withShapeSiId(2)
        .build();
    var line2 = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(2000)
        .withOraclePolygonBoundaryId(200)
        .withConnectionOrder(1L)
        .build();

    when(shapeRepository.findAllById(List.of(2, SHAPE_1_SI_ID))).thenReturn(List.of(shape1, shape2));
    when(polygonRepository.findAllByOracleShapeIdIn(List.of(SHAPE_1_SI_ID, 2)))
        .thenReturn(List.of(POLYGON_1, polygon2));
    when(boundaryRepository.findAllByOracleShapePolygonIdIn(List.of(POLYGON_1_SID.intValue(), 20)))
        .thenReturn(List.of(BOUNDARY_1, boundary2));
    when(lineRepository.findAllByOraclePolygonBoundaryIdIn(List.of(BOUNDARY_1_SID.intValue(), 200)))
        .thenReturn(List.of(LINE_ORDER_1, line2));

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(2, SHAPE_1_SI_ID));

    var expected = List.of(
        new EntityBackedOracleShape(
            shape2,
            Map.of(polygon2, List.of(boundary2)),
            Map.of(boundary2, List.of(line2))
        ),
        new EntityBackedOracleShape(
            shape1,
            Map.of(POLYGON_1, List.of(BOUNDARY_1)),
            Map.of(BOUNDARY_1, List.of(LINE_ORDER_1))
        )
    );
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getEntityBackedOracleShapesByIdsIn_multiplePolygonsAndBoundaries() {
    var polygon2 = OracleShapePolygonTestUtil.newBuilder()
        .withPolygonSidId(20)
        .withShapeSidId(SHAPE_1_SID)
        .withOracleShapeId(SHAPE_1_SI_ID)
        .build();
    var boundary2 = OraclePolygonBoundaryTestUtil.newBuilder()
        .withBoundarySidId(200)
        .withOracleShapePolygonId(20)
        .build();
    var lineBoundary2 = OracleBoundaryLineTestUtil.newBuilder()
        .withLineSidId(2000)
        .withOraclePolygonBoundaryId(200)
        .withConnectionOrder(1L).build();

    when(shapeRepository.findAllById(List.of(SHAPE_1_SI_ID))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByOracleShapeIdIn(List.of(SHAPE_1_SI_ID))).thenReturn(List.of(POLYGON_1, polygon2));
    when(boundaryRepository.findAllByOracleShapePolygonIdIn(List.of(POLYGON_1_SID.intValue(), 20)))
        .thenReturn(List.of(BOUNDARY_1, boundary2));
    when(lineRepository.findAllByOraclePolygonBoundaryIdIn(List.of(BOUNDARY_1_SID.intValue(), 200)))
        .thenReturn(List.of(LINE_ORDER_1, lineBoundary2));

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(SHAPE_1_SI_ID));

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
        .withLineSidId(1003)
        .withOraclePolygonBoundaryId(BOUNDARY_1_SID.intValue())
        .withConnectionOrder(3L)
        .build();

    when(shapeRepository.findAllById(List.of(SHAPE_1_SI_ID))).thenReturn(List.of(SHAPE_1));
    when(polygonRepository.findAllByOracleShapeIdIn(List.of(SHAPE_1_SI_ID))).thenReturn(List.of(POLYGON_1));
    when(boundaryRepository.findAllByOracleShapePolygonIdIn(List.of(POLYGON_1_SID.intValue())))
        .thenReturn(List.of(BOUNDARY_1));
    when(lineRepository.findAllByOraclePolygonBoundaryIdIn(List.of(BOUNDARY_1_SID.intValue())))
        .thenReturn(List.of(lineOrder3, LINE_ORDER_1, LINE_ORDER_2));

    var result = oracleService.getEntityBackedOracleShapesByIdsIn(List.of(SHAPE_1_SI_ID));

    var expected = List.of(new EntityBackedOracleShape(
        SHAPE_1,
        Map.of(POLYGON_1, List.of(BOUNDARY_1)),
        Map.of(BOUNDARY_1, List.of(LINE_ORDER_1, LINE_ORDER_2, lineOrder3))
    ));
    assertThat(result).usingRecursiveComparison().isEqualTo(expected);
  }

  @Test
  void getLinkedParentShapeSiId_whenShapeLinkExists_thenReturnsParentShapeId() {
    var childShapeSiId = 10;
    var parentShapeSiId = 20;
    var shapeLink = OracleShapeLinkTestUtil.newBuilder()
        .withChildShapeId(childShapeSiId)
        .withParentShapeId(parentShapeSiId)
        .build();

    when(shapeLinkRepository.findByChildShapeId(childShapeSiId)).thenReturn(Optional.of(shapeLink));

    var result = oracleService.getLinkedParentShapeSiId(childShapeSiId);

    assertThat(result).contains(parentShapeSiId);
  }

  @Test
  void getLinkedParentShapeSiId_whenShapeLinkDoesNotExist_thenReturnsEmptyOptional() {
    var childShapeSiId = 10;

    when(shapeLinkRepository.findByChildShapeId(childShapeSiId)).thenReturn(Optional.empty());

    var result = oracleService.getLinkedParentShapeSiId(childShapeSiId);

    assertThat(result).isEmpty();
  }

  @Test
  void getAttributeMapForSiIdAndLevel() {
    var sidId = 10;
    var attributeLevel = AttributeLevel.SHAPE;
    var firstAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(sidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName("FIRST_ATTRIBUTE")
        .withAttributeValue("First value")
        .build();
    var secondAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(sidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName("SECOND_ATTRIBUTE")
        .withAttributeValue("Second value")
        .build();
    var nullNameAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(sidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName(null)
        .withAttributeValue("Null name value")
        .build();
    var nullValueAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(sidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName("NULL_VALUE_ATTRIBUTE")
        .withAttributeValue(null)
        .build();

    when(attributeRepository.findAllByAssociatedSiIdAndAttributeLevel(sidId, attributeLevel))
        .thenReturn(List.of(firstAttribute, secondAttribute, nullNameAttribute, nullValueAttribute));

    var result = oracleService.getAttributeMapForSiIdAndLevel(sidId, attributeLevel);

    assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
        "FIRST_ATTRIBUTE", "First value",
        "SECOND_ATTRIBUTE", "Second value"
    ));
  }

  @Test
  void getIdToAttributeMapForSiIdAndLevel() {
    var firstSidId = 10;
    var secondSidId = 20;
    var attributeLevel = AttributeLevel.BOUNDARY_LINE;
    var firstAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(firstSidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName("FIRST_ATTRIBUTE")
        .withAttributeValue("First value")
        .build();
    var secondAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(firstSidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName("SECOND_ATTRIBUTE")
        .withAttributeValue("Second value")
        .build();
    var thirdAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(secondSidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName("THIRD_ATTRIBUTE")
        .withAttributeValue("Third value")
        .build();
    var nullNameAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(firstSidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName(null)
        .withAttributeValue("Null name value")
        .build();
    var nullValueAttribute = OracleAttributeTestUtil.newBuilder()
        .withAssociatedSiId(secondSidId)
        .withAttributeLevel(attributeLevel)
        .withAttributeName("NULL_VALUE_ATTRIBUTE")
        .withAttributeValue(null)
        .build();

    var sidIds = List.of(firstSidId, secondSidId);

    when(attributeRepository.findAllByAssociatedSiIdInAndAttributeLevel(sidIds, attributeLevel))
        .thenReturn(List.of(firstAttribute, secondAttribute, thirdAttribute, nullNameAttribute, nullValueAttribute));

    var result = oracleService.getIdToAttributeMapForSiIdAndLevel(sidIds, attributeLevel);

    assertThat(result).containsExactlyInAnyOrderEntriesOf(Map.of(
        firstSidId, Map.of(
            "FIRST_ATTRIBUTE", "First value",
            "SECOND_ATTRIBUTE", "Second value"
        ),
        secondSidId, Map.of(
            "THIRD_ATTRIBUTE", "Third value"
        )
    ));
  }
}
