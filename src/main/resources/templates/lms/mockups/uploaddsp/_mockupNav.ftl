<#-- Mockup-only user switching, shown on every page: who is acting, and links to the other actors -->
<#macro mockupNavigation>
  <#if mockupActors??>
    <@fdsInsetText.insetText>
      Mockup navigation (not part of the design). Current actor: <strong>${mockupActors.current()}</strong>
      <#if mockupActors.others()?has_content>
        <br/>Switch to:
        <#list mockupActors.others() as other>
          <a class="govuk-link" href="${springUrl(other.url())}">${other.label()}</a><#if other?has_next> | </#if>
        </#list>
      </#if>
    </@fdsInsetText.insetText>
  </#if>
</#macro>
