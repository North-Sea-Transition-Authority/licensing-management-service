<#import "/spring.ftl" as spring>

<#macro baseMap featureId includeNstaQuadrants=true>
  <div
      data-gis-component="gis-base-map"
      data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
      data-gis-features-url="<@spring.url '/api/gis-framework/feature/${featureId}'/>"
   >
  </div>
</#macro>
