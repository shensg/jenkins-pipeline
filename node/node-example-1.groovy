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
