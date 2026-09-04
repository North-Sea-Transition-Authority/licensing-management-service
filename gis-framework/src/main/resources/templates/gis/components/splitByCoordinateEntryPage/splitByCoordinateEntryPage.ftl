<#import "/spring.ftl" as spring>

<#macro splitByCoordinateEntryPage commandJourneyId srsWkid precision=4 includeNstaQuadrants=true includeNstaBlocks=true>
  <div
      data-gis-component="gis-split-by-coordinate-entry"
      data-gis-command-journey-id="${commandJourneyId}"
      data-gis-srs-wkid="${srsWkid?c}"
      data-gis-precision="${precision?c}"
      data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
      data-gis-include-nsta-blocks="${includeNstaBlocks?c}"
      data-gis-features-base-url="<@spring.url '/api/gis-framework/command-journey-features'/>"
      data-gis-outline-nodes-base-url="<@spring.url '/api/gis-framework/command-journey-outline-nodes'/>"
      data-gis-split-url="<@spring.url '/api/gis-framework/split'/>"
      data-gis-history-base-url="<@spring.url '/api/gis-framework/split-history'/>"
      data-gis-undo-base-url="<@spring.url '/api/gis-framework/undo'/>"
      data-gis-redo-base-url="<@spring.url '/api/gis-framework/redo'/>"
      data-gis-textual-description-url="<@spring.url '/api/gis-framework/command-journey-textual-description'/>"
      data-gis-csrf-header-name="${_csrf.headerName}"
      data-gis-csrf-token="${_csrf.token}"
   >
  </div>
</#macro>
