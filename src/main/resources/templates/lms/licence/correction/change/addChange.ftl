<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  caption=pageCaption
  backLinkUrl=springUrl(backLinkUrl)
>
  <@fdsForm.htmlForm>
    <@fdsRadio.radioGroup
      path="form.changeType"
      labelText="What type of change do you want to add?"
      hiddenContent=true
    >
      <#list changeTypeOptions as changeType, changeTypeDisplayName>
        <@fdsRadio.radioItem
          path="form.changeType"
          itemMap={changeType: changeTypeDisplayName}
          isFirstItem=changeType?is_first
        >
          <#if changeType == subareaSetEquityChangeType>
            <@fdsSearchSelector.searchSelectorEnhanced
              path="form.subarea"
              options=subareaOptions
              labelText="Which subarea do you want to set equity for?"
              labelClass="govuk-label--s"
              nestingPath="form.changeType"
            />
          </#if>
        </@fdsRadio.radioItem>
      </#list>
    </@fdsRadio.radioGroup>
    <@fdsAction.submitButtons
      primaryButtonText="Continue"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>