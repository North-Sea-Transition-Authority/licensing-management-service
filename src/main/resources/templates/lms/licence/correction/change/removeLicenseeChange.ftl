<#include '../../../layout/layout.ftl'>
<#import '../../../licence/position/_positionChanges.ftl' as positionChanges>

<@defaultPage
  htmlTitle=pageTitle
  pageHeading=pageTitle
  pageSize=PageSize.TWO_THIRDS_COLUMN
>
    <@fdsForm.htmlForm>

      <#if correctedLicenseeChangeView??>
        <h3 class="govuk-heading-s govuk-!-margin-bottom-0">Current (corrected)</h3>
        <@positionChanges.licenseeChange change=correctedLicenseeChangeView/>

        <#if restoredLicenseeChangeView??>
          <h3 class="govuk-heading-s govuk-!-margin-bottom-0">Will be restored to</h3>
          <@positionChanges.licenseeChange change=restoredLicenseeChangeView/>
        </#if>
      <#else>
        <h3 class="govuk-heading-s govuk-!-margin-bottom-0">To be reinstated</h3>
        <@positionChanges.licenseeChange change=restoredLicenseeChangeView/>
      </#if>

      <#if resultingLicensees?has_content>
        <h3 class="govuk-heading-s govuk-!-margin-bottom-0">Resulting licensees</h3>
        <ul class="govuk-list">
          <#list resultingLicensees as licensee>
            <li>${licensee}</li>
          </#list>
        </ul>
      <#else>
        <@fdsInsetText.insetText insetTextClass="govuk-inset-text--red">
          Removing this change will result in no licensees on the licence
        </@fdsInsetText.insetText>
      </#if>


      <@fdsAction.submitButtons
        primaryButtonText=primaryButton.buttonText()
        primaryButtonClass=primaryButton.buttonClass()
        secondaryLinkText="Cancel"
        linkSecondaryAction=true
        linkSecondaryActionUrl=springUrl(cancelUrl)
      />
    </@fdsForm.htmlForm>
</@defaultPage>