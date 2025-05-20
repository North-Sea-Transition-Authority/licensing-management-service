<#include '../../layout/layout.ftl'>

<#macro stringValueDisplay stringValues>
    <#if stringValues?size gt 1>
      <ol class="govuk-list">
          <#list stringValues as value>
            <li>
                <@multiLineText.multiLineText contentText=value!""/>
            </li>
          </#list>
      </ol>
    <#elseif stringValues?size == 1>
        <@multiLineText.multiLineText contentText=stringValues[0]!""/>
    </#if>
</#macro>
