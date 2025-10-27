<#include  '../../layout/layout.ftl'>

<#macro actionItems actionItems screenReaderText>
  <#if actionItems?has_content>
    <@fdsAction.buttonGroup>
      <#list actionItems as action>
        <#assign actionClass>
          <#if action.primaryAction()>
            govuk-button
          <#else>
            govuk-button govuk-button--secondary
          </#if>
        </#assign>
        <#if action.screenReaderTextPrefix()?has_content>
          <#assign screenReaderContent="${action.screenReaderTextPrefix()} ${screenReaderText}">
        <#else>
          <#assign screenReaderContent="${screenReaderText}">
        </#if>
        <@fdsForm.htmlForm>
          <@fdsAction.link
            linkUrl=springUrl(action.url())
            linkText=action.displayName()
            linkClass=actionClass
            linkScreenReaderText=screenReaderContent
          />
        </@fdsForm.htmlForm>
      </#list>
    </@fdsAction.buttonGroup>
  </#if>
</#macro>
