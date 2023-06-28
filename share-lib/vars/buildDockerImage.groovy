def call(dockerRegistry) {
    // build docker images
    stage ('Build Images') {
        echo "\n\n\n\n\n\n########################## 'Build Docker' ##########################"
        imageName = "${dockerRegistry.repo}:${dockerRegistry.tag}"

        script {
            dir("${env.WORKSPACE}/code") {
                // docker build and push
                docker.withRegistry("https://${dockerRegistry.repo}", "${dockerRegistry.user}") {
                    docker.build("${imageName}", "-f ${dockerRegistry.dockerfile} .").push()
                }
            }
        }

        // clear images
        sh "docker images | grep ${dockerRegistry.url} | grep ${dockerRegistry.tag} |tail -n +2 | gawk '{print \$1\":\"\$2}' | xargs -i docker rmi {} &"
    }
}