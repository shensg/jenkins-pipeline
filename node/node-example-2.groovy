// 优化后精简版
node {
    try {
        //import lib
        @Library('share-lib') _
        def dateTime = sh(returnStdout: true, script: "date +%y%m%d.%H%M%S").trim()

        // set imageRepo
        imageRepo = """'repository': '{{ dockerRegistry.repo }}','tag': '{{ dockerRegistry.tag }}'"""

        // set image info
        def dockerRegistry = ["repo": "${RepoUrl}", "tag": "", "dockerfile": "Dockerfile", "user": "<RepoUser>"]

        // set ansible
        def ansibleRepoCode = ["branch": "master", "repo": "<pipelineRepo>", "path": "AnsiblePlaybook/build-k8s-code.yml", "user": "gitlab"]
        def ansibleRepoDeploy = ["branch": "master", "repo": "<pipelineRepo>", "path": "AnsiblePlaybook/build-k8s-deploy.yml", "user": "gitlab"]
        def ansibleVars = """
                # 设置代码库、Docker Registry
                codeRepo: [{"branch": "alpha", "repo": "<projectRepo>", "workspace": "${env.WORKSPACE}/code"}]
                dockerRegistry: {"repo": "${RepoUrl}", "tag": "", "dockerfile": "Dockerfile"}

                # 设置K8s集群、Helm Release
                k8sCluster: "ydj-test-tke"
                helmRelease: [{"name": "<serverName>", "namespace": "<namespace>", "flags": "{'replicaCount': 1,'ingress':{'hosts':['']},'image':{${imageRepo}}}", "chart": "yidejia/nginx"},]

                # 验证Workload
                validateWorkloads: [{"namespace":"<namespace>", "controller":"deployment", "name":"<serverName>"}]
                """

        // set qid
        buildNotice = ["qid": ""]

        // ansible git
        ansibleBuildCode(ansibleRepoCode, ansibleVars)

        // build docker images
        buildDockerImage(dockerRegistry)

        // ansible deploy
        ansibleBuildDeploy(ansibleRepoDeploy, ansibleVars)
        buildResults = '成功'

    }catch (e) {
        buildResults = '失败'
        echo "\n\n\n\n\n\n########################## 构建失败原因 ##########################\n${e}\n\n################################################################\n\n\n\n\n\n"

    }
    finally {
        // notice
        ansibleBuildNotice(env, currentBuild, buildResults, buildNotice)

        // clean workspace
        deleteDir()
    }
}
