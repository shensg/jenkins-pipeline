package org.devops.publicpackage

def gitCheckout(gitRepo) {
    // git
    git branch: "${gitRepo.branch}", credentialsId: "${gitRepo.user}", url: "${gitRepo.git}"
}

def buildImage(buildEnv, imageName, registryName, registryRepo) {
    // tool
    dockerPath = tool 'docker'
    env.PATH = "${dockerPath}/bin:${env.PATH}"

    // docker build and push
    docker.withRegistry("https://${registryName.url}", "${registryName.user}") {
        docker.build("${imageName}", "-f ${registryRepo.dockerfile} .").push()
    }

    // clear images
    sh "docker images | grep ${registryName.url} | grep ${buildEnv} | tail -n +2 | gawk '{print \$1\":\"\$2}' | xargs -i docker rmi {} &"
}

def deployNginx(imageName, k8sCluster, k8sManifests) {
    // init k8s
    //sh "ssh -p ${k8sCluster.port} ${k8sCluster.user}@${k8sCluster.ipaddr} 'test -d ${k8sCluster.work} || mkdir -p ${k8sCluster.work} && chown ${k8sCluster.user}. -R ${k8sCluster.work}'"
    dir('k8s-work') {
        // git
        git branch: "${k8sManifests.branch}", credentialsId: "${k8sManifests.user}", url: "${k8sManifests.git}"

        // edit manifests
        command = """sed -i \'s!MiniProgramAdminImage!${imageName}!g\' ${k8sManifests.path}/deploy.yaml"""
        sh "${command}"

        // sync manifests
        sh "rsync -avruz --delete --progress -e 'ssh -p ${k8sCluster.port}' --exclude '.git' ${k8sManifests.path} root@${k8sCluster.ipaddr}:${k8sCluster.work}"

        // kubectl apply
        sh "ssh -p ${k8sCluster.port} ${k8sCluster.user}@${k8sCluster.ipaddr} 'sudo kubectl apply -f ${k8sCluster.work}/${k8sManifests.path}/deploy.yaml'"

    }
}

def helmDeployNginx(imageTag, k8sCluster, helmRelease, registryName) {
    // init k8s
    sh "ssh -p ${k8sCluster.port} ${k8sCluster.user}@${k8sCluster.ipaddr} 'test -d ${k8sCluster.work} || mkdir -p ${k8sCluster.work} && chown ${k8sCluster.user}. -R ${k8sCluster.work}'"

    // kubectl apply
    for (helm in helmRelease) {
        command = """helm repo update && helm upgrade --history-max 1 --install ${helm.name} -n ${helm.namespace} ${helm.chart} --set replicaCount=${helm.replica},image.repository=${registryName.url},image.tag=${imageTag},ingress.hosts={${helm.hosts}} ${helm.flags}"""
        sh "echo '${command}'"
        sh "ssh -p ${k8sCluster.port} ${k8sCluster.user}@${k8sCluster.ipaddr} '${command}'"

    }
}




