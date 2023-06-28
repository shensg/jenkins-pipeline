// 优化后
node {
    try {
        //import lib
        @Library('share-lib') _

        // set build env type, code's git and docker registry
        def buildEnv = ''
        def gitRepo = ["branch": "master", "git": "<projectGitRepo>", "user": "<gitUser>"]
        def registryRepo = ["project": "<project>", "repo":"<Repo>", "dockerfile": "Dockerfile"]
        def registryName = ["url": "${RepoUrl}/${registryRepo.project}/${registryRepo.repo}", "user": "harbor"]

        // set manifests's git and k8s cluster
        k8sManifests = ["branch": "master", "git": "<yamlGitRepo>", "path": "<yaml_path>" , "user": "gitlab"]
        k8sCluster = ["ipaddr": "<k8sAddr>", "user": "root", "port": "", "work": "/yaml"]

        // verification pod is running

        podArray = [['namespace':'<namespace>', 'controller':'deployment', 'name':'<serverName>']]
        build_env = 'example.go'


        // set jenkins build notice
        //buildNotice = ["type": "dingding", "token": ""]


        stage('Preparation') {
            echo "\n\n\n\n\n\n########################## 'Preparation' ##########################"

            // git
            git branch: "${gitRepo.branch}", credentialsId: "${gitRepo.user}", url: "${gitRepo.git}"

            // tool
            dockerPath = tool 'docker'
            env.PATH = "${dockerPath}/bin:${env.PATH}"
        }

        stage('Build Docker') {
            echo "\n\n\n\n\n\n########################## 'Build Docker' ##########################"

            def dateTime = sh(returnStdout: true, script: "date +%y%m%d.%H%M").trim()
            imageName = "${registryName.url}:${buildEnv}-${dateTime}"

            // copy Dockerfile
            sh "cp ${registryRepo.dockerfile} Dockerfile"

            // copy Dockerfile
            sh "sed -i s#BUILD_ENV#${build_env}#g  Dockerfile"

            // docker build and push
            docker.withRegistry("https://${registryName.url}", "${registryName.user}") {
                docker.build("${imageName}").push()
            }

            // clear images
            sh "docker images | grep ${registryName.url} | grep ${buildEnv} | tail -n +2 | gawk '{print \$1\":\"\$2}' | xargs -i docker rmi {} &"

        }


        stage('Deploy') {
            echo "\n\n\n\n\n\n########################## 'Deploy' ##########################"

            dir('k8s-work') {
                // git
                git branch: "${k8sManifests.branch}", credentialsId: "${k8sManifests.user}", url: "${k8sManifests.git}"

                // init k8s
                sh "ssh -p ${k8sCluster.port} root@${k8sCluster.ipaddr} 'mkdir -p ${k8sCluster.work} && chown -R ${k8sCluster.user}. ${k8sCluster.work}'"

                // edit manifests
                command = """sed -i \'s!MiniProgramAdminImage!${imageName}!g\' ${k8sManifests.path}/${BUILD_TYPE}.yaml"""
                sh "${command}"

                // sync manifests
                sh "rsync -avruz --delete --progress -e 'ssh -p ${k8sCluster.port}' --exclude '.git' ${k8sManifests.path} root@${k8sCluster.ipaddr}:${k8sCluster.work}"

                // kubectl apply
                sh "ssh -p ${k8sCluster.port} ${k8sCluster.user}@${k8sCluster.ipaddr} 'sudo kubectl apply -f ${k8sCluster.work}/${k8sManifests.path}/${BUILD_TYPE}.yaml'"
            }
        }

        stage('Verification') {
            echo "\n\n\n\n\n\n########################## 'Verification' ##########################"

            // verification pod is running
            for (pod in podArray) {
                verificationFunction(pod, k8sCluster, k8sManifests)
            }
        }

        stage('Notice') {
            buildResults = '成功'
            errorContent = ''
        }

    }catch (e) {
        buildResults = '失败'
        echo "\n\n\n\n\n\n########################## 构建失败原因 ##########################\n${e}\n\n################################################################\n\n\n\n\n\n"
        errorContent = e

        try {
            echo "${imageName}"
        }catch (a) {
            imageName = "无"
        }
    }
    finally {
        golangBuildNotice(env, buildNotice, imageName, buildResults, errorContent, BUILD_TYPE)

        // clean workspace
        deleteDir()
    }
}

def golangBuildNotice(env, buildNotice, imageName, buildResults, errorContent, BUILD_TYPE) {
    echo "\033[93m\n\n\n\n\n\n\n\n\n########################## 以下内容是构建通知的相关操作，请忽略 ##########################\033[0m"

    // get build user
    withCredentials([usernamePassword(credentialsId: 'deployment', passwordVariable: 'urlPwd', usernameVariable: 'urlUser')]) {
        sh "curl --user ${urlUser}:${urlPwd} ${env.BUILD_URL}consoleText | head -n 1 | tee tmp1.tmp"
    }
    def USER = readFile('tmp1.tmp').trim()

    // get build use time
    def startTime =currentBuild.startTimeInMillis/1000 as int
    sh "endTime=`date +%s`;echo \$endTime;useTime=`expr \$endTime - ${startTime}`;echo \$useTime;useTimeH=`echo \$useTime|awk -F : '{print \$1 / 3600}'|awk -F . '{print \$1}'`;useTimeM=`echo \$useTime|awk -F : '{print \$1 % 3600 / 60}'|awk -F . '{print \$1}'`;useTimeS=`echo \$useTime|awk -F : '{print \$1 % 60}'`;if [ \$useTime -lt 60 ];then echo \$useTimeS秒|tee tmp2.tmp;elif [ \$useTime -lt 3600 ];then echo \$useTimeM分\$useTimeS秒|tee tmp2.tmp;else echo \$useTimeH小时\$useTimeM分\$useTimeS秒|tee tmp2.tmp;fi"
    def buildTime = readFile('tmp2.tmp').trim()

    if (buildNotice.type == 'dingding') {
        def title = "${env.JOB_NAME} 构建${buildResults}"
        def content = "# ${env.JOB_NAME} [构建${buildResults}](${env.BUILD_URL}console)\n________________\n> 项目名称：${env.JOB_NAME}\n\n> 构建模块：${BUILD_TYPE}\n\n> 构建编号：第${env.BUILD_NUMBER}次构建\n\n> 触发原因：${USER}\n\n> 部署镜像：${imageName}\n\n> 构建时长：${buildTime}\n\n"

        if (buildResults == '成功') {
            content += '> ![screenshot](https://${imageUrl}/%E7%BB%BF%E8%89%B2.png)'
        } else if (buildResults == '失败') {
            content += "> 失败原因：${errorContent}\n\n> ![screenshot](https://${imageUrl}/%E7%BA%A2%E8%89%B2.png)"
        }

        sh "curl -l -H 'Content-Type: application/json; charset=utf-8' -X POST -d '{\"msgtype\": \"markdown\",\"markdown\": {\"title\":\"" + title + "\",\"text\":\""+ content +" \"},\"at\": {\"isAtAll\": true}}' https://oapi.dingtalk.com/robot/send?access_token=${buildNotice.token}"

    }

    // clear
    sh "rm -rf tmp3.tmp tmp2.tmp tmp1.tmp"
}