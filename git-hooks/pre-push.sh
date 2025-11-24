#!/usr/bin/env bash

set -euo pipefail

remote="$1"
remote_url="$2"

echo_info() {
    echo -e "\033[1;34m$1\033[0m"
}

echo_success() {
    echo -e "\033[1;32m$1\033[0m"
}

echo_error() {
    echo -e "\033[1;31m$1\033[0m"
}

check_pushed_commits_formatting() {
  local remote="$1"
  local push_sha="$2"
  local exe="${KOTLIN_FORMATTER_EXE:-bin/kotlin-formatter}"

  declare -a paths_to_check
  while IFS= read -r filename; do
    paths_to_check+=("$filename")
  done < <(git diff "${remote}/main...$push_sha" --name-only | grep '\.kt$')

  if [[ -z "${paths_to_check:-}" ]]; then
    # No Kotlin files changed
    return 0
  fi

  echo_info "⌛️ Checking Kotlin formatting..."

  # Since we've got a list of individual files which can be quite long, we might
  # exceed the maximum command line length. If the length is more than half the allowed
  # limit to exec, then let's fall back to doing the entire modules which is slower
  # but at least won't error out.
  path_args="${paths_to_check[*]}"
  if [[ ${#path_args} -gt $(($(getconf ARG_MAX) / 2)) ]]; then
    declare -a changed_modules
    while IFS= read -r module; do
      changed_modules+=("$module")
    done < <(printf "%s\n" "${paths_to_check[@]}" | cut -d'/' -f1 | sort | uniq)
    paths_to_check=("${changed_modules[@]}")
  fi

  echo ""
  set +eo pipefail
  "$exe" --pre-push "--push-commit=$push_sha" --dry-run --set-exit-if-changed "${paths_to_check[@]}" | sed 's/^/- /'
  formatting_check_exit_status=${PIPESTATUS[0]}
  set -eo pipefail
  echo ""

  if [ "$formatting_check_exit_status" -eq 3 ]; then
    echo_error "⚠️  Formatting issue found. Run '${exe} <path>' to apply formatting."
  elif [ "$formatting_check_exit_status" -ne 0 ]; then
    echo_error "⚠️  Code formatting check failed (exit code: $formatting_check_exit_status)."
  else
    echo_success "🎉 Formatting check completed successfully"
  fi

  return "$formatting_check_exit_status"
}

# Only do kotlin formatting checks when pushing to actually-remote remotes, not for e.g.
#  git push . origin/main:main
# We heuristically detect this by the presence of `:` character in the URL, since https
# and SSH urls should both have `:` characters but local file paths generally should not.
if [[ "$remote_url" =~ ":" ]]; then
  # Do checks for each local ref being pushed...
  while read -r _local_ref local_sha _remote_ref _remote_sha; do
    # ... except where the remote ref is being deleted, via e.g.
    #   git push origin +:foo
    if [[ "$local_sha" != "0000000000000000000000000000000000000000" ]]; then
      check_pushed_commits_formatting "$remote" "$local_sha"
    fi
  done
fi
