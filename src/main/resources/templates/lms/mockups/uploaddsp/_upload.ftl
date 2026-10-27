<#-- fileUpload rebinds spring status to its own path, so restore the parent radio's binding afterwards -->
<#macro dspFileUpload attributes legend nestingPath="">
  <@fdsFieldset.fieldset legendHeading=legend legendHeadingClass="govuk-fieldset__legend--s">
    <@fdsFileUpload.fileUpload
      path=attributes.path()
      allowedExtensions=attributes.allowedExtensions()
      uploadUrl=attributes.uploadUrl()
      downloadUrl=attributes.downloadUrl()
      deleteUrl=attributes.deleteUrl()
      existingFiles=attributes.existingFiles()
      maxAllowedSize=attributes.maxAllowedSize()/>
  </@fdsFieldset.fieldset>
  <#if nestingPath?has_content>
    <@spring.bind nestingPath/>
  </#if>
</#macro>
