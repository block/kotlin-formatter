#!/usr/bin/env bash

set -euo pipefail

echo_info() {
    echo -e "\033[1;34m$1\033[0m"
}

echo_success() {
    echo -e "\033[1;32m$1\033[0m"
}

echo_error() {
    echo -e "\033[1;31m$1\033[0m"
}

format_kotlin_staged_files() {
  local staged_kotlin_files
  declare -a staged_modules

  staged_kotlin_files=$(git diff --cached --name-only --diff-filter=ACMR | grep '\.kt$' || true)
  while IFS= read -r module; do
    staged_modules+=("$module")
  done < <(echo "$staged_kotlin_files" | cut -d'/' -f1 | sort | uniq)

  if [[ -z "${staged_modules:-}" ]]; then
    # Nothing to format
    return 0
  fi

  echo_info "⌛️ Formatting staged Kotlin changes..."

  echo ""
  set +eo pipefail
  "${KOTLIN_FORMATTER_EXE:-bin/kotlin-formatter}" --pre-commit "${staged_modules[@]}" | sed 's/^/- /'
  formatting_exit_status=${PIPESTATUS[0]}
  set -eo pipefail
  echo ""

  if [ "$formatting_exit_status" -ne 0 ]; then
    echo_error "⚠️  Formatting completed with issues (exit_code: $formatting_exit_status)"
    return "$formatting_exit_status"
  else
    echo_success "🎉 Formatting completed successfully"
  fi
}

format_kotlin_staged_files
