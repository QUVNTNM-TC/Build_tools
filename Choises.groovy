    if (Arch.equals('ARM')) {
       return ['6.4_a15','7.2_a15','6.4_a9','7.2_a9','7.2_kryo','8.x-kryo']
    } else if (Arch.equals('DESKTOP')) {
       return ['ivybridge','sandybridge','haswell','westmere']
    }