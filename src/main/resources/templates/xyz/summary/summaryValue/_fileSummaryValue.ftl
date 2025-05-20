<#include '../../layout/layout.ftl'>
<#import '../../../fds/objects/layouts/generic.ftl' as fdsGeneric>
<#import '../../../fds/components/button/button.ftl' as fdsButton>

<#macro fileDisplay file>
    <@fdsButton.link
    linkText=file.fileName()
    linkUrl=fdsGeneric.springUrl(file.downloadUrl())
    linkClass="govuk-link govuk-!-font-size-19"
    linkScreenReaderText="Download"
    ariaDescribedBy="${file.fileId()}-description"
    />
  <span> - ${file.fileSize()}</span>
    <#if file.fileDescription()?has_content>
      <p
        id="${file.fileId()}-description"
        class="govuk-body govuk-body__preserve-whitespace govuk-!-margin-bottom-0"
      >
          ${file.fileDescription()}
      </p>
    </#if>
</#macro>

<#macro fileValueDisplay fileValues>
    <#if fileValues?size gt 1>
      <ul class="govuk-list">
          <#list fileValues as file>
            <li class="govuk-!-margin-top-2">
                <@fileDisplay file/>
            </li>
          </#list>
      </ul>
    <#elseif fileValues?size == 1>
      <div class="govuk-body">
          <@fileDisplay fileValues[0]/>
      </div>
    </#if>
</#macro>
