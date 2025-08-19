# Code Quality Checks

This project includes comprehensive code quality checks that run automatically before each push and can also be run manually.

## Pre-Push Hook

The pre-push hook automatically runs the following checks before allowing code to be pushed:

1. **ktlint** - Kotlin code style checking
2. **detekt** - Static code analysis
3. **lint** - Android lint checks
4. **Unused Imports** - Detects unused import statements
5. **Unused Assets** - Finds unused drawable, mipmap, and other resources
6. **Unused Files** - Identifies potentially unused Kotlin/Java files

## Manual Execution

### Run All Checks

To run all code quality checks manually:

```bash
./scripts/check-code-quality.sh
```

### Individual Checks

You can also run individual checks:

```bash
# Code style
./gradlew ktlintCheck

# Static analysis
./gradlew detekt

# Android lint
./gradlew lint

# Unused imports
./gradlew detekt --configuration detekt-unused-imports.yml

# Unused assets
./gradlew checkUnusedAssets

# Unused files
./gradlew checkUnusedFiles
```

## Configuration Files

### detekt.yml
Main detekt configuration file with comprehensive static analysis rules.

### detekt-unused-imports.yml
Specialized detekt configuration focused only on unused imports detection.

### lint.xml
Android lint configuration with security and performance rules.

## What Each Check Does

### ktlint
- Enforces Kotlin coding style conventions
- Checks formatting, naming conventions, and code structure
- Automatically formats code when run with `ktlintFormat`

### detekt
- Performs static code analysis
- Detects potential bugs, code smells, and complexity issues
- Includes rules for naming, complexity, performance, and style

### Android Lint
- Android-specific static analysis
- Checks for security issues, performance problems, and best practices
- Validates resource usage and API compatibility

### Unused Imports
- Detects import statements that are not used in the code
- Helps keep code clean and reduces compilation time
- Uses detekt's UnusedImports rule

### Unused Assets
- Scans for unused drawable, mipmap, and other resource files
- Checks both Kotlin/Java code and XML layouts for resource usage
- Helps reduce APK size by identifying unused resources

### Unused Files
- Identifies potentially unused Kotlin/Java files
- Uses heuristics to determine if files are actually used
- Excludes common patterns like Activities, ViewModels, etc.

## Fixing Issues

### ktlint Issues
```bash
# Auto-fix formatting issues
./gradlew ktlintFormat
```

### detekt Issues
- Review the detekt report in `app/build/reports/detekt/`
- Fix issues according to the suggestions
- Some issues can be suppressed with `@Suppress` annotations

### Lint Issues
- Review the lint report in `app/build/reports/lint-results.html`
- Fix issues according to the suggestions
- Some issues can be suppressed with `@SuppressLint` annotations

### Unused Imports
- Remove unused import statements
- Use IDE's "Optimize Imports" feature (Ctrl+Alt+O / Cmd+Option+O)

### Unused Assets
- Remove unused resource files
- Or use them in your code if they're needed

### Unused Files
- Review the list of potentially unused files
- Remove files that are truly unused
- Be careful with files that might be used dynamically

## Continuous Integration

These checks are designed to be run in CI/CD pipelines. The exit codes are:
- `0` - All checks passed
- `1` - One or more checks failed

## Customization

### Adding New Checks
1. Add the check logic to `app/build.gradle.kts`
2. Update the pre-push hook in `.git/hooks/pre-push`
3. Update the manual script in `scripts/check-code-quality.sh`

### Modifying Rules
- Edit `detekt.yml` for detekt rules
- Edit `lint.xml` for Android lint rules
- Edit `app/build.gradle.kts` for ktlint configuration

## Troubleshooting

### Hook Not Running
Make sure the pre-push hook is executable:
```bash
chmod +x .git/hooks/pre-push
```

### Permission Issues
If you encounter permission issues, ensure the scripts are executable:
```bash
chmod +x scripts/check-code-quality.sh
chmod +x .git/hooks/pre-push
```

### False Positives
Some checks may produce false positives:
- Unused files check uses heuristics and may flag files that are used dynamically
- Unused assets check may miss resources used in themes or styles
- Review flagged items carefully before removing them 