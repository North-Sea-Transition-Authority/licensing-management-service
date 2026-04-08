<#include '../../layout/layout.ftl'>
<#import '../../../fds/components/button/button.ftl' as fdsAction>

<#-- @ftlvariable name="externalUrlValues" type="java.util.Collection<uk.co.nstauthority.licensingmanagementservice.summary.ExternalUrlView>" -->

<#macro externalUrlValueDisplay externalUrlValues>
  <#if externalUrlValues?size gt 1>
    <ul class="govuk-list">
      <#list externalUrlValues as externalUrl>
        <li class="govuk-!-margin-top-2">
          <@fdsAction.link linkText=externalUrl.linkText() linkUrl=externalUrl.url() openInNewTab=true/>
        </li>
      </#list>
    </ul>
  <#elseif externalUrlValues?size == 1>
    <@fdsAction.link linkText=externalUrlValues[0].linkText() linkUrl=externalUrlValues[0].url() openInNewTab=true/>
  </#if>
</#macro>

