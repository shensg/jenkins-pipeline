def call(ansibleRepo, ansibleVars, ansibleInv="") {
    stage('Ansible') { 
        echo "\n\n\n\n\n\n########################## 'Ansible' ##########################"
        
        // git
        git changelog: false, branch: "${ansibleRepo.branch}", credentialsId: "${ansibleRepo.user}", url: "${ansibleRepo.repo}"
        
        // ansible-playbook
        writeFile file: 'AnsiblePlaybook/hosts', text: "${ansibleInv}"
        writeFile file: 'AnsiblePlaybook/vars', text: "${ansibleVars}"
        if (env.RELEASE_PROCESS && env.BUILD_TYPE) {
            ansiblePlaybook extras: "-e 'RELEASE_PROCESS=${env.RELEASE_PROCESS} CUSTOM_IMAGE=${env.CUSTOM_IMAGE} BUILD_TYPE=${BUILD_TYPE}'", colorized: true, credentialsId: 'ansible', installation: 'ansible2.11.12', playbook: "${ansibleRepo.path}"

        } else if (env.RELEASE_PROCESS) {
            ansiblePlaybook extras: "-e 'RELEASE_PROCESS=${env.RELEASE_PROCESS} CUSTOM_IMAGE=${env.CUSTOM_IMAGE}'", colorized: true, credentialsId: 'ansible', installation: 'ansible2.11.12', playbook: "${ansibleRepo.path}"

        } else if (env.BUILD_TYPE) {
            ansiblePlaybook extras: "-e 'BUILD_TYPE=${BUILD_TYPE}'", colorized: true, credentialsId: 'ansible', installation: 'ansible2.11.12', playbook: "${ansibleRepo.path}"

        } else {
            ansiblePlaybook extras: "", colorized: true, credentialsId: 'ansible', installation: 'ansible2.11.12', inventory: 'AnsiblePlaybook/hosts', playbook: "${ansibleRepo.path}"

        }

    }

    stage('Notice') {
        //
    }
}
