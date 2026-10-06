<#import "/spring.ftl" as spring>

<#macro depthMapPage commandJourneyId srsWkid includeNstaQuadrants=true includeNstaBlocks=true id="" error="">
  <div
    data-gis-component="gis-depth-map"
    data-gis-command-journey-id="${commandJourneyId}"
    data-gis-srs-wkid="${srsWkid?c}"
    data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
    data-gis-include-nsta-blocks="${includeNstaBlocks?c}"
    data-gis-features-base-url="<@spring.url '/api/gis-framework/command-journey-features'/>"
    data-gis-outline-nodes-base-url="<@spring.url '/api/gis-framework/command-journey-outline-nodes'/>"
    data-gis-textual-description-url="<@spring.url '/api/gis-framework/command-journey-textual-description'/>"
  ></div>
</#macro>
