def call(env){
    // loading GlobalVars
    def GlobalVars = new org.devops.publicpackage.GlobalVars()
    // loading tools
    def tools = new org.devops.publicpackage.tools()
    // Preparation environment variable
    def ENV = sh(returnStdout: true, script: "echo \"${env.JOB_NAME}\" | sed -n 's#-# #gp' | gawk '{print \$1}'").trim()
    def ProjectName = sh(returnStdout: true, script: "echo \"${env.JOB_NAME}\" | sed -n 's#-# #gp' | gawk '{ \$1=\"\"; print \$0 }' | sed -n 's# ##gp'").trim()
    def PROJECT = sh(returnStdout: true, script: "echo \"${NAMESPACE}\" | sed -n 's#-# #gp' | gawk '{print \$1}'").trim()
    def DeployName = "${PROJECT}-${ENV}-${ProjectName}-html"

    if (ENV == "test"){
        globalBranch = GlobalVars.TestBranch
        kubeIp = GlobalVars.KubeTest
        noticeToken = GlobalVars.NoticeTest
    }else if (ENV == "uat"){
        globalBranch = GlobalVars.UatBranch
        kubeIp = GlobalVars.KubeProd
        noticeToken = GlobalVars.NoticeProd
    }else if (ENV == "prod"){
        globalBranch = GlobalVars.ProdBranch
        kubeIp = GlobalVars.KubeProd
        noticeToken = GlobalVars.NoticeProd
    }
    if (BRANCH == '') {
        BRANCH = "${globalBranch}"
    }
    if (REPLICAS == '') {
        REPLICAS = GlobalVars.REPLICAS
    }

    node{
        try {
            def buildEnv = '${ENV}'
            def gitRepo = ["branch": "${BRANCH}", "git": "${GitRepo}", "user": "gitlab"]
            def registryRepo = ["project": "${PROJECT}", "repo":"${REPO}", "dockerfile": "${DockerFile}"]
            def registryName = ["url": "${repoUrl}/${registryRepo.project}/${registryRepo.repo}", "user": "harbor"]

            // set manifests's git and k8s cluster
            def helmRelease = [["replica": "${REPLICAS}", "name": "${DeployName}", "namespace": "${NAMESPACE}", "hosts": "${HOSTS}", "flags": "--set image.containerPort=3000,ingress.annotations.\"nginx\\.ingress\\.kubernetes\\.io\\/proxy\\-body\\-size\"=5M", "chart": "chats/nginx"],]
            def k8sManifests = ""
            def k8sCluster = ["ipaddr": "${kubeIp}", "user": "root", "port": "9528", "work": "/data/yaml/${env.JOB_NAME}"]
            // verification pod is running
            podArray = [["namespace":"${NAMESPACE}", "controller":"deployment", "name":"${DeployName}"]]

            // set jenkins build notice
            buildNotice = ["type": "yim", "qid": "${GroupID}", "token": "${noticeToken}"]

            // image tags
            def dateTime = sh(returnStdout: true, script: "date +%y%m%d.%H%M").trim()
            imageTag = "${buildEnv}-${dateTime}"
            imageName = "${registryName.url}:${imageTag}"

            stage('Preparation'){
                echo "\n\n\n\n\n\n########################## 'Preparation' ##########################"
                tools.gitCheckout(gitRepo)
            }

            stage('Build Docker') {
                echo "\n\n\n\n\n\n########################## 'Build Docker' ##########################"
                tools.buildImage(buildEnv, imageName, registryName, registryRepo)
            }

            stage('Deploy') {
                echo "\n\n\n\n\n\n########################## 'Deploy' ##########################"
                if ( k8sManifests != "") {
                    tools.deployNginx(imageName, k8sCluster, k8sManifests)
                } else {
                    tools.helmDeployNginx(imageTag, k8sCluster, helmRelease, registryName)
                }
            }

            stage('Verification') {
                echo "\n\n\n\n\n\n########################## 'Verification' ##########################"

                // verification pod is running
                for (pod in podArray) {
                    verificationFunction(pod, k8sCluster)
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
            jenkinsBuildNotice(env, buildNotice, imageName, buildResults, errorContent)

            // clean workspace
            deleteDir()
        }
    }
}
