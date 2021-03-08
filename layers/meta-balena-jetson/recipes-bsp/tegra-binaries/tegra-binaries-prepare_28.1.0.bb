SUMMARY = "Prepare bsp binaries for flashing"

LICENSE = "Apache-2.0"
LIC_FILES_CHKSUM = "file://${BALENA_COREBASE}/COPYING.Apache-2.0;md5=89aea4e17d99a7cacdbeed46a0096b10"

DEPENDS = "virtual/bootloader virtual/kernel tegra-binaries"

inherit deploy
SRC_URI = " \
    file://flash.xml \
    file://partition_specification.txt \
    "

SHARED = "${TMPDIR}/work-shared/L4T-${SOC_FAMILY}-${PV}-${PR}/Linux_for_Tegra"
B = "${WORKDIR}/build"
S = "${WORKDIR}"

DTB_blackboard-tx2 = "${DEPLOY_DIR_IMAGE}/tegra186-tx2-blackboard.dtb"
DTB_skx2 = "${DEPLOY_DIR_IMAGE}/tegra186-tx2-cti-ASG916.dtb"

BOOT0 = "boot0.img"
BINARY_INSTALL_PATH = "/opt/tegra-binaries"

do_configure() {
    dtb_name=$(basename ${DTB} | cut -d '.' -f 1)
    sed -i -e "s/\[DTB_NAME\]/$dtb_name/g" ${WORKDIR}/flash.xml
    sed -i -e "s/\[DTB_NAME\]/$dtb_name/g" ${WORKDIR}/partition_specification.txt
}

