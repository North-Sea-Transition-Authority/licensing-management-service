package uk.co.fivium.gisframework.grpc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.grpc.gis.ArcGisServiceGrpc;
import uk.co.fivium.grpc.gis.BuildPolygonRequest;
import uk.co.fivium.grpc.gis.BuildPolygonResponse;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.SplitPolygonRequest;
import uk.co.fivium.grpc.gis.SplitPolygonResponse;

@ExtendWith(MockitoExtension.class)
class GrpcClientServiceTest {

  @Mock
  private ArcGisServiceGrpc.ArcGisServiceBlockingStub grpcClient;

  @InjectMocks
  private GrpcClientService grpcClientService;

  @Test
  void splitPolygons_verifyServiceClientCall() {
    var esriJsonPolygon = "dummy esriJson polygon";
    var esriJsonCutterLine = "dummy esriJson cutter line";

    var splitResult = "dummy esriJson split result";
    var expectedRequest = SplitPolygonRequest.newBuilder()
        .setEsriJsonPolygonTarget(esriJsonPolygon)
        .setEsriJsonLineCutter(esriJsonCutterLine)
        .build();
    var expectedResponse = SplitPolygonResponse.newBuilder()
        .addOutputPolygonEsriJsons(splitResult)
        .build();

    when(grpcClient.splitPolygon(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService.splitPolygon(esriJsonPolygon, esriJsonCutterLine)).containsExactly(splitResult);
  }

  @Test
  void buildPolygon_verifyServiceClientCall() {
    var esriJsonPolylines = List.of(
        "dummy esriJson polyline 1",
        "dummy esriJson polyline 2"
    );
    var coordinateSystem = CoordinateSystem.ED50;

    var builtPolygon = "dummy esriJson polygon";
    var expectedRequest = BuildPolygonRequest.newBuilder()
        .addAllEsriJsonPolylines(esriJsonPolylines)
        .setCoordinateSystemWkid(CoordinateSystemUtils.getWkid(coordinateSystem))
        .build();
    var expectedResponse = BuildPolygonResponse.newBuilder()
        .setPolygonEsriJson(builtPolygon)
        .build();

    when(grpcClient.buildPolygon(expectedRequest)).thenReturn(expectedResponse);
    assertThat(grpcClientService.buildPolygon(esriJsonPolylines, coordinateSystem)).isEqualTo(builtPolygon);
  }
}