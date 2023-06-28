def call(env, buildNotice, imageName, buildResults, errorContent) {
    echo "\033[93m\n\n\n\n\n\n\n\n\n########################## 以下内容是构建通知的相关操作，请忽略 ##########################\033[0m"
    
    // get build user
    withCredentials([usernamePassword(credentialsId: 'dingding', passwordVariable: 'urlPwd', usernameVariable: 'urlUser')]) {
        sh "curl --user ${urlUser}:${urlPwd} ${env.BUILD_URL}consoleText | head -n 1 | tee tmp1.tmp"
    }
    def USER = readFile('tmp1.tmp').trim()
    
    // get build use time
    def startTime =currentBuild.startTimeInMillis/1000 as int
    sh "endTime=`date +%s`;echo \$endTime;useTime=`expr \$endTime - ${startTime}`;echo \$useTime;useTimeH=`echo \$useTime|awk -F : '{print \$1 / 3600}'|awk -F . '{print \$1}'`;useTimeM=`echo \$useTime|awk -F : '{print \$1 % 3600 / 60}'|awk -F . '{print \$1}'`;useTimeS=`echo \$useTime|awk -F : '{print \$1 % 60}'`;if [ \$useTime -lt 60 ];then echo \$useTimeS秒|tee tmp2.tmp;elif [ \$useTime -lt 3600 ];then echo \$useTimeM分\$useTimeS秒|tee tmp2.tmp;else echo \$useTimeH小时\$useTimeM分\$useTimeS秒|tee tmp2.tmp;fi"
    def buildTime = readFile('tmp2.tmp').trim()
    
    if (buildNotice.type == 'dingding') {
        def title = "${env.JOB_NAME} 构建${buildResults}"
        def content = "# ${env.JOB_NAME} [构建${buildResults}](${env.BUILD_URL}console)\n________________\n> 项目名称：${env.JOB_NAME}\n\n> 构建编号：第${env.BUILD_NUMBER}次构建\n\n> 触发原因：${USER}\n\n> 部署镜像：${imageName}\n\n> 构建时长：${buildTime}\n\n"
    
        if (buildResults == '成功') {
            content += '> ![screenshot](https://${image_url}/%E7%BB%BF%E8%89%B2.png)'
        } else if (buildResults == '失败') {
            content += "> 失败原因：${errorContent}\n\n> ![screenshot](https://${image_url}/%E7%BA%A2%E8%89%B2.png)"
        } 

        sh "curl -l -H 'Content-Type: application/json; charset=utf-8' -X POST -d '{\"msgtype\": \"markdown\",\"markdown\": {\"title\":\"" + title + "\",\"text\":\""+ content +" \"},\"at\": {\"isAtAll\": true}}' https://oapi.dingtalk.com/robot/send?access_token=${buildNotice.token}"
    
    } else if (buildNotice.type == 'yim') {
        def title = "${env.JOB_NAME} 构建${buildResults}"
        def content = "${env.JOB_NAME} 构建${buildResults}\\\\n________________\\\\n项目名称：${env.JOB_NAME}\\\\n构建编号：第${env.BUILD_NUMBER}次构建\\\\n触发原因：${USER}\\\\n部署镜像：${imageName}\\\\n构建时长：${buildTime}\\\\n"
    
        if (buildResults == '成功') {
            imageUrl = 'https://${image_url}/%E7%BB%BF%E8%89%B2.png'
        } else if (buildResults == '失败') {
            content += "失败原因：${errorContent}\\\\n"
            imageUrl ="https://${image_url}/%E7%BA%A2%E8%89%B2.png"
        } 

        writeFile file: 'tmp3.tmp', text: """
            curl --location --request POST 'https://${notice_url}/conversation/${buildNotice.qid}/message?is_room=1&token=${buildNotice.token}' \\
            --header 'Content-Type: application/json' \\
            -d '{
                "is_room": true,
                "type": 21,
                "content": "{\\"direction\\": \\"column\\", \\"width\\": 320, \\"title\\": \\"${title}\\", \\"elements\\": [{\\"id\\": 1, \\"type\\": \\"image\\", \\"url\\": \\"${imageUrl}\\", \\"clickType\\": 3, \\"height\\": 55, \\"marginBottom\\": 8, \\"mobile_marginBottom\\": 10}, {\\"id\\": 2, \\"type\\": \\"text\\", \\"text\\": \\"${content}\\", \\"color\\": \\"rgba(10,19,18,0.6)\\", \\"mobile_color\\": \\"#515151\\", \\"marginBottom\\": 12, \\"mobile_marginBottom\\": 16,\\"marginLeft\\": 12, \\"marginRight\\": 12}, {\\"id\\": 3, \\"type\\": \\"text\\", \\"text\\": \\"查看详情\\", \\"alignItems\\": \\"center\\", \\"justifyContent\\": \\"center\\", \\"height\\": 40, \\"clickType\\": 2, \\"href\\": \\"${env.BUILD_URL}console\\", \\"color\\": \\"#00C8C8\\"}]}"
            }'
            """
        sh "bash tmp3.tmp"

    }

    // clear
    sh "rm -rf tmp3.tmp tmp2.tmp tmp1.tmp"
}