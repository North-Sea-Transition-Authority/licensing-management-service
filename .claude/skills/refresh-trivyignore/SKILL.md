---
name: refresh-trivyignore
description: >
  Rebuild .trivyignore from a fresh local Trivy scan of the Docker image, for when the
  trivy-scan CI step is failing on new CVEs.
disable-model-invocation: true
---

# Refresh .trivyignore

The `trivy-scan` step in `.drone.yml` builds the Docker image and fails on any unignored
vulnerability. This skill replaces `.trivyignore` with exactly what a scan reports today, so
fixed CVEs drop off and new ones are added. It suppresses findings only. Do not bump
dependency versions as part of this: Spring Boot manages most of them, and overriding them
is a separate decision.

Run the steps in Git Bash from the repo root. Use a scratch directory, not the repo, for the
Trivy cache and output. `$S` below stands for that directory.

## 1. Clear the ignore file and build the image

```bash
: > .trivyignore
./gradlew bootJar -q
docker build -q -t lms:trivy-scan-target .
```

## 2. Scan with the CI's Trivy version

Read the Trivy image tag from the `trivy-scan` step in `.drone.yml` (e.g.
`aquasec/trivy:0.58.0`) so local results match CI. Also take the DB repositories from there.

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
  -v //var/run/docker.sock:/var/run/docker.sock \
  -v "$S/trivy_cache:/cache" -v "$S:/out" \
  -e TRIVY_DB_REPOSITORY=public.ecr.aws/aquasecurity/trivy-db,aquasec/trivy-db,ghcr.io/aquasecurity/trivy-db \
  -e TRIVY_JAVA_DB_REPOSITORY=public.ecr.aws/aquasecurity/trivy-java-db,aquasec/trivy-java-db,ghcr.io/aquasecurity/trivy-java-db \
  aquasec/trivy:<version> --cache-dir /cache image --timeout 30m \
  --format json --output /out/trivy.json lms:trivy-scan-target
```

`MSYS_NO_PATHCONV=1` stops Git Bash from rewriting the container paths.

## 3. Write the new ignore file

```bash
node .claude/skills/refresh-trivyignore/group-trivy-findings.js "$S/trivy.json" > .trivyignore
```

The script prints each finding to stderr (package, installed version, ID, severity, fixed
version). Keep that output for the summary. Stdout is the ignore file: a `# group:artifact`
header per package, sorted, with its IDs underneath. An ID reported against several packages
is listed once, under the first package.

## 4. Verify

Re-run the scan against the new file. It must report `Total: 0` and exit 0:

```bash
MSYS_NO_PATHCONV=1 docker run --rm \
  -v //var/run/docker.sock:/var/run/docker.sock \
  -v "$S/trivy_cache:/cache" -v "$(pwd -W)/.trivyignore:/.trivyignore" -w / \
  aquasec/trivy:<version> --cache-dir /cache image \
  --skip-db-update --skip-java-db-update --exit-code 1 lms:trivy-scan-target
```

## 5. Report

Show `git diff .trivyignore` and summarise:
- the IDs added, with package, severity and fixed version
- the IDs removed, which are now fixed
- any CRITICAL findings, and whether a fixed version is available

Don't commit unless asked.
