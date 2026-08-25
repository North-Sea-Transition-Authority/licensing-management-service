<#import "/spring.ftl" as spring>

<#macro splitByPointAndClickPage commandJourneyId srsWkid includeNstaQuadrants=true includeNstaBlocks=true id="" error="">
  <div
      <#if id?has_content>id="${id}" tabindex="-1"</#if>
      class="govuk-form-group<#if error?has_content> govuk-form-group--error</#if>"
  >
    <#if error?has_content>
      <p<#if id?has_content> id="${id}-error"</#if> class="govuk-error-message">
        <span class="govuk-visually-hidden">Error:</span> ${error}
      </p>
    </#if>
    <div
        data-gis-component="gis-split-by-point-and-click"
        data-gis-command-journey-id="${commandJourneyId}"
        data-gis-srs-wkid="${srsWkid?c}"
        data-gis-include-nsta-quadrants="${includeNstaQuadrants?c}"
        data-gis-include-nsta-blocks="${includeNstaBlocks?c}"
        data-gis-features-base-url="<@spring.url '/api/gis-framework/command-journey-features'/>"
        data-gis-outline-nodes-base-url="<@spring.url '/api/gis-framework/command-journey-outline-nodes'/>"
        data-gis-split-url="<@spring.url '/api/gis-framework/split'/>"
        data-gis-history-base-url="<@spring.url '/api/gis-framework/split-history'/>"
        data-gis-undo-base-url="<@spring.url '/api/gis-framework/undo'/>"
        data-gis-redo-base-url="<@spring.url '/api/gis-framework/redo'/>"
        data-gis-csrf-header-name="${_csrf.headerName}"
        data-gis-csrf-token="${_csrf.token}"
     >
    </div>
  </div>
</#macro>
