# Jenkins 流水线的基本使用例子

* 详细的例子我分别放在不同的目录中了，希望对大家学习jenkins流水线能提供一些思路


## 关于流水线概念

流水线是用户定义的一个CD流水线模型。流水线的代码是定义整个构建的过程，测试和交付。Pipeline 模块是 声明式流水线语法的关键

## 两种流水线示例：

Jenkins pipeline分为两种构建项目时的流水线声明，一种是声明式流水线，一种是脚本化流水线。关于不管是"声明式"流水线或者"脚本化"流水线。下面演示的代码骨架(语法)说明了声明式流水线语法和 脚本化流水线语法之间的根本差异

* 笔者更喜欢脚本式流水线

1、声明式流水线
简单的代码
```groovy
pipeline {
    agent any ①
    stages {
        stage('Build') { ②
            steps {
                // ③
            }
        }
        stage('Test') { ④
            steps {
                // ⑤
            }
        }
        stage('Deploy') { ⑥
            steps {
                // ⑦
            }
        }
    }
}
```

    ① 在任何可用的代理上，执行流水线或它的任何阶段。
    ② 定义 “Build” 阶段。
    ③ 执行与 “Build” 阶段相关的步骤。
    ④ 定义 “Test”阶段
    ⑤ 执行与 “Test” 阶段相关的步骤。
    ⑥ 定义 “Deploy”阶段
    ⑦ 执行与 “Deploy” 阶段相关的步骤。


2、脚本流水线

简单的代码
```groovy
node {  ①
    stage('Build') { ②
        // ③
    }
    stage('Test') { ④
        // ⑤
    }
    stage('Deploy') { ⑥
        // ⑦
    }
}
```

    ① 在任何可用的代理上【可以指定节点运行】，执行流水线或它的任何阶段。
    ② 定义 "Build" 阶段。 stage 块 在脚本化流水线语法中是可选的。 然而, 在脚本化流水线中实现 stage 块 ，可以清楚的显示Jenkins UI中的每个 stage 的任务子集。
    ③ 执行与 "Build" 阶段相关的步骤。
    ④ 定义 "Test" 阶段。
    ⑤ 执行与 "Test" 阶段相关的步骤。
    ⑥ 定义 "Deploy" 阶段。
    ⑦ 执行与 "Deploy" 阶段相关的步骤。