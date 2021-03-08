FILESEXTRAPATHS_prepend := "${THISDIR}/${PN}:"

UBOOT_KCONFIG_SUPPORT = "1"

inherit resin-u-boot

BALENA_BOOT_PART = "0xC"
BALENA_DEFAULT_ROOT_PART = "0xD"

SRC_URI_append = " \
  file://0004-u-boot-Increase-env-size.patch \
  file://0001-menu-Use-default-menu-entry-from-extlinux.conf.patch \
  file://tx2-Integrate-with-Balena-u-boot-environment.patch \
  file://0001-load-extlinux-from-active-rootfs.patch \
"
