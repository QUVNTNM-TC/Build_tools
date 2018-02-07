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
        wget https://raw.githubusercontent.com/QUVNTNM-TC/configs_desktop/master/config_$Variant
        mv config_$Variant .config
        echo $Jobs
        sed -i 's@CT_LOCAL_TARBALLS_DIR="${CT_TOP_DIR}/src"@CT_LOCAL_TARBALLS_DIR="${CT_TOP_DIR}/../src"@g' .config
        sed -i 's|CT_PARALLEL_JOBS=5|CT_PARALLEL_JOBS='"$Jobs"'|g' .config
        '''
    }
}

int build(){
    dir(env.BUILD_DIR) {
        cleanUP()
        getCONFIG()
        return sh (returnStatus: true, script: '''#!/usr/bin/env bash
        ct-ng build
        ''')
    }
}

node {
    env
    currentBuild.description = env.Variant
    env.WORKSPACE = '/home/jenkins/workspace/QTC-Desktop'
    env.BUILD_DIR = env.WORKSPACE + '/' + env.Variant
    env.RESULT_DIR = env.BUILD_DIR + '/x-tools/x86_64-qtc-linux-gnu' 
    env.PUSH_DIR = env.WORKSPACE + '/DESKTOP-TC'

    stage('Checkout') {
        checkout([$class: 'GitSCM', branches: [[name: '**']], doGenerateSubmoduleConfigurations: false, extensions: [[$class: 'RelativeTargetDirectory', relativeTargetDir: '/home/jenkins/workspace/QTC-Desktop/DESKTOP-TC'], [$class: 'CloneOption', depth: 3, noTags: true, reference: '', shallow: true, timeout: 20]], submoduleCfg: [], userRemoteConfigs: [[credentialsId: '6f973906-7fd2-4504-9aba-9526eac9fedd', url: 'git@github.com:QUVNTNM-TC/DESKTOP-TC.git']]])
    }

   stage('Build process') {
       ret = build()
       if ( ret != 0 )
       cleanUP()
       error('Build failed!')
    }

   stage('Publishing') {
       dir(env.PUSH_DIR) {
           return sh (returnStatus: true, script: '''#!/usr/bin/env bash
           git checkout ${Variant}
           ''')
       }

      dir(env.PUSH_DIR) {
          echo "Copying files.."
          return sh (returnStatus: true, script: '''#!/usr/bin/env bash
          cd ../
          rm -rf ${PUSH_DIR}/*
          cp -r ${RESULT_DIR}/* ${PUSH_DIR}/
          cd ${PUSH_DIR}
          wget https://raw.githubusercontent.com/QUVNTNM-TC/Build_tools/master/scripts/DESKTOP/link_desk.sh && chmod +x link_desk.sh && ./link_desk.sh
          rm link*
          git add --all
          ''')
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