/**
 * Utilities to parse jenkinsExtraModules files and resolve the best branch to checkout.
 */

def buildGitSshPrefix(sshCmd) {
  return sshCmd?.trim() ? "GIT_SSH_COMMAND='${sshCmd}' " : ''
}

def branchExistsRemote(repoDir, branchName, scriptCtx, sshCmd = null) {
  if (!branchName?.trim()) {
    return false
  }
  def gitSshPrefix = buildGitSshPrefix(sshCmd)
  return scriptCtx.sh(
    script: "cd ${repoDir} && ${gitSshPrefix}git ls-remote --exit-code --heads origin ${branchName}",
    returnStatus: true
  ) == 0
}

def checkoutRemoteBranch(repoDir, branchName, label, scriptCtx) {
  scriptCtx.sh "cd ${repoDir} && git checkout ${branchName}"
  scriptCtx.echo "${label}: ${branchName}"
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
  def scriptCtx = params.script ?: this
  def sshCmd = params.sshCmd

  def candidates = resolveBranchCandidates(branchSpec, defaultCandidates, tokenValues)
  scriptCtx.echo "Branch candidates for ${moduleName}: ${candidates.join(' > ')}"
  for (String candidate : candidates) {
    if (branchExistsRemote(repoDir, candidate, scriptCtx, sshCmd)) {
      checkoutRemoteBranch(repoDir, candidate, "Using branch for ${moduleName}", scriptCtx)
      return candidate
    }
  }
  scriptCtx.echo "No candidate branch found for ${moduleName}. Keeping current branch."
  return scriptCtx.sh(script: "cd ${repoDir} && git rev-parse --abbrev-ref HEAD", returnStdout: true).trim()
}

return this
