package uk.co.nstauthority.licensingmanagementservice.migration.carbonstorage;

import java.time.Instant;

record ActivityStatusData(String statusDisplayName, Instant caseInstant) {}
