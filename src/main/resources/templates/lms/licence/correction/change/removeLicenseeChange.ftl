<#include '../../../layout/layout.ftl'>
<#import '../../../licence/position/_positionChanges.ftl' as positionChanges>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.TWO_THIRDS_COLUMN
>
    <@fdsForm.htmlForm>

      <h3 class="govuk-heading-s govuk-!-margin-bottom-0">Current (corrected)</h3>
      <@positionChanges.licenseeChange change=correctedLicenseeChangeView/>

      <#if restoredLicenseeChangeView??>
        <h3 class="govuk-heading-s govuk-!-margin-bottom-0">Will be restored to</h3>
        <@positionChanges.licenseeChange change=restoredLicenseeChangeView/>
      </#if>

      <h3 class="govuk-heading-s govuk-!-margin-bottom-0">Resulting licensees</h3>
      <ul class="govuk-list">
        <#list resultingLicensees as licensee>
          <li>${licensee}</li>
        </#list>
      </ul>

      <@fdsAction.submitButtons
        primaryButtonText=primaryButtonText
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(cancelUrl)
        primaryButtonClass="govuk-button govuk-button--warning"
      />
    </@fdsForm.htmlForm>
</@defaultPage>