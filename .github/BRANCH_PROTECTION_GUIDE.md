# Branch Protection Implementation Guide

This guide helps you apply the branch protection rules defined in this repository to secure the `main` branch.

## Quick Start

**Goal**: Prevent non-owners and non-administrators from committing directly to the `main` branch.

**Solution**: Require all changes to go through pull requests with code owner review.

---

## Step 1: Create the Admin Team (5 minutes)

If you don't already have an admin team in your GitHub organization:

1. Go to your organization: https://github.com/CopperForge
2. Click **Teams** tab
3. Click **New team**
4. Configure the team:
   - **Team name**: `admins` (or your preferred name)
   - **Description**: "Repository administrators with code review authority"
   - **Visibility**: Private or Visible (your choice)
5. Click **Create team**
6. Add members who should be administrators
7. Give the team **Admin** or **Write** access to the `mog` repository

**Alternative**: If you want to use individual usernames instead of a team, edit `.github/CODEOWNERS` and replace `@CopperForge/admins` with individual usernames like `@user1 @user2`.

---

## Step 2: Apply Branch Protection Rules (10 minutes)

1. Go to the repository: https://github.com/CopperForge/mog
2. Click **Settings** tab (requires admin access)
3. In the left sidebar, click **Branches**
4. Click **Add branch protection rule**
5. Configure the rule:

### Branch name pattern
```
main
```

### Protect matching branches

Check these boxes:

#### ✅ Require a pull request before merging
- **Required approvals**: 1
- ✅ Dismiss stale pull request approvals when new commits are pushed
- ✅ Require review from Code Owners

#### ✅ Require status checks to pass before merging
- ✅ Require branches to be up to date before merging
- In the search box, start typing to find your workflow checks:
  - Look for checks from `.github/workflows/ci.yml`
  - Select the checks that appear after running CI
  - Common pattern: `Build and Test (Java 21) (ubuntu-latest)`

> **Note**: You may need to merge one PR first to see which status checks are available. You can add them later by editing the rule.

#### ✅ Require conversation resolution before merging
(All review comments must be resolved)

#### ✅ Include administrators
(Apply these rules to admins too - recommended)

#### ✅ Restrict who can push to matching branches
- Click the search box under "Restrict who can push to matching branches"
- Select the `admins` team you created in Step 1

#### ❌ Allow force pushes
(Leave unchecked - prevents force pushes)

#### ❌ Allow deletions
(Leave unchecked - prevents branch deletion)

6. Click **Create** or **Save changes**

---

## Step 3: Verify Protection (5 minutes)

Test that the protection is working:

### Test 1: Direct Push (Should Fail)
```bash
# Try to push directly to main
git checkout main
git commit --allow-empty -m "Test commit"
git push origin main
```

**Expected result**: Push is rejected with message about required reviews or status checks.

### Test 2: Pull Request Flow (Should Work)
```bash
# Create a branch and push
git checkout -b test-protection
git commit --allow-empty -m "Test commit"
git push origin test-protection

# Open a PR on GitHub
# The PR should:
# - Request review from code owners
# - Show required status checks
# - Not be mergeable until approved
```

---

## Step 4: Update Status Checks (Optional)

After your first PR is merged:

1. Go to **Settings → Branches**
2. Click **Edit** on the `main` branch rule
3. Scroll to **Require status checks to pass before merging**
4. The search box will now show all available checks
5. Add any missing checks from your CI workflows
6. Click **Save changes**

---

## Troubleshooting

### "Code owners not found" error
- **Cause**: The team `@CopperForge/admins` doesn't exist
- **Fix**: Create the team (Step 1) or update `.github/CODEOWNERS` with individual usernames

### No status checks available
- **Cause**: No workflows have run yet on the main branch
- **Fix**: 
  1. Skip status checks for now
  2. Merge one PR
  3. Come back and add status checks (Step 4)

### Can't enable "Require review from Code Owners"
- **Cause**: CODEOWNERS file references non-existent teams/users
- **Fix**: Verify all teams and users in `.github/CODEOWNERS` exist and have repository access

### Admins can still bypass protection
- **Cause**: "Include administrators" is not checked
- **Fix**: Edit the branch protection rule and check "Include administrators"

### Team doesn't appear in "Restrict who can push"
- **Cause**: Team doesn't have write access to the repository
- **Fix**: 
  1. Go to **Settings → Collaborators and teams**
  2. Add the team with **Write** or **Admin** access

---

## Summary of Protection

Once configured, the `main` branch will:

| Protection | Effect |
|------------|--------|
| ✅ Require PR | No direct commits to main |
| ✅ Require review | At least 1 approval needed |
| ✅ Code owner review | Admin team must approve |
| ✅ Status checks | CI must pass before merge |
| ✅ Restrict push access | Only admin team can push |
| ❌ Force push | Prevented |
| ❌ Branch deletion | Prevented |

---

## Maintenance

### When team membership changes:
- Add/remove members from the `admins` team
- No need to update branch protection rules

### When adding new sensitive paths:
- Update `.github/CODEOWNERS` with new patterns
- Changes take effect immediately

### Quarterly review:
- Review team membership
- Verify status checks are still relevant
- Check if any new sensitive paths need protection

---

## Alternative: Repository Rulesets

GitHub now offers Rulesets as a more flexible alternative to branch protection rules. To use rulesets:

1. Go to **Settings → Rules → Rulesets**
2. Click **New ruleset → New branch ruleset**
3. Apply the same protections documented above
4. Rulesets offer additional features like tag protection and organization-wide policies

See: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-rulesets

---

## Getting Help

- **GitHub Documentation**: https://docs.github.com/en/repositories/configuring-branches-and-merges-in-your-repository/managing-protected-branches
- **Code Owners Guide**: https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-code-owners
- **Organization Teams**: https://docs.github.com/en/organizations/organizing-members-into-teams

---

## Files in This Security Setup

| File | Purpose |
|------|---------|
| `.github/CODEOWNERS` | Defines who must review changes |
| `.github/SECURITY.md` | Security policy and detailed configuration |
| `.github/settings.yml` | Documents recommended settings |
| `.github/BRANCH_PROTECTION_GUIDE.md` | This implementation guide |
| `.github/README.md` | Overview of GitHub configuration |

All settings are documented in code for transparency, version control, and easy replication across repositories.