do_compile() {
    tegraflash="${SHARED}/bootloader/tegraflash.py"

    files=" \
        ${SHARED}/bootloader/mce_mts_d15_prod_cr.bin \ 
        ${SHARED}/bootloader/cboot.bin \
        ${SHARED}/bootloader/tos.img \
        ${SHARED}/bootloader/eks.img \
        ${SHARED}/bootloader/bpmp.bin \
        ${SHARED}/bootloader/camera-rtcpu-sce.bin \
        ${SHARED}/bootloader/t186ref/warmboot.bin \
        ${SHARED}/bootloader/t186ref/tegra186-a02-bpmp-quill-p3310-1000-c01-00-te770d-ucm2.dtb \
        ${DTB} \
        \
        ${SHARED}/bootloader/t186ref/BCT/P3310_A00_8GB_Samsung_8GB_lpddr4_204Mhz_A02_l4t.cfg \
        ${SHARED}/bootloader/t186ref/BCT/tegra186-mb1-bct-misc-si-l4t.cfg \
        ${SHARED}/bootloader/t186ref/BCT/tegra186-mb1-bct-pinmux-quill-p3310-1000-c03.cfg \
        ${SHARED}/bootloader/t186ref/BCT/tegra186-mb1-bct-pmic-quill-p3310-1000-c03.cfg \
        ${SHARED}/bootloader/t186ref/BCT/tegra186-mb1-bct-pad-quill-p3310-1000-c03.cfg \
        ${SHARED}/bootloader/t186ref/BCT/tegra186-mb1-bct-prod-quill-p3310-1000-c03.cfg \
        ${SHARED}/bootloader/t186ref/BCT/tegra186-mb1-bct-bootrom-quill-p3310-1000-c03.cfg \
        ${SHARED}/bootloader/t186ref/BCT/mobile_scr.cfg \
        ${SHARED}/bootloader/t186ref/BCT/minimal_scr.cfg \
        ${SHARED}/bootloader/t186ref/BCT/emmc.cfg \
        \
        ${SHARED}/bootloader/t186ref/nvtboot.bin \
        ${SHARED}/bootloader/nvtboot_cpu.bin \
        ${SHARED}/bootloader/mb1_recovery_prod.bin \
        ${SHARED}/bootloader/nvtboot_recovery.bin \
        ${SHARED}/bootloader/preboot_d15_prod_cr.bin \
        ${SHARED}/bootloader/mce_mts_d15_prod_cr.bin \
        ${SHARED}/bootloader/mb1_prod.bin \
        ${SHARED}/bootloader/spe.bin \
        ${SHARED}/bootloader/nvtboot_recovery_cpu.bin \
        "

    for file in $files; do
        cp $file ${B}
    done

    cp ${WORKDIR}/flash.xml ${B}/flash.xml
    
    ${tegraflash} --bl nvtboot_recovery_cpu.bin --sdram_config P3310_A00_8GB_Samsung_8GB_lpddr4_204Mhz_A02_l4t.cfg --odmdata 0x1090000 --applet mb1_recovery_prod.bin --cmd "flash" --cfg flash.xml --chip 0x18 --misc_config tegra186-mb1-bct-misc-si-l4t.cfg --pinmux_config tegra186-mb1-bct-pinmux-quill-p3310-1000-c03.cfg --pmic_config tegra186-mb1-bct-pmic-quill-p3310-1000-c03.cfg --pmc_config tegra186-mb1-bct-pad-quill-p3310-1000-c03.cfg --prod_config tegra186-mb1-bct-prod-quill-p3310-1000-c03.cfg --scr_config minimal_scr.cfg --scr_cold_boot_config mobile_scr.cfg --br_cmd_config tegra186-mb1-bct-bootrom-quill-p3310-1000-c03.cfg --dev_params emmc.cfg  --bins "mb2_bootloader nvtboot_recovery.bin; mts_preboot preboot_d15_prod_cr.bin; mts_bootpack mce_mts_d15_prod_cr.bin; bpmp_fw bpmp.bin; bpmp_fw_dtb $(basename ${DTB}); tlk tos.img; eks eks.img; bootloader_dtb tegra186-quill-p3310-1000-c03-00-base.dtb" --keep & export _PID=$! ; wait ${_PID} || true

    # Create boot table
    # BCT
    cat ${B}/${_PID}/br_bct_BR.bct > ${B}/${BOOT0}
    cat ${B}/${_PID}/br_bct_BR.bct >> ${B}/${BOOT0}
    dd if=/dev/zero bs=16 count=576 >> ${B}/${BOOT0}
    cat ${B}/${_PID}/br_bct_BR.bct >> ${B}/${BOOT0}
    dd if=/dev/zero bs=16 count=800 >> ${B}/${BOOT0}

    # MB1
    cat ${B}/${_PID}/mb1_prod.bin.encrypt >> ${B}/${BOOT0}
    dd if=/dev/zero bs=16 count=10480 >> ${B}/${BOOT0}

    # MB1 BCT
    cat ${B}/${_PID}/mb1_cold_boot_bct_MB1_sigheader.bct.encrypt >> ${B}/${BOOT0}
    dd if=/dev/zero bs=16 count=969 >> ${B}/${BOOT0}

    # SPE
    cat ${B}/${_PID}/spe_sigheader.bin.encrypt >> ${B}/${BOOT0}
    dd if=/dev/zero bs=16 count=1 >> ${B}/${BOOT0}
    dd if=${B}/${_PID}/mb1_prod.bin bs=16 count=5 skip=1 >> ${B}/${BOOT0}
    dd if=/dev/zero bs=16 count=3840 >> ${B}/${BOOT0}
    
    # MB2
    cat ${B}/${_PID}/nvtboot_sigheader.bin.encrypt >> ${B}/${BOOT0}
    dd if=/dev/zero bs=1 count=4 >> ${B}/${BOOT0}
    dd if=${B}/${_PID}/spe_sigheader.bin.encrypt bs=1 skip=4 count=476 >> ${B}/${BOOT0}
    dd if=/dev/zero bs=16 count=10240 >> ${B}/${BOOT0}

    # MTS-PREBOOT
    cat ${B}/${_PID}/preboot_d15_prod_cr_sigheader.bin.encrypt >> ${B}/${BOOT0}
    dd if=/dev/zero bs=1 count=8 >> ${B}/${BOOT0}
    dd if=${B}/${_PID}/nvtboot_sigheader.bin.encrypt bs=8 skip=1 count=59 >> ${B}/${BOOT0}
    dd if=/dev/zero bs=16 count=211168 >> ${B}/${BOOT0}

    mkdir -p ${B}/out
    cp -r ${B}/${_PID}/* ${B}/out
    rm -rf ${B}/${_PID}
}

do_install() {
    install -d ${D}/${BINARY_INSTALL_PATH}

    files=$(cat ${WORKDIR}/partition_specification.txt | grep -v :u-boot)

    for file in $files; do
        file_name=$(echo $file | cut -d ':' -f 2)
        cp ${B}/out/$file_name ${D}/${BINARY_INSTALL_PATH}
    done

    cp ${WORKDIR}/partition_specification.txt ${D}/${BINARY_INSTALL_PATH}
    cp ${B}/${BOOT0} ${D}/${BINARY_INSTALL_PATH}

    cp ${DEPLOY_DIR_IMAGE}/u-boot-dtb.bin ${D}/${BINARY_INSTALL_PATH}

    # extlinux will now be installed in the rootfs,
    # near the kernel, initrd is not used
    install -d ${D}/boot/extlinux
    install -m 0644 ${DEPLOY_DIR_IMAGE}/extlinux.conf ${D}/boot/extlinux/extlinux.conf
}

do_deploy() {
    mkdir -p ${DEPLOYDIR}/$(basename ${BINARY_INSTALL_PATH})
    cp -r ${D}/${BINARY_INSTALL_PATH}/* ${DEPLOYDIR}/$(basename ${BINARY_INSTALL_PATH})
}

FILES_${PN} += " \
    ${BINARY_INSTALL_PATH} \
    /boot/extlinux/ \
"

INHIBIT_PACKAGE_STRIP = "1"
INHIBIT_PACKAGE_DEBUG_SPLIT = "1"

do_configure[depends] += "tegra-binaries:do_preconfigure"
do_compile[depends] += "virtual/kernel:do_deploy"
do_install[depends] += "virtual/kernel:do_deploy"
do_populate_lic[depends] += "tegra-binaries:do_unpack"

addtask do_deploy before do_package after do_install
