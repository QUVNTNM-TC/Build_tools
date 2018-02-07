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

        if [ $Arch == "DESKTOP" ]; then
        wget -O .config https://raw.githubusercontent.com/QUVNTNM-TC/configs_desktop/master/config_$Variant 
        elif [ $Arch == "ARM" ]; then
        wget -O .config https://raw.githubusercontent.com/QUVNTNM-TC/configs/master/config_LINARO_$Variant 
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
        cleanUP()
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
          cd ../
          rm -rf ${PUSH_DIR}/*
          cp -r ${RESULT_DIR}/* ${PUSH_DIR}/
          cd ${PUSH_DIR}

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

node {
    env
    currentBuild.description = env.Arch + '/' + env.Variant
    if (env.Arch == "DESKTOP") {
        env.WORKSPACE = '/home/jenkins/workspace/QTC-Desktop'
        env.PUSH_DIR = env.WORKSPACE + '/DESKTOP-TC'
        env.RESULT_DIR = env.BUILD_DIR + '/x-tools/x86_64-qtc-linux-gnu' 
    } else if (env.Arch == "ARM") {
        env.WORKSPACE = '/home/jenkins/workspace/QTC-Arm'
        env.PUSH_DIR = env.WORKSPACE + '/TC'

        if (env.Variant == "7.2_kryo") {
            env.RESULT_DIR = env.BUILD_DIR + '/x-tools/aarch64-QUVNTNM_TOOLCHAIN-linux-gnu'  
        } else if (env.Variant == "8.x_kryo") {
            env.RESULT_DIR = env.BUILD_DIR + '/x-tools/aarch64-QUVNTNM_TOOLCHAIN-linux-gnu'   
        } else {
            env.RESULT_DIR = env.BUILD_DIR + '/x-tools/arm-QUVNTNM_TOOLCHAIN-linux-gnueabihf' 
        }
    }
    env.BUILD_DIR = env.WORKSPACE + '/' + env.Variant

    stage('Checkout') {

        if (env.Arch == 'DESKTOP') {
        checkout([$class: 'GitSCM', branches: [[name: '**']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/QTC-Desktop/DESKTOP-TC'], [$class: 'CloneOption', depth: 3, noTags: true, reference: '', shallow: true, timeout: 20]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '6f973906-7fd2-4504-9aba-9526eac9fedd', url: 'git@github.com:QUVNTNM-TC/DESKTOP-TC.git']]])
        } else if (env.Arch == "ARM") {
        checkout([$class: 'GitSCM', branches: [[name: '**']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/QTC-Arm/TC'], [$class: 'CloneOption', depth: 3, noTags: true, reference: '', shallow: true, timeout: 20]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '6f973906-7fd2-4504-9aba-9526eac9fedd', url: 'git@github.com:QUVNTNM-TC/TC.git']]])
        }
    }

   stage('Build process') {
       ret = build()
       if ( ret != 0 ) {
       cleanUP()
       error('Build failed!')
       }
    }

   stage('Publishing') {
       dir(env.PUSH_DIR) {
           return sh (returnStatus: true, script: '''#!/usr/bin/env bash
           if [ $Arch == "ARM" ]; then
             case "$Variant" in
             6.4_a15) VARCHECK=Q6.4-a15-neon ;;
             7.2_a15) VARCHECK=Q7.2-a15-neon ;;
             6.4_a9) VARCHECK=Q6.4-a9-neon ;;
             7.2_a9) VARCHECK=Q7.2-a9-neon ;;
             7.2_kryo) VARCHECK=Q7.2-kryo-aarch ;;
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
          sshagent (credentials: ['6f973906-7fd2-4504-9aba-9526eac9fedd']) {
          sh('git config --global user.email "sudokamikaze@openmailbox.org" ')
          sh('git config --global user.name "QUVNTNM-Jenkins" ')
          sh('''git commit -m "Bump build of $(date +%d-%m-%Y)"''')
          sh('git push')
          }
      }
   }

   stage('CleanUP') {
        dir(env.BUILD_DIR) {
        cleanUP()
    }
   }
}