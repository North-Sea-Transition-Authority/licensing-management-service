<#import "/spring.ftl" as spring>

<#--Load the assets for the GIS framework. This includes the CSS and JavaScript files needed to render the maps and related components.-->
<#--This should be included on the layout.ftl of the consuming app-->

<link rel="stylesheet" href="<@spring.url '/gis/dist/gis-framework.css'/>">
<#--  We use defer to load the gis-boundle after the document has been parsed to avoid maps rendering with 0 height-->
<#--  https://developer.mozilla.org/en-US/docs/Web/HTML/Reference/Elements/script#defer-->
<script defer type="module" src="<@spring.url '/gis/dist/gis-bundle.js'/>"></script>