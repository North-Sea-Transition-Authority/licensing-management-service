<#include '../../../layout/layout.ftl'>

<@defaultPage
  htmlTitle="Add change"
  pageHeading="Add change"
  caption=operatorType.licenceReference
  backLinkUrl=springUrl(backUrl)
>
  <@fdsForm.htmlForm>
    <@fdsRadio.radioGroup
      path="form.changeType"
      labelText="What type of change do you want to add?"
      hiddenContent=true
    >
      <#list changeTypeOptions as changeType, changeTypeDisplayName>
        <#if changeType == operatorChangeType && operatorType.subareaScoped>
          <@fdsRadio.radioItem
            path="form.changeType"
            itemMap={changeType: changeTypeDisplayName}
            isFirstItem=changeType?is_first
          >
            <@fdsSelect.select
              path="form.blockId"
              options=blockOptions
              labelText="Which subarea are you changing operator for?"
              labelClass="govuk-label--s"
              nestingPath="form.changeType"
            />
          </@fdsRadio.radioItem>
        <#else>
          <@fdsRadio.radioItem
            path="form.changeType"
            itemMap={changeType: changeTypeDisplayName}
            isFirstItem=changeType?is_first
          />
        </#if>
      </#list>
    </@fdsRadio.radioGroup>
    <@fdsAction.submitButtons
      primaryButtonText="Continue"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
