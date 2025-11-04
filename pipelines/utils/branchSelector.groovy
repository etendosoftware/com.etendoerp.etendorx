#!/usr/bin/env groovy

/**
 * BranchSelector.groovy
 *
 * Utility to determine the base branch (CORE or BASE) according to the current branch type
 * and the years defined in the backport and prerelease branches.
 *
 * Parameters:
 *   @param outputVar (String)  → Name of the environment variable to set (default 'CORE_BRANCH')
 *   @param includeDevLogic (Boolean) → Whether to include develop/feature/epic logic (default true)
 */

def determineBranch(boolean includeDevLogic = true) {
  def yearBackportBranch = (env.BACKPORT_BRANCH =~ /release\/(\d{2})\./)?.find() ? (env.BACKPORT_BRANCH =~ /release\/(\d{2})\./)[0][1] : null
  def yearPrereleaseBranch = (env.PRERELEASE_BRANCH =~ /prerelease\/(\d{2})\./)?.find() ? (env.PRERELEASE_BRANCH =~ /prerelease\/(\d{2})\./)[0][1] : null

  if (includeDevLogic && (
      GIT_BRANCH.startsWith(env.DEVELOP_BRANCH) ||
      ((GIT_BRANCH.startsWith("feature") || GIT_BRANCH.startsWith("epic")) && !(GIT_BRANCH.contains("-Y")))
  )) {
    echo '-------------------------- Develop/Feature/Epic Branch Detected --------------------------'
    return env.DEVELOP_BRANCH

  } else if (GIT_BRANCH.contains("-Y")) {
    def yearCommitBranch = (GIT_BRANCH =~ /-Y(\d{2})/)?.find() ? (GIT_BRANCH =~ /-Y(\d{2})/)[0][1] : null

    if (yearCommitBranch && yearBackportBranch && yearPrereleaseBranch) {
      env.FROM_BACKPORT = (yearCommitBranch == yearBackportBranch) ? TRUE : FALSE
      env.FROM_PRERELEASE = (yearCommitBranch == yearPrereleaseBranch) ? TRUE : FALSE

      if (env.FROM_BACKPORT == TRUE) {
        echo "Branch from Backport (${env.BACKPORT_BRANCH})"
        return env.BACKPORT_BRANCH
      } else if (env.FROM_PRERELEASE == TRUE) {
        echo "Branch from Prerelease (${env.PRERELEASE_BRANCH})"
        return env.PRERELEASE_BRANCH
      } else {
        error("Year mismatch: module branch (${yearCommitBranch}) does not match backport (${yearBackportBranch}) or prerelease (${yearPrereleaseBranch}).")
      }
    } else {
      error("Year mismatch between module branch (${yearCommitBranch}), backport branch (${yearBackportBranch}) and prerelease branch (${yearPrereleaseBranch}). Cannot determine base branch.")
    }
  } else if (env.GIT_BRANCH.startsWith("release")) {
    echo '-------------------------- Release Branch Detected --------------------------'
    env.FROM_BACKPORT = TRUE
    return env.BACKPORT_BRANCH

  } else if (env.GIT_BRANCH.startsWith("prerelease")) {
    echo '-------------------------- Prerelease Branch Detected --------------------------'
    env.FROM_PRERELEASE = TRUE
    return env.PRERELEASE_BRANCH
  }
  echo "⚠️ No matching rule found for branch '${env.GIT_BRANCH}'"
  return env.MAIN_BRANCH
}

return this
