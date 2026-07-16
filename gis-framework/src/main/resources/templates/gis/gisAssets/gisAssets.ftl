<#import "/spring.ftl" as spring>

<#--Load the assets for the GIS framework. This includes the CSS and JavaScript files needed to render the maps and related components.-->
<#--This should be included on the layout.ftl of the consuming app-->

<#if (gisViteDevServerUrl!"")?has_content>
  <#-- Local Vite dev server (HMR). -->
  <#-- Vite injects component CSS via JS in dev, so no separate stylesheet link is needed here. -->
  <script type="module" src="${gisViteDevServerUrl}/@vite/client"></script>
  <script type="module" src="${gisViteDevServerUrl}/src/main/resources/js/gis-all.ts"></script>
<#else>
  <link rel="stylesheet" href="<@spring.url '/gis/dist/gis-framework.css'/>">
  <#--  We use defer to load the gis-bundle after the document has been parsed to avoid maps rendering with 0 height-->
  <#--  https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/script#defer-->
  <script defer type="module" src="<@spring.url '/gis/dist/gis-bundle.js'/>"></script>
</#if>
