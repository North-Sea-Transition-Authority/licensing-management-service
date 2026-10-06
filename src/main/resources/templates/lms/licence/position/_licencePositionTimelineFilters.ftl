<#include '../../layout/layout.ftl'>

<#macro filters filterOptions filterUrl clearFilterUrl>
  <#assign changeTypeOptions=filterOptions.changeTypeOptions()>
  <#assign organisationOptions=filterOptions.organisationOptions()>
  <#if changeTypeOptions?has_content || organisationOptions?has_content>
    <@fdsForm.htmlForm actionUrl=springUrl(filterUrl)>
      <#if changeTypeOptions?has_content>
        <@fdsCheckbox.checkboxes
          path="form.changeTypes"
          checkboxes=changeTypeOptions
          fieldsetHeadingText="Type of change"
          fieldsetHeadingSize="h2"
          smallCheckboxes=true
          inline=true
        />
      </#if>
      <#if organisationOptions?has_content>
        <@fdsSearchSelector.searchSelectorEnhanced
          path="form.organisationIds"
          options=organisationOptions
          labelText="Organisation"
          multiSelect=true
          labelHeadingClass="govuk-label--m"
          inputClass="govuk-!-width-two-thirds"
        />
      </#if>
      <@fdsAction.buttonGroup>
        <@fdsAction.button buttonText="Apply filters" buttonClass="govuk-button govuk-button--secondary"/>
        <@fdsAction.link linkText="Clear filters" linkUrl=springUrl(clearFilterUrl) linkClass="govuk-link"/>
      </@fdsAction.buttonGroup>
    </@fdsForm.htmlForm>
  </#if>
</#macro>
