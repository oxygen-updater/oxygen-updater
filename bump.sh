#!/usr/bin/env bash

# -e exits on error, -u errors on undefined variables, and -o (for option) pipefail exits on command pipe failures
set -euo pipefail

{ set +x; } 2>/dev/null # skip execution trace

readonly VERSION_FILE='app/build.gradle.kts'
readonly README_FILE='README.md'
readonly COMMITMSG_FILE='.git/COMMIT_EDITMSG'

readonly RED=$(tput setaf 1)
readonly YELLOW=$(tput setaf 3)
readonly RESET=$(tput sgr0)

# Do some basic input validation, because using user inputs directly could be
# dangerous (think SQLi but for shell control).
# Note: bash doesn't support `\d`, use `[0-9]` or `[[:digit:]]`
readonly SEMVER='(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)'
readonly SEMVER_REGEX="^$SEMVER$"

# Used with `$OSTYPE` to detect if the shell is running in Windows
# see: https://stackoverflow.com/a/8597411
readonly WINDOWS_OS_REGEX='^(cygwin|msys)$'
# Used with `uname -o` to detect if the shell is running in Windows
# see: https://en.wikipedia.org/wiki/Uname#Examples
readonly UNAME_WINDOWS_OS_REGEX='^(MS/Windows|Cygwin|Msys)$'

# To avoid unnecessary git-diffs later on, ensure line
# endings are CRLF if shell is running under Windows
correct_line_endings() {
  if [[ $OSTYPE =~ $WINDOWS_OS_REGEX ]]; then
    unix2dos "$@"
  fi
}

version_name="$*"
if [[ -z $version_name ]]; then
  read -p 'Semantic version: ' version_name
fi
readonly version_name

if [[ ! "$version_name" =~ $SEMVER_REGEX ]]; then
  echo "${RED}Error: invalid version (must be semver)${RESET}"
  exit 1
fi

existing_version_name=$(grep -oE "versionName = \"$SEMVER\"" "$VERSION_FILE" | sed -E 's/[^0-9\.]//g')
if [[ "$existing_version_name" != "$version_name" ]]; then
  # Extract `versionCode` from version file and increment it
  existing_version_code=$(grep -oE 'versionCode = [0-9]+' "$VERSION_FILE" | sed -E 's/[^0-9]//g')
  readonly existing_version_code
  version_code=$((existing_version_code + 1))

  # Replace `versionName = "…"` in the version file with the user-supplied version
  # -b: treat file as binary, thus preserving line endings (must be before -i)
  # -i: in-place
  # -E: extended regex (can also be -r)
  # s/: replace
  # /w: write changes to specified file (will be truncated if it already exists)
  sed -bi -E "s/(versionName) = \"$SEMVER\"/\1 = \"$version_name\"/" "$VERSION_FILE"
  # Write incremented `versionCode` back to version file
  sed -bi -E "s/(versionCode) = [0-9]+/\1 = $version_code/" "$VERSION_FILE"
  echo "${YELLOW}Updated $VERSION_FILE with $version_name ($version_code)${RESET}"

  # Update release badge in README
  sed -bi -E "s/(https:\/\/img\.shields\.io\/badge\/release)-v$SEMVER/\1-v$version_name/g" "$README_FILE"
  echo "${YELLOW}Updated $README_FILE with $version_name${RESET}"

  # Correct line endings (if required) before staging
  correct_line_endings "$VERSION_FILE"
  correct_line_endings "$README_FILE"
  git add "$VERSION_FILE"
  git add "$README_FILE"

  existing_commit_message=$(cat "$COMMITMSG_FILE")
  # Don't commit automatically. Instead, prepare a "release" commit message (preserving existing).
  git commit -em "Release $version_name

https://github.com/oxygen-updater/oxygen-updater/releases/tag/oxygen-updater-$version_name

$existing_commit_message"
else
  echo "Nothing changed, nothing to do"
fi
