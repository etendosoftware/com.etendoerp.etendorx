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

def branchExistsRemote(repoUrl, branchName, scriptCtx) {
  if (!branchName?.trim()) {
    return false
  }
  return scriptCtx.sh(
    script: "git ls-remote --exit-code --heads ${repoUrl} ${branchName}",
    returnStatus: true
  ) == 0
}

def resolveRemoteHeadBranch(repoUrl, scriptCtx) {
  return scriptCtx.sh(
    script: "git ls-remote --symref ${repoUrl} HEAD | sed -n 's#^ref: refs/heads/\\([^[:space:]]*\\)[[:space:]]*HEAD#\\1#p'",
    returnStdout: true
  ).trim()
}

def resolveExistingRemoteBranch(repoUrl, candidates = [], scriptCtx = this) {
  def uniqueCandidates = candidates.findAll { it?.trim() }.unique()
  scriptCtx.echo "Evaluating remote branch candidates for ${repoUrl}: ${uniqueCandidates.join(' > ')}"

  for (String candidate : uniqueCandidates) {
    if (branchExistsRemote(repoUrl, candidate, scriptCtx)) {
      scriptCtx.echo "Using remote branch ${candidate} for ${repoUrl}"
      return candidate
    }
  }

  def remoteHead = resolveRemoteHeadBranch(repoUrl, scriptCtx)
  if (remoteHead) {
    scriptCtx.echo "No candidate branch found for ${repoUrl}. Falling back to remote HEAD branch: ${remoteHead}"
    return remoteHead
  }

  error("Could not resolve any branch for ${repoUrl}")
}

def extractBranchYearSuffix(branchName = env.GIT_BRANCH) {
  def matcher = (branchName =~ /-Y(\d{2})/)
  return matcher?.find() ? matcher[0][1].toInteger() : null
}

def currentTwoDigitYear() {
  return Calendar.getInstance(TimeZone.getTimeZone('UTC')).get(Calendar.YEAR) % 100
}

def isFutureYearBranch(branchName = env.GIT_BRANCH) {
  def branchYear = extractBranchYearSuffix(branchName)
  if (branchYear == null) {
    return false
  }
  return currentTwoDigitYear() < branchYear
}

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
      } else if (yearCommitBranch.toInteger() > yearPrereleaseBranch.toInteger() && yearCommitBranch.toInteger() > yearBackportBranch.toInteger()) {
        echo "Branch year (${yearCommitBranch}) is newer than both backport (${yearBackportBranch}) and prerelease (${yearPrereleaseBranch}) — using develop branch"
        return env.DEVELOP_BRANCH
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
