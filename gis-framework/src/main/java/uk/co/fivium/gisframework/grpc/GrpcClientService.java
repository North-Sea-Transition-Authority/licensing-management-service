package uk.co.fivium.gisframework.grpc;

import java.util.List;
import net.devh.boot.grpc.client.inject.GrpcClient;
import org.springframework.stereotype.Service;
import uk.co.fivium.gisframework.feature.CoordinateSystemUtils;
import uk.co.fivium.grpc.gis.ArcGisServiceGrpc;
import uk.co.fivium.grpc.gis.BuildPolygonRequest;
import uk.co.fivium.grpc.gis.CoordinateSystem;
import uk.co.fivium.grpc.gis.SplitPolygonRequest;

@Service
public class GrpcClientService {

  @GrpcClient("node-server")
  private ArcGisServiceGrpc.ArcGisServiceBlockingStub arcgisClient;

  /**
   * Split a polygon using a cutter line.
   * @param esriJsonPolygon The polygon to split.
   * @param esriJsonCutterLine The cutter line.
   * @return A list of output EsriJSON polygons.
   */
  public List<String> splitPolygon(String esriJsonPolygon,
                                   String esriJsonCutterLine) {
    var request = SplitPolygonRequest.newBuilder()
        .setEsriJsonPolygonTarget(esriJsonPolygon)
        .setEsriJsonLineCutter(esriJsonCutterLine)
        .build();

    var response = arcgisClient.splitPolygon(request);
    return response.getOutputPolygonEsriJsonsList();
  }

  /**
   * Takes a list of polylines EsriJSON and combines them into a polygon using the arcgis js sdk.
   * @param esriJsonPolylines An ordered list of EsriJSON polylines. Must be sorted by their ring connection order.
   * @param coordinateSystem The coordinate system of the polylines. Must be the same for all polylines.
   * @return EsriJSON of the built polygon as a string.
   */
  public String buildPolygon(List<String> esriJsonPolylines,
                             CoordinateSystem coordinateSystem) {
    var request = BuildPolygonRequest.newBuilder()
        .addAllEsriJsonPolylines(esriJsonPolylines)
        .setCoordinateSystemWkid(CoordinateSystemUtils.getWkid(coordinateSystem))
        .build();

    var response = arcgisClient.buildPolygon(request);
    return response.getPolygonEsriJson();
  }
}
