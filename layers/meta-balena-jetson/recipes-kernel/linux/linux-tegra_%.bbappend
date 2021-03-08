inherit kernel-resin deploy

FILESEXTRAPATHS_append := ":${THISDIR}/${PN}"
SRC_URI_append = " \
    file://0001-Expose-spidev-to-the-userspace.patch \
    file://0002-mttcan-ivc-enable.patch \
    file://0002-NFLX-2019-001-SACK-Panic.patch \
    file://0003-NFLX-2019-001-SACK-Panic-for-lteq-4.14.patch \
    file://0004-NFLX-2019-001-SACK-Slowness.patch \
    file://0005-NFLX-2019-001-Resour-Consump-Low-MSS.patch \
    file://0006-NFLX-2019-001-Resour-Consump-Low-MSS.patch \
    file://tegra186-tx2-blackboard.dtb \
    file://tegra186-tx2-cti-ASG916.dtb \
    "

BALENA_CONFIGS_append = " compat spi gamepad can tpg"
BALENA_CONFIGS_remove = "brcmfmac"

BALENA_CONFIGS[compat] = " \
    CONFIG_COMPAT=y \
    "

BALENA_CONFIGS[spi] = " \
		CONFIG_SPI=y \
		CONFIG_SPI_MASTER=y \
		CONFIG_SPI_SPIDEV=m \
		"
BALENA_CONFIGS_DEPS[spi] = " \
		CONFIG_QSPI_TEGRA186=y \
		CONFIG_SPI_TEGRA144=y \
		"

BALENA_CONFIGS[gamepad] = " \
		CONFIG_JOYSTICK_XPAD=m \
		"
BALENA_CONFIGS_DEPS[gamepad] = " \
		CONFIG_INPUT_JOYSTICK=y \
		CONFIG_USB_ARCH_HAS_HCD=y \
		"

BALENA_CONFIGS_append_skx2 = " cdc_acm wdm"

BALENA_CONFIGS_DEPS[cdc_acm] = "CONFIG_TTY=y"
BALENA_CONFIGS[cdc_acm] = "CONFIG_USB_ACM=m"

BALENA_CONFIGS[wdm] = "CONFIG_USB_WDM=m"

BALENA_CONFIGS_append_m2pcie-tx2 = " gasket"

BALENA_CONFIGS[gasket] = " \
        CONFIG_STAGING_GASKET_FRAMEWORK=m \
        CONFIG_STAGING_APEX_DRIVER=m \
        "

BALENA_CONFIGS[can] = " \
		CONFIG_CAN=m \
		CONFIG_CAN_RAW=m \
		CONFIG_CAN_DEV=m \
		CONFIG_MTTCAN=m \
		CONFIG_MTTCAN_IVC=m \
"

BALENA_CONFIGS_append_srd3-tx2 = " tpg"

BALENA_CONFIGS[tpg] = " \
		CONFIG_VIDEO_TEGRA_VI_TPG=m \
"

KERNEL_MODULE_AUTOLOAD_srd3-tx2 += "nvhost-vi-tpg"
KERNEL_MODULE_PROBECONF_srd3-tx2 += "nvhost-vi-tpg"

TEGRA_INITRAMFS_INITRD = "0"

KERNEL_ROOTSPEC = "\${resin_kernel_root} ro rootwait"
KERNEL_ROOTSPEC_FLASHER = "root=/dev/mmcblk1p2 ro rootwait"

KERNEL_ROOTSPEC_append_m2pcie-tx2 = " gasket.dma_bit_mask=32"
KERNEL_ROOTSPEC_FLASHER_append_m2pcie-tx2 = " gasket.dma_bit_mask=32"

generate_extlinux_conf() {
    install -d ${D}/${KERNEL_IMAGEDEST}/extlinux
    rm -f ${D}/${KERNEL_IMAGEDEST}/extlinux/extlinux.conf
    rm -f ${D}/${KERNEL_IMAGEDEST}/extlinux/extlinux.conf_flasher
    kernelRootspec="${KERNEL_ROOTSPEC}" ; cat >${D}/${KERNEL_IMAGEDEST}/extlinux/extlinux.conf << EOF
DEFAULT primary
TIMEOUT 30
MENU TITLE Boot Options
LABEL primary
      MENU LABEL primary ${KERNEL_IMAGETYPE}
      LINUX /boot/${KERNEL_IMAGETYPE}
      APPEND ${KERNEL_ARGS} ${kernelRootspec} \${os_cmdline}
EOF
    kernelRootspec="${KERNEL_ROOTSPEC_FLASHER}" ; cat >${D}/${KERNEL_IMAGEDEST}/extlinux/extlinux.conf_flasher << EOF
DEFAULT primary
TIMEOUT 30
MENU TITLE Boot Options
LABEL primary
      MENU LABEL primary ${KERNEL_IMAGETYPE}
      LINUX /${KERNEL_IMAGETYPE}
      APPEND ${KERNEL_ARGS} ${kernelRootspec} \${os_cmdline}
EOF
}

FILES_${KERNEL_PACKAGE_NAME}-image_append = "/boot/extlinux/extlinux.conf /boot/extlinux/extlinux.conf_flasher"

do_deploy_append() {
    mkdir -p "${DEPLOYDIR}"
    install -m 0600 "${D}/boot/extlinux/extlinux.conf" "${DEPLOYDIR}"
    install -m 0600 "${D}/boot/extlinux/extlinux.conf_flasher" "${DEPLOYDIR}"

    cp ${WORKDIR}/tegra186-tx2-cti-ASG916.dtb "${DEPLOYDIR}"
}

do_deploy_append_blackboard-tx2() {
    cp ${WORKDIR}/tegra186-tx2-blackboard.dtb "${DEPLOYDIR}"
}
