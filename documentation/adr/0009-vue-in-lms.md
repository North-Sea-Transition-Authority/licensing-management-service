# Usage of GOV.UK Vue library in LMS

Technical Story: https://fivium.atlassian.net/browse/EPGF-189

## Context and Problem Statement

We want to decide whether we should use the GOV.UK Vue library in LMS / GIS Framework when building our pages.

Previous ADR for this was done this problem
on [EPSCI in 2024](https://github.com/Fivium/energy-pathfinder-supply-chain-interface/blob/31d2fe383a538cc723d32988bc132f477db074df/documentation/adr/0003-frontend-libraries.md)
and we decided against using it, but considered a long enough time has passed to revisit this.

## Considered Options

* Using the GOV.UK Vue library.
* Forking the library / creating our own.
* Copying in components as and when needed, same as EPSCI.

## Decision Outcome

Chosen option: "Copying in components as and when needed, same as EPSCI.", because we only need a small number of components
provided by that library. It is also out of scope of this project to start and maintain a Vue version of FDS.

## Pros and Cons of the Options 
### Option 1: Use the GOVUK Vue frontend library directly

* Good, because it has all the components we need readily available.
* Good, because we know the owner of the project.
* Bad, because there are no tests for it.
* Bad, because Fivium does not own it.
* Bad, because we don't need all the components, so there is a lot of extra bloat.


### Option 2: Forking the library / creating our own.

* Good, because it has all the components we need readily available.
* Good, because Fivium would be in control of the repo
* Bad, because there are no tests for it.
* Bad, because we don't need all the components, so there is a lot of extra bloat.
* Bad, because it's out of scope to essentially create/maintain Vue in FDS for this project.

### Option 3: Copying in components as and when needed, same as EPSCI.

* Good, because this is easy to do.
* Good, because we can only copy across the relatively small number of components we need.
* Bad, because we may at some point want to use Vue components on multiple services, so we'd have to come back and refactor.
