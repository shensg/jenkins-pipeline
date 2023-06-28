def call(env, buildNotice) {
    echo "\033[93m\n\n\n\n\n\n\n\n\n########################## 以下内容是构建通知的相关操作，请忽略 ##########################\033[0m"
    
    // get build user
    withCredentials([usernamePassword(credentialsId: 'deployment', passwordVariable: 'urlPwd', usernameVariable: 'urlUser')]) {
        sh "curl --user ${urlUser}:${urlPwd} ${env.BUILD_URL}consoleText | head -n 1 | tee tmp1.tmp"
    }
    def USER = readFile('tmp1.tmp').trim()
    
    if (buildNotice.type == 'dingding') {
        def title = "${env.JOB_NAME} 开始构建"
        def content = "# ${env.JOB_NAME} [开始构建](${env.BUILD_URL}console)\n________________\n> 项目名称：${env.JOB_NAME}\n\n> 构建编号：第${env.BUILD_NUMBER}次构建\n\n> 触发原因：${USER}\n\n"
    
        sh "curl -l -H 'Content-Type: application/json; charset=utf-8' -X POST -d '{\"msgtype\": \"markdown\",\"markdown\": {\"title\":\"" + title + "\",\"text\":\""+ content +" \"},\"at\": {\"isAtAll\": true}}' https://oapi.dingtalk.com/robot/send?access_token=${buildNotice.token}"
    
    }

    // clear
    sh "rm -rf tmp3.tmp tmp2.tmp tmp1.tmp"
}