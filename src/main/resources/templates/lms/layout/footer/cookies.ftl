<#include '../layout.ftl'>

<#-- @ftlvariable name="serviceBranding" type="uk.co.nstauthority.licensingmanagementservice.branding.ServiceConfigurationProperties" -->

<@defaultPage htmlTitle="Cookies" pageHeading="Cookies">

    <#assign essentialCookies>
        <@fdsCookiePreferences.essentialCookieRow name="SESSION" purpose="Used to keep you signed in" expiry="When you close your browser"/>
    </#assign>

    <@fdsCookiePreferences.cookiePreferences serviceName=serviceBranding.name() essentialCookies=essentialCookies>

      <h2 class="govuk-heading-m">Analytics cookies (optional)</h2>

      <p class="govuk-body">
        With your permission, we use Google Analytics to collect data about how you use the '${serviceBranding.name()}' service.
        This information helps us to improve our service.
      </p>

      <p class="govuk-body">
        Google is not allowed to use or share our analytics data with anyone.
      </p>

      <p class="govuk-body">
        Google Analytics stores anonymised information about:
      </p>

      <ul class="govuk-list govuk-list--bullet">
        <li>how you got to the '${serviceBranding.name()}' service</li>
        <li>the pages you visit on this service and how long you spend on them</li>
      </ul>

      <table class="govuk-table">
        <caption class="govuk-visually-hidden">Google Analytics cookies</caption>
        <thead class="govuk-table__head">
        <tr class="govuk-table__row">
          <th class="govuk-table__header">Name</th>
          <th class="govuk-table__header">Purpose</th>
          <th class="govuk-table__header">Expires</th>
        </tr>
        </thead>
        <tbody class="govuk-table__body">
        <tr class="govuk-table__row">
          <td class="govuk-table__cell">
            _ga
          </td>
          <td class="govuk-table__cell govuk-!-width-one-half">
            Checks if you’ve visited this service before. This helps us count how many people use the service.
          </td>
          <td class="govuk-table__cell">
            2 years
          </td>
        </tr>
        <tr class="govuk-table__row">
          <td class="govuk-table__cell">
            [TODO XYZ GA cookies]
          </td>
          <td class="govuk-table__cell govuk-!-width-one-half">
            Stores information about how you use this service. This helps us improve the service for other people.
          </td>
          <td class="govuk-table__cell">
            2 years
          </td>
        </tr>
        </tbody>
      </table>

    </@fdsCookiePreferences.cookiePreferences>

</@defaultPage>