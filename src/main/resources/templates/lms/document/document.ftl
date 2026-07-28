<#-- @ftlvariable name="isPreview" type="boolean" -->
<#-- @ftlvariable name="applicationReference" type="java.lang.String" -->
<#-- @ftlvariable name="documentTemplateSectionSummaryView" type="java.util.List" -->
<#-- @ftlvariable name="documentInstanceSectionSummaryView" type="uk.co.fivium.digitaldocumentlibrary.document.DocumentInstanceSectionsSummaryView" -->

<#if documentTemplateSectionSummaryView?has_content>
  <#assign documentSectionView = documentTemplateSectionSummaryView/>
<#elseif documentInstanceSectionSummaryView?has_content>
  <#assign documentSectionView = documentInstanceSectionSummaryView/>
</#if>

<html>
<head>
  <link rel="stylesheet" href="classpath:///document-assets/all.css"/>
  <style>
    @page {
      @footnote {
        width: 100%;
        border-top: 1px solid black;
        padding-top: 15px;
        orphans: 0;
        widows: 0;
      }
    }
    .footnote {
      float: footnote;
      text-align: left;
      font-weight: normal;
      font-style: normal;
    }
    ::footnote-call {
      counter-increment: footnote 1;
      content: counter(footnote);
      vertical-align: super;
      font-size: smaller;
    }
    ::footnote-marker {
      content: counter(footnote) ". ";
    }
  </style>
</head>
<body>
<table style="border: none">
  <tbody>
  <tr>
    <td>
      <img src="classpath:///document-assets/NSTA_LOGO.png" alt="" style="max-height: 75px; float: left;"/>
    </td>
  </tr>
  <tr>
    <td style="width: 105mm; vertical-align: top; padding-top: 20px;">
      ${companyName!""}
      <br/>
      <#list companyRegisteredAddress as addressLine>
        ${addressLine!""}
        <br/>
      </#list>
      <#if companyRegisteredNumber?has_content>
        Registered No.:${companyRegisteredNumber}
      </#if>
      <br/>
      <br/>
      <br/>
      Date: ${currentDate!""}
      <br/>
    </td>
  </tr>
  </tbody>
</table>
<#if isPreview>
  <div class="watermark">
    PREVIEW DOCUMENT
  </div>
</#if>
<table class="footer">
  <tbody>
  <tr>
    <td class="page-number"></td>
  </tr>
  </tbody>
</table>
<#list documentSectionView![] as documentSectionSummaryView>
  <@sectionContentTable documentSectionSummaryView=documentSectionSummaryView/>
</#list>
</body>
</html>

<#macro sectionContentTable documentSectionSummaryView>
  <#assign sectionNumber = documentSectionSummaryView.sectionNumber()!>
  <#assign hasPageBreakBefore = documentSectionSummaryView.hasPageBreakBefore()>
  <#assign content = documentSectionSummaryView.content()!>
  <#assign children = documentSectionSummaryView.children()>

  <#if hasPageBreakBefore>
    <div style="page-break-after: always;"></div>
  </#if>

  <table style="width: 100%;">
    <tbody>
    <tr>
      <td style="vertical-align: top;">
        <#if sectionNumber?has_content>
          ${sectionNumber}
        </#if>
      </td>
      <td style="vertical-align: top; width: 100%;">
        ${content?no_esc}
      </td>
    </tr>
    <tr>
      <td></td>
      <td>
        <#list children as child>
          <@sectionContentTable documentSectionSummaryView=child/>
        </#list>
      </td>
    </tr>
    </tbody>
  </table>
</#macro>