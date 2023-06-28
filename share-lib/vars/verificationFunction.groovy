def call(workload, k8sCluster, isPod=false) {
    // create script 
    if (isPod) {
        writeFile file: 'verification.sh', text: """
            for (( i=0; i<100; i++ )); do
                total_allstatus=\$(kubectl -n ${workload.namespace} get po | grep ${workload.name} | gawk '{print \$3}' | wc -l)
                total_running=\$(kubectl -n ${workload.namespace} get po | grep ${workload.name} | grep -ac 'Running')
                pod_status=\$(kubectl -n ${workload.namespace} get po | grep ${workload.name} | gawk '{print \$3}' | uniq)
                scale_status=\$(kubectl -n ${workload.namespace} get pod ${workload.name} | tail -1 | gawk '{print \$2}' | gawk -F'/' '{if (\$1 == \$2){print 1} else {print 0}}')
    
                if [ \$total_allstatus -eq 1 ] && [ \$pod_status == 'Running' ] && [ \$total_running -eq 1 ] && [ \$scale_status -eq 1 ]; then 
                    echo -e "\n\n\n##################\n'${workload.name}'对象的所有POD已经处于'Running'状态。\n##################\n\n\n"
                    break
                elif [ \$i -eq 20 ]; then 
                    echo -e "\n\n\n！！！！！！！！！！！\n'${workload.name}'对象的POD运行状态异常，请登录k8s集群查看！！\n！！！！！！！！！！！\n\n\n"
                    exit 1
                else 
                    echo -e "'${workload.name}'对象的所有POD还没完全运行！！\n\n\n"
                fi
    
                sleep 10
            done
            """
    } else {
        writeFile file: 'verification.sh', text: """
            for (( i=0; i<100; i++ )); do
                total_allstatus=\$(kubectl -n ${workload.namespace} get po | grep ${workload.name}- | gawk '{print \$3}' | wc -l)
                total_running=\$(kubectl -n ${workload.namespace} get po | grep ${workload.name}- | grep -ac 'Running')
                pod_status=\$(kubectl -n ${workload.namespace} get po | grep ${workload.name}- | gawk '{print \$3}' | uniq)
                scale=\$(kubectl -n ${workload.namespace} get ${workload.controller} ${workload.name} | tail -1 | gawk '{print \$2}' | gawk -F'/' '{print \$2}')
    
                if [ \$total_allstatus -eq \$scale ] && [ \$pod_status == 'Running' ] && [ \$total_running -eq \$scale ]; then 
                    echo -e "\n\n\n##################\n'${workload.name}'对象的所有POD已经处于'Running'状态。\n##################\n\n\n"
                    break
                elif [ \$i -eq 60 ]; then 
                    echo -e "\n\n\n！！！！！！！！！！！\n'${workload.name}'对象的POD运行状态异常，请登录k8s集群查看！！\n！！！！！！！！！！！\n\n\n"
                    exit 1
                else 
                    echo -e "'${workload.name}'对象的所有POD还没完成更新！！\n其期望的副本数为：\$scale\n目前运行的副本数为：\$total_allstatus\n处于'Running'状态的副本数为：\$total_running\n\n\n"
                fi
    
                sleep 10
            done
            """
    }

    // execute script 
    sh label: '', script: """
        scp -P ${k8sCluster.port} verification.sh ${k8sCluster.ipaddr}:${k8sCluster.work}/
        ssh -p ${k8sCluster.port} ${k8sCluster.user}@${k8sCluster.ipaddr} 'bash ${k8sCluster.work}/verification.sh'
        rm -f verification.sh
        """
}
