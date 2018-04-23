int cleanUP() {
    echo "Cleaning..."
    dir(env.BUILD_DIR) {
            return sh (returnStatus: true, script: '''#!/usr/bin/env bash
            ct-ng clean
            rm -rf x-tools
            rm .config*
        ''')
    }
}

int getCONFIG() {
    echo "Downloading config..."
    dir(env.BUILD_DIR) {
        sh '''#!/usr/bin/env bash

        if [ $Experimental == "true" ]; then
        
        if [ $Arch == "DESKTOP" ]; then
        wget -O .config http://10.7.0.20:3000/sudokamikaze/configs_experimental/raw/master/config_$Variant 
        elif [ $Arch == "ARM" ]; then
        wget -O .config http://10.7.0.20:3000/sudokamikaze/configs_experimental/raw/master/config_LINARO_$Variant 
        fi
        
        else
        
        if [ $Arch == "DESKTOP" ]; then
        wget -O .config https://raw.githubusercontent.com/QUVNTNM-TC/configs_desktop/master/config_$Variant 
        elif [ $Arch == "ARM" ]; then
        wget -O .config https://raw.githubusercontent.com/QUVNTNM-TC/configs/master/config_LINARO_$Variant 
        fi

        fi

        sed -i 's@CT_LOCAL_TARBALLS_DIR="${CT_TOP_DIR}/src"@CT_LOCAL_TARBALLS_DIR="${CT_TOP_DIR}/../src"@g' .config
        if [ "$Jobs" != 5 ]; then
            sed -i 's|CT_PARALLEL_JOBS=5|CT_PARALLEL_JOBS='"$Jobs"'|g' .config
        fi
        '''
    }
}

int build() {
    dir(env.BUILD_DIR) {
        getCONFIG()
        return sh (returnStatus: true, script: '''#!/usr/bin/env bash
        ct-ng build
        ''')
    }
}

int Publish() {
      dir(env.PUSH_DIR) {
          echo "Copying files.."
          return sh (returnStatus: true, script: '''#!/usr/bin/env bash
          rm -rf $PUSH_DIR/*
          cp -r $RESULT_DIR/*/* $PUSH_DIR/
          cd $PUSH_DIR/

          if [ $Arch == "DESKTOP" ]; then
          wget https://raw.githubusercontent.com/QUVNTNM-TC/Build_tools/master/scripts/DESKTOP/link_desk.sh && chmod +x link_desk.sh && ./link_desk.sh
          elif [ $Arch == "ARM" ]; then
          case "$Variant" in
            "7.2_kryo"|"8.x_kryo") wget https://raw.githubusercontent.com/QUVNTNM-TC/Build_tools/master/scripts/ARM/link_kryo.sh && chmod +x link_kryo.sh && ./link_kryo.sh ;;
            *) wget https://raw.githubusercontent.com/QUVNTNM-TC/Build_tools/master/scripts/ARM/link_cortex.sh && chmod +x link_cortex.sh && ./link_cortex.sh ;;
          esac
          fi

          rm link*
          git add --all
          ''')
      }
}

