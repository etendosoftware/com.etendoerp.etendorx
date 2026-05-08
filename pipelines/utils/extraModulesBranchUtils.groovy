/**
 * Utilities to parse jenkinsExtraModules files and resolve the best branch to checkout.
 */

def branchExistsRemote(repoDir, branchName, shStep) {
  if (!branchName?.trim()) {
    return false
  }
  return shStep(
    script: "cd ${repoDir} && git ls-remote --exit-code --heads origin ${branchName}",
    returnStatus: true
  ) == 0
}

def checkoutRemoteBranch(repoDir, branchName, label, shStep) {
  shStep "cd ${repoDir} && git checkout ${branchName}"
  echo "${label}: ${branchName}"
}

def parseExtraModuleEntries(rawValue) {
  if (!rawValue?.trim()) {
    return []
  }

  return rawValue
    .replace(',', '\n')
    .split('\n')
    .collect { it.trim() }
    .findAll { it && !it.startsWith('#') }
    .collect { line ->
      def parts = line.split(/\|/, 2)
      [
        repoUrl   : parts[0].trim(),
        branchSpec: parts.size() > 1 ? parts[1].trim() : ''
      ]
    }
}

def resolveBranchCandidates(branchSpec, defaultCandidates = [], tokenValues = [:]) {
  if (!branchSpec?.trim()) {
    return defaultCandidates.findAll { it?.trim() }.unique()
  }

  return branchSpec
    .split('>')
    .collect { it.trim() }
    .findAll { it }
    .collect { token ->
      def resolved = token
      tokenValues.each { key, value ->
        resolved = resolved.replace("{${key}}", value ?: '')
      }
      return resolved
    }
    .findAll { it?.trim() }
    .unique()
}

def checkoutBestBranch(Map params) {
  def repoDir = params.repoDir
  def moduleName = params.moduleName
  def branchSpec = params.branchSpec
  def defaultCandidates = params.defaultCandidates ?: []
  def tokenValues = params.tokenValues ?: [:]
  def shStep = params.sh

  def candidates = resolveBranchCandidates(branchSpec, defaultCandidates, tokenValues)
  echo "Branch candidates for ${moduleName}: ${candidates.join(' > ')}"
  for (String candidate : candidates) {
    if (branchExistsRemote(repoDir, candidate, shStep)) {
      checkoutRemoteBranch(repoDir, candidate, "Using branch for ${moduleName}", shStep)
      return candidate
    }
  }
  echo "No candidate branch found for ${moduleName}. Keeping current branch."
  return shStep(script: "cd ${repoDir} && git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
}

return this
