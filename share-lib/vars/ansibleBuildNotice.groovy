def call(env, currentBuild, buildResults, buildNotice) {
    echo "\033[93m\n\n\n\n\n\n\n\n\n########################## 以下内容是构建通知的相关操作，请忽略 ##########################\033[0m"
    
    if (env.RELEASE_PROCESS && env.BUILD_TYPE) {
        sh "python3 PythonScript/jenkinsBuildNotice.py ${env.BUILD_URL} ${env.JOB_NAME} ${env.BUILD_NUMBER} '${currentBuild.durationString}' ${buildResults} ${buildNotice.qid} '{\"RELEASE_PROCESS\": \"${RELEASE_PROCESS}\", \"CUSTOM_IMAGE\": \"${CUSTOM_IMAGE}\" , \"BUILD_TYPE\": \"${BUILD_TYPE}\"}'"
    
    } else if (env.RELEASE_PROCESS) {
        sh "python3 PythonScript/jenkinsBuildNotice.py ${env.BUILD_URL} ${env.JOB_NAME} ${env.BUILD_NUMBER} '${currentBuild.durationString}' ${buildResults} ${buildNotice.qid} '{\"RELEASE_PROCESS\": \"${RELEASE_PROCESS}\", \"CUSTOM_IMAGE\": \"${CUSTOM_IMAGE}\"}'"
    
    } else if (env.BUILD_TYPE) {
        sh "python3 PythonScript/jenkinsBuildNotice.py ${env.BUILD_URL} ${env.JOB_NAME} ${env.BUILD_NUMBER} '${currentBuild.durationString}' ${buildResults} ${buildNotice.qid} '{\"BUILD_TYPE\": \"${BUILD_TYPE}\"}'"
    
    } else {
        sh "python3 PythonScript/jenkinsBuildNotice.py ${env.BUILD_URL} ${env.JOB_NAME} ${env.BUILD_NUMBER} '${currentBuild.durationString}' ${buildResults} ${buildNotice.qid} '{}'"

    }
}
