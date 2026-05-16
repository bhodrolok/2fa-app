### Contributor License Agreement (CLA)

All contributors must agree to the [Contributor License Agreement](/CLA). **By submitting a contribution (such as a pull request, patch, issue comment containing code, or any other material), you confirm that you have read, understood, and agree to be bound by these terms.**

If you do not agree to these terms, please do not submit contributions.

### Commit Guidelines

All commits must follow these rules to ensure a clean, traceable, and verifiable project history.

- **One logical change per commit.** A commit should address a single, atomic change (e.g., one bug fix, one feature addition, one refactor). Do not bundle unrelated changes together.
- **Keep commits small and focused.** If your commit message requires an "and" to describe it, it should likely be split into multiple commits.
- **Avoid "mega-commits".** Large, sweeping changes that touch many unrelated files make review difficult and hinder bisecting to identify regressions.
- **All commits must be signed.** Commits must be signed using GPG or SSH. Unsigned commits will not be accepted.

**Commit Message Format**

Commit messages **must** follow the Conventional Commits specification using this format:

```
<type>(<scope>): <short summary>
```

**Allowed types:**

- `feat` - Introduces a new feature
- `fix` - Fixes a bug
- `docs` - Changes to documentation only
- `style` - Code style or formatting changes (no functional impact)
- `refactor` - Code change that neither fixes a bug nor adds a feature
- `test` - Adds or updates tests
- `chore` - Maintenance tasks, tooling, or dependency updates
- `perf` - Performance improvements
- `ci` - Changes to CI configuration
- `build` - Changes to the build system or dependencies

**Examples:**

```
feat(account): add double tap to copy codes
fix(hotp): save account when cycling counter
refactor(passphrase): estimate passphrase strength logic
docs(readme): clarify secure clipboard explanation
chore(rust): update to latest dependencies
```

These guidelines ensure commits are easy to understand, review, and trace over time.

### Kotlin

TODO

### Rust

TODO