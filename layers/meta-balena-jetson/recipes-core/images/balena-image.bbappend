include balena-image.inc

IMAGE_FSTYPES_append = " hostapp-ext4"

BALENA_BOOT_PARTITION_FILES_append  = " extlinux.conf:/extlinux/extlinux.conf"

DEVICE_SPECIFIC_SPACE = "49152"

do_image_balenaos-img[depends] += " tegra-binaries-prepare:do_deploy" 

device_specific_configuration() {
    partitions=$(cat ${DEPLOY_DIR_IMAGE}/tegra-binaries/partition_specification.txt)

    START=${BALENA_IMAGE_ALIGNMENT}
    for n in ${partitions}; do
      part_name=$(echo $n | cut -d ':' -f 1)
      file_name=$(echo $n | cut -d ':' -f 2)
      END=$(expr ${START} \+ ${BALENA_IMAGE_ALIGNMENT})
      parted -s ${BALENA_RAW_IMG} unit KiB mkpart $part_name ${START} ${END}
      dd if=$(find ${DEPLOY_DIR_IMAGE}/tegra-binaries -name $file_name) of=${BALENA_RAW_IMG} conv=notrunc seek=1 bs=$(expr 1024 \* ${START})
      START=${END}
    done
}

IMAGE_INSTALL_append_jetson-tx2 = " tegra-brcm-patchram bt-scripts"
