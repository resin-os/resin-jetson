include balena-image.inc

BALENA_BOOT_PARTITION_FILES_append = " extlinux.conf_flasher:/extlinux/extlinux.conf"

# Switch extlinux.conf to the flasher one
add_extlinux_to_flasher_rootfs() {
    cp ${DEPLOY_DIR_IMAGE}/extlinux.conf_flasher ${WORKDIR}/rootfs/boot/extlinux/extlinux.conf
    sed -i 's/Image/boot\/Image/g' ${WORKDIR}/rootfs/boot/extlinux/extlinux.conf
}

IMAGE_PREPROCESS_COMMAND += " add_extlinux_to_flasher_rootfs; "
