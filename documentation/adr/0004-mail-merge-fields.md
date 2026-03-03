# Mail Merge Fields for LMS Documents

## Context

LMS generates various decision letters for licence applications and work programme amendments. This includes continuation letters and Extension and Amendment Requests (EAR) decision letters. Over time, as new application types are built, additional letter types will be added.

This ADR documents:
1. An inventory of known mail merge fields for the initial letter types
2. How these fields map to underlying LMS data sources

This ADR will not be kept in sync with every future field addition, but provides the foundation and pattern for new fields as they are needed.

---

## Common (System) Mail Merge Fields

These fields are shared across all or most LMS templates. All are already implemented.

| Mnemonic                     | Description                                          | Data Source                                                     |
|------------------------------|------------------------------------------------------|-----------------------------------------------------------------|
| `CURRENT_DATE`               | Today's date (when document is generated)            | `Clock`                                                         |
| `COMPANY_NAME`               | The name of the company the letter is being sent to  | EPA `OrganisationUnitQueryService` via `DocumentLinkingService` |
| `COMPANY_REGISTERED_ADDRESS` | The company registered number (if a UK company)      | EPA `OrganisationUnitQueryService` via `DocumentLinkingService` |
| `REGULATOR_SIGNATORY_NAME`   | The name of the person signing the document          | `EnergyPortalUserJson.displayName() `                           |
| `DIGITAL_SIGNATURE`          | Digital signature placeholder                        | This will come from FTSS                                        |
| `REGULATOR_NAME`             | The full name of the department                      | Use/extend property in `CustomerConfigurationProperties`        |
| `REGULATOR_ADDRESS`          | The address of the regulator team                    | Use/extend property in `CustomerConfigurationProperties`        |
| `REGULATOR_EMAIL`            | The email address of the logged in regulator user    | `EnergyPortalUserJson.telephoneNumber()`                        |
| `REGULATOR_TELEPHONE`        | The telephone number of the logged in regulator user | `EnergyPortalUserJson.emailAddress()`                           |

The current offline letters have the job title of the signatory listed below their name. However, this information is not yet available through EPA. Therefore, it may need to be added to EPA.

---

## Continuation Letter Fields

**Template:** `continuationLetter.ftl` (applies to `CONTINUATION_LETTER` template type)

**Template Type Enum Value:** (to be determined—likely `CONTINUATION_LETTER`)

| Mnemonic                          | Description                                            | Data Source                                   |
|-----------------------------------|--------------------------------------------------------|-----------------------------------------------|
| `LICENCE_NUMBER`                  | The licence reference (e.g. "P1234")                   | Licence data                                  |
| `CURRENT_TERM_NAME`               | Name of the term ending (e.g. "Initial")               | Continuation application data                 |
| `START_DATE_OF_SUBSEQUENT_TERM`   | Date the subsequent term begins                        | Calculated from Continuation application data |
| `SUBSEQUENT_TERM_NAME`            | Name of the new term (e.g. "Second")                   | Continuation application data                 |

The following area(s) of the continuation letter will need manual editing:
* Heading of the contract clause permitting continuation

---

## Extension and Amendment Requests (EAR) Decision Letter Fields

### Common Fields (All EAR Letters)

| Mnemonic         | Description            | Data Source          |
|------------------|------------------------|----------------------|
| `REQUEST_DATE`   | Date of the request    | EAR application data |
| `REQUEST_REASON` | Reason for the request | EAR application data |

### To be determined
Unlike continuation letters, most of the EAR letter mail merge fields are still yet to be determined. The following elements will need to determined:
- Original work programme text
- Amended/approved work programme text
- What to display now that extensions are captured digitally
  - How to handle multiple extensions
- How to handle extensions and amendments in one request
- Fee amounts
- Response deadline date

---

## Conditions Identified

The following *possible* conditions have been identified (but note that the final conditions are still to be determined):

- **Licence type** — Onshore vs. Offshore
- **Licence type group** — Production vs. Carbon Storage
- **Decision outcome** — Approved vs. Declined
- **Application type** — Extension Amendment Request, Continuation request
- **EAR Request purpose** — Extension, Amendment or Both?

---
