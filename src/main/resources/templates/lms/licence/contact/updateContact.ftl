<#include '../../layout/layout.ftl'>
<#import '../../macros/_applyToOtherLicences.ftl' as applyToOtherLicencesMacro>

<@defaultPage
  htmlTitle="${isUpdate?then('Update', 'Add')} contact"
  pageHeading="${isUpdate?then('Update', 'Add')} contact for ${licenceReference}"
  pageSize=PageSize.FULL_COLUMN
  extendContainerWidth=true
>
  <@fdsForm.htmlForm>
    <@fdsTextInput.textInput
      path="form.contactEmail"
      labelText="Contact email address"
    />
    <#if otherLicences?has_content>
      <h2 class="govuk-heading-m">Apply to other licences (optional)</h2>
      <p class="govuk-body">
        You are updating the contact for <strong>${licenceReference}</strong>.
        You can also apply this change to other licences held by <strong>${licenseeName}</strong>.
      </p>
      <@applyToOtherLicencesMacro.applyToOtherLicences candidates=otherLicences path="form.bulkUpdateLicenceIds"/>
    </#if>
    <@fdsAction.submitButtons
      primaryButtonText="Save"
      secondaryLinkText="Cancel"
      linkSecondaryAction=true
      linkSecondaryActionUrl=springUrl(backLinkUrl)
    />
  </@fdsForm.htmlForm>
</@defaultPage>
