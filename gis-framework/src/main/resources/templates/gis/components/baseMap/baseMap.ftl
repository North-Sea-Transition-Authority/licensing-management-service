<#import "/spring.ftl" as spring>

<#macro baseMap featureId srsWkid includeNstaQuadrants=true includeNstaBlocks=true includeSnapPoints=true>
  <div
      data-gis-component="gis-base-map"
      data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
      data-gis-include-nsta-blocks="${includeNstaBlocks?c}"
      data-gis-include-snap-points="${includeSnapPoints?c}"
      data-gis-features-url="<@spring.url '/api/gis-framework/feature/${featureId}'/>"
      data-gis-srs-wkid="${srsWkid}"
   >
  </div>
</#macro>
