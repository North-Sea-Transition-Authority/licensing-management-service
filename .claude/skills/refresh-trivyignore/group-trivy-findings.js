// Converts Trivy JSON output into the .trivyignore format: one "# group:artifact" header per
// package, sorted by package name, followed by its vulnerability IDs. An ID reported against
// several packages is only listed under the first, as the ignore file applies to the whole scan.
//
// Usage: node group-trivy-findings.js <trivy-output.json>
const report = JSON.parse(require('fs').readFileSync(process.argv[2], 'utf8'));

const idsByPackage = {};
for (const result of report.Results || []) {
  for (const vulnerability of result.Vulnerabilities || []) {
    console.error(vulnerability.PkgName, vulnerability.InstalledVersion, vulnerability.VulnerabilityID,
        vulnerability.Severity, 'fixed in:', vulnerability.FixedVersion || 'none');
    const ids = (idsByPackage[vulnerability.PkgName] ||= []);
    if (!ids.includes(vulnerability.VulnerabilityID)) {
      ids.push(vulnerability.VulnerabilityID);
    }
  }
}

const seenIds = new Set();
const sections = [];
for (const packageName of Object.keys(idsByPackage).sort()) {
  const ids = idsByPackage[packageName].filter(id => !seenIds.has(id));
  ids.forEach(id => seenIds.add(id));
  if (ids.length > 0) {
    sections.push(`# ${packageName}\n${ids.join('\n')}`);
  }
}
console.log(sections.join('\n\n'));
