<#include '../../layout/layout.ftl'>

<#macro filters changeTypeOptions filterUrl clearFilterUrl>
  <#if changeTypeOptions?has_content>
    <@fdsForm.htmlForm actionUrl=springUrl(filterUrl)>
      <@fdsCheckbox.checkboxes
        path="form.changeTypes"
        checkboxes=changeTypeOptions
        fieldsetHeadingText="Type of change"
        fieldsetHeadingSize="h2"
        smallCheckboxes=true
        inline=true
      />
      <@fdsAction.buttonGroup>
        <@fdsAction.button buttonText="Apply filters" buttonClass="govuk-button govuk-button--secondary"/>
        <@fdsAction.link linkText="Clear filters" linkUrl=springUrl(clearFilterUrl) linkClass="govuk-link"/>
      </@fdsAction.buttonGroup>
    </@fdsForm.htmlForm>
  </#if>
</#macro>
