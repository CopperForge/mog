# Security Policy

## Branch Protection

This repository implements security controls to protect the `main` branch and ensure code quality.

### Main Branch Protection Rules

To prevent unauthorized commits to the main branch, the following protection rules should be enforced:

#### Required Settings (via GitHub UI)

Navigate to: **Settings → Branches → Add branch protection rule**

1. **Branch name pattern**: `main`

2. **Protect matching branches**:
   - ✅ Require a pull request before merging
     - ✅ Require approvals: **1**
     - ✅ Dismiss stale pull request approvals when new commits are pushed
     - ✅ Require review from Code Owners
   
   - ✅ Require status checks to pass before merging
     - ✅ Require branches to be up to date before merging
     - Required status checks:
       - `Build and Test (Java 21) (ubuntu-latest)`
       - `Build and Test (Java 21) (windows-latest)`
       - `build`
   
   - ✅ Require conversation resolution before merging
   
   - ✅ Require signed commits (optional, recommended for high-security)
   
   - ✅ Require linear history (optional, prevents merge commits)
   
   - ✅ Include administrators (enforces rules for admins too)
   
   - ✅ Restrict who can push to matching branches
     - Add teams/users who can push: `admins` team
   
   - ✅ Do not allow bypassing the above settings
   
   - ❌ Allow force pushes: **Never**
   
   - ❌ Allow deletions: **Never**

### Code Owners

The `.github/CODEOWNERS` file defines individuals and teams responsible for reviewing changes:

- **Default**: All changes require review from `@CopperForge/admins`
- **Workflows**: Changes to `.github/workflows/` require admin review
- **Build config**: Changes to build files require admin review
- **Security-sensitive paths**: Changes to `/etc/` and `/src/install/` require admin review

### Repository Rulesets (Alternative to Branch Protection)

GitHub Rulesets provide more flexible and powerful branch protection. To use rulesets instead:

1. Navigate to: **Settings → Rules → Rulesets**
2. Create a new ruleset targeting the `main` branch
3. Apply the same protections as documented in `.github/settings.yml`

### Workflow Security

All GitHub Actions workflows follow these security principles:

1. **Principle of Least Privilege**: Workflows have minimal required permissions
2. **No Auto-Commits**: No workflow automatically commits to main
3. **Validated Actions**: All third-party actions are pinned to specific versions
4. **Secrets Management**: Secrets are never logged or exposed

### Reporting Security Issues

If you discover a security vulnerability, please:

1. **Do NOT** open a public issue
2. Email the repository maintainers directly
3. Include:
   - Description of the vulnerability
   - Steps to reproduce
   - Potential impact
   - Suggested remediation (if any)

### Security Best Practices for Contributors

1. **Never commit secrets**: No API keys, passwords, or tokens in code
2. **Review dependencies**: Check for known vulnerabilities before adding dependencies
3. **Sign commits**: Use GPG to sign your commits (recommended)
4. **Keep dependencies updated**: Regularly update to patched versions
5. **Follow secure coding practices**: Input validation, output encoding, etc.

### Compliance Checklist

Before merging to main, ensure:

- [ ] Code has been reviewed by at least one code owner
- [ ] All CI checks pass (build, tests)
- [ ] No new security vulnerabilities introduced
- [ ] Documentation is updated if needed
- [ ] Commit messages are clear and descriptive
- [ ] No sensitive data committed
- [ ] Dependencies are up to date and free of known vulnerabilities

## Additional Resources

- [GitHub Branch Protection Documentation](https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches/about-protected-branches)
- [GitHub Code Owners](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners)
- [GitHub Security Best Practices](https://docs.github.com/en/code-security/getting-started/securing-your-repository)