node(env.Host) {
    env
    currentBuild.description = env.Arch + '/' + env.Variant
    if (env.Experimental == "true") {
        currentBuild.description = currentBuild.description + "/EXPERIMENTAL"
    }

    if (env.Jobs != "5") {
        currentBuild.description = currentBuild.description + "/" + "Threads=" + env.Jobs
    }

    if (env.Arch == "DESKTOP") {
        env.WORKSPACE = '/home/jenkins/workspace/QTC-Desktop'
        if (env.Experimental == "true") {
            env.PUSH_DIR = env.WORKSPACE + '/DESKTOP-TC-EX'
        } else {
            env.PUSH_DIR = env.WORKSPACE + '/DESKTOP-TC'
        }
    } else if (env.Arch == "ARM") {
        env.WORKSPACE = '/home/jenkins/workspace/QTC-Arm'
        if (env.Experimental == "true") {
            env.PUSH_DIR = env.WORKSPACE + '/TC-EX'
        } else {
        env.PUSH_DIR = env.WORKSPACE + '/TC'
        }
    }

    env.BUILD_DIR = env.WORKSPACE + '/' + env.Variant
    env.RESULT_DIR = env.BUILD_DIR + '/x-tools' 

    stage('Checkout') {
        if (env.Arch == 'DESKTOP') {
            if (env.Experimental == 'true') {
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '**']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/QTC-Desktop/DESKTOP-TC-EX'], [$class: 'CloneOption', noTags: true, reference: '', shallow: true, timeout: 25]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '01481822-0d30-47db-b97a-9990399ced23', url: 'ssh://git@10.7.0.20:1935/sudokamikaze/DESKTOP-TC.git']]]
            } else {
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '**']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/QTC-Desktop/DESKTOP-TC'], [$class: 'CloneOption', noTags: true, reference: '', shallow: true, timeout: 25]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '01481822-0d30-47db-b97a-9990399ced23', url: 'git@github.com:QUVNTNM-TC/DESKTOP-TC.git']]]
            } 
            // Clone Linaro sources to reduce space
            checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: 'gcc-7-branch']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'CheckoutOption', timeout: 20], [$class: 'CloneOption', noTags: true, reference: '', shallow: true, timeout: 20], [$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/QTC-Desktop/src/GCC']], submoduleCfg: [], userRemoteConfigs: [[url: 'http://git.linaro.org/toolchain/gcc.git']]]
        } else if (env.Arch == 'ARM') {
            if (env.Experimental == 'true') {
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '**']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/QTC-Arm/TC-EX'], [$class: 'CloneOption', noTags: true, reference: '', shallow: true, timeout: 25]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '01481822-0d30-47db-b97a-9990399ced23', url: 'ssh://git@10.7.0.20:1935/sudokamikaze/TC.git']]]
            } else {
                checkout changelog: false, poll: false, scm: [$class: 'GitSCM', branches: [[name: '**']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/QTC-Arm/TC'], [$class: 'CloneOption', noTags: true, reference: '', shallow: true, timeout: 25]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '01481822-0d30-47db-b97a-9990399ced23', url: 'git@github.com:QUVNTNM-TC/TC.git']]]
            }
        }
    }

   stage('CleanBefore') {
        dir(env.BUILD_DIR) {
            cleanUP()
        }
   }

   stage('Build process') {
       ret = build()
       if ( ret != 0 ) {
       cleanUP()
       error('Build failed!')
       }
    }

    stage('Testing') {
        agent {
            docker { image 'testing-image' }
        }
        steps {
            return sh (returnStatus: true, script: '''#!/usr/bin/env bash
            make DG_TOOLNAME=g++ DG_TARGET_HOSTNAME=127.0.0.1 DG_TARGET_USERNAME=root
            make DG_TOOLNAME=gcc DG_TARGET_HOSTNAME=127.0.0.1 DG_TARGET_USERNAME=root
            ''')
        }
    }

   stage('Publishing') {
       dir(env.PUSH_DIR) {
           return sh (returnStatus: true, script: '''#!/usr/bin/env bash
           if [ $Arch == "ARM" ]; then
             case "$Variant" in
             6.4_a15) VARCHECK=Q6.4-a15-neon ;;
             7.3_a15) VARCHECK=Q7.3-a15-neon ;;
             6.4_a9) VARCHECK=Q6.4-a9-neon ;;
             7.3_a9) VARCHECK=Q7.3-a9-neon ;;
             7.3_kryo) VARCHECK=Q7.3-kryo-aarch ;;
             8.x_kryo) VARCHECK=Q8.0-kryo-aarch ;;
             esac
             git checkout $VARCHECK
            elif [ $Arch == "DESKTOP" ]; then
             git checkout $Variant
            fi
           ''')
       }

       aret = Publish()
       if ( aret != 0 ) {
       cleanUP()
       error('Publish failed!')
       }
    
      dir(env.PUSH_DIR) {
          echo "Uploading into github"
          sshagent (credentials: ['01481822-0d30-47db-b97a-9990399ced23']) {
          sh('git config --global user.email "sudokamikaze@openmailbox.org" ')
          sh('git config --global user.name "QUVNTNM-Jenkins" ')
          sh('''git commit -m "Bump build of $(date +%d-%m-%Y)"''')
          sh('git push')
          }
      }
   }

   stage('CleanAfter') {
        dir(env.BUILD_DIR) {
            cleanUP()
        }
    }
}
