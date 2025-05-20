<#include '../layout/layout.ftl'>
<#import '../application/_contextHeaderSummaryDataView.ftl' as contextHeaderSummaryDataView>

<#macro resultListItem dataView>
  <#if dataView.tagText()?has_content>
    <#assign tagContent>
      <@fdsResultList.resultListTag tagClass=dataView.tagClass() tagText=dataView.tagText()/>
    </#assign>
  <#else>
    <#assign tagContent=""/>
  </#if>

  <@fdsResultList.resultListItem
    linkHeadingText=dataView.linkHeadingText()!""
    linkHeadingUrl=springUrl(dataView.linkHeadingUrl())
    captionHeadingText=dataView.captionText()!""
    itemTag=tagContent
  >
    <@contextHeaderSummaryDataView.summaryDataView dataView.dataItemRows()/>
  </@fdsResultList.resultListItem>
</#macro>

<#macro selectableResultListItem dataItem path>
  <div style="display: flex; align-items: center;">
      <@spring.bind path/>

      <#local id=fdsUtil.sanitiseId(spring.status.expression)>
      <#local name=fdsUtil.getSpringStatusExpression()>
      <#local hasError=fdsUtil.hasSpringStatusErrors()>

      <#local selectedSearchDataItemIds = spring.stringStatusValue?has_content?then(spring.stringStatusValue?split(","), [])>
      <#local isSelected = selectedSearchDataItemIds?seq_contains("${dataItem.id()}")>

      <#local labelText=isSelected?then("de-select application ${dataItem.linkHeadingText()}", "select application ${dataItem.linkHeadingText()}")>

      <div class="govuk-checkboxes__item">
        <input
          class="govuk-checkboxes__input"
          id="${id}"
          name="${name}"
          type="checkbox"
          value="${dataItem.id()}"
          <#if isSelected>checked</#if>
        />
        <label class="govuk-label govuk-checkboxes__label" for="${id}">
          <span class="govuk-visually-hidden">${labelText}</span>
        </label>
      </div>

      <div class="govuk-!-width-full">
          <@resultListItem dataView=dataItem/>
      </div>
  </div>
</#macro>
