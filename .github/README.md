# GitHub Configuration

This directory contains GitHub-specific configuration files for the mog repository.

## Files

### CODEOWNERS
Defines code ownership for automatic review requests. Any changes to the repository require approval from the designated code owners (`@CopperForge/admins`).

### SECURITY.md
Documents the security policy, branch protection requirements, and security best practices for contributors.

### settings.yml
Documents recommended repository settings and branch protection rules. These settings should be applied through the GitHub UI or API (Settings → Branches).

### workflows/
Contains GitHub Actions workflow definitions:
- **ci.yml**: Continuous integration workflow that runs on all branches
- **gradle.yml**: Main build workflow with dependency submission
- **gradle-publish.yml**: Package publishing workflow
- **release.yml**: Release creation workflow (triggered by tags)

## Security Configuration

### Branch Protection Setup

To enable branch protection for the `main` branch:

1. Go to **Settings → Branches** in the GitHub UI
2. Click **Add branch protection rule**
3. Enter `main` as the branch name pattern
4. Follow the configuration documented in `SECURITY.md`

Key protections:
- ✅ Require pull request reviews (1 approval)
- ✅ Require code owner review
- ✅ Require status checks to pass
- ✅ Prevent force pushes
- ✅ Prevent deletion
- ✅ Include administrators

### Why These Files?

While GitHub doesn't automatically apply settings from YAML files, documenting them in the repository provides:

1. **Transparency**: Team members can see what protections should be in place
2. **Consistency**: Makes it easy to replicate settings across repositories
3. **Version Control**: Changes to security policy are tracked in git history
4. **Onboarding**: New administrators know exactly how to configure the repository
5. **Audit Trail**: Security requirements are clearly documented

### Applying Settings

**Option 1: GitHub UI (Recommended)**
- Navigate to repository Settings
- Apply branch protection rules as documented
- Configure team access under Settings → Collaborators and teams

**Option 2: GitHub API/CLI**
- Use the settings.yml as reference
- Apply via GitHub REST API or `gh` CLI tool
- Useful for automating multi-repository setup

**Option 3: Terraform/Infrastructure as Code**
- Export settings.yml to Terraform configuration
- Manage repository settings alongside infrastructure

## Testing Branch Protection

To verify branch protection is working:

1. Attempt to push directly to main:
   ```bash
   git push origin main
   ```
   Should be rejected with: "Required status check(s) are not passing"

2. Create a pull request without approval:
   - Should not be mergeable until reviewed by code owner

3. Try to force push:
   ```bash
   git push --force origin main
   ```
   Should be rejected

## Maintenance

Review and update these configurations:
- **Quarterly**: Review CODEOWNERS for team changes
- **After major releases**: Update workflow security
- **When adding sensitive paths**: Update CODEOWNERS patterns
- **Security incidents**: Document lessons learned in SECURITY.md
