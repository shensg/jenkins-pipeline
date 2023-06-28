@Library('utils-share-lib') _
def process = currentBuild.getProjectName()
echo "${process}"(env)
