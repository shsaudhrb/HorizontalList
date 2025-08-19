#!/bin/bash

# Code Quality Check Script
# This script runs all code quality checks that are also run in the pre-push hook

set -e

echo "🔍 Running comprehensive code quality checks..."

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${GREEN}✅ $1${NC}"
}

print_error() {
    echo -e "${RED}❌ $1${NC}"
}

print_warning() {
    echo -e "${YELLOW}⚠️  $1${NC}"
}

print_info() {
    echo -e "${BLUE}ℹ️  $1${NC}"
}

print_header() {
    echo -e "${BLUE}========================================${NC}"
    echo -e "${BLUE}$1${NC}"
    echo -e "${BLUE}========================================${NC}"
}

# Check if we're in the right directory
if [ ! -f "build.gradle.kts" ]; then
    print_error "This script must be run from the project root directory"
    exit 1
fi

# Store the exit code
EXIT_CODE=0

# Function to run a command and check its exit code
run_check() {
    local task_name="$1"
    local command="$2"
    local description="$3"
    
    print_header "Running $description"
    print_info "Command: $command"
    
    if eval "$command"; then
        print_status "$description passed"
    else
        print_error "$description failed"
        EXIT_CODE=1
    fi
    
    echo ""
}

# 1. Run ktlint
run_check "ktlint" "./gradlew ktlintCheck" "ktlint code style check"

# 2. Run detekt
run_check "detekt" "./gradlew detekt" "detekt static code analysis"

# 3. Run lint
run_check "lint" "./gradlew lint" "Android lint checks"

# 4. Check for unused imports using detekt
run_check "unused-imports" "./gradlew detektUnusedImports" "unused imports check"

# 5. Check for unused assets
run_check "unused-assets" "./gradlew checkUnusedAssets" "unused assets check"

# 6. Check for unused files
run_check "unused-files" "./gradlew checkUnusedFiles" "unused files check"

# Final result
print_header "Code Quality Check Summary"

if [ $EXIT_CODE -eq 0 ]; then
    print_status "All code quality checks passed! 🎉"
    echo ""
    print_info "Your code meets all quality standards."
    print_info "You can safely push your changes."
else
    echo ""
    print_error "Code quality checks failed! Please fix the issues above."
    print_warning "You can run individual checks:"
    print_warning "  ./gradlew ktlintCheck    - for code style"
    print_warning "  ./gradlew detekt         - for static analysis"
    print_warning "  ./gradlew lint           - for Android lint"
    print_warning "  ./gradlew checkUnusedAssets - for unused assets"
    print_warning "  ./gradlew checkUnusedFiles  - for unused files"
    echo ""
    print_error "Please fix all issues before pushing your code."
fi

exit $EXIT_CODE 