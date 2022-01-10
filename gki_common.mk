#
# Copyright (C) 2022 The Android Open Source Project
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#      http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

# The files will be copy from the source folder to the dist folder
_output-kernel-info-files := \
    prebuilt-info.txt \
    manifest.xml \


# Copy the files from the source folder to the dist folder
#
# Skip if the file is not existing.
#
# $(1): file list
# $(2): the source folder
# $(3): the dist folder
define _output_kernel_files
$(foreach f,$(1), \
  $(if $(wildcard $(2)/$(f)), \
    $(call dist-for-goals,dist_files,$(2)/$(f):$(3)/$(f))))
endef


# Output the release kernel prebuilt files to dist folder
#
# $(1): the source folder contains the kernel prebuilt files
# $(2): the dist folder
#
# Notes:
#   For mainline kernel, it outputs -allsyms kernel as release kernel.
#
define _output-kernel-user
$(if $(findstring mainline,$(1)), \
  $(eval PRODUCT_COPY_FILES += \
    $(foreach f,$(wildcard $(1)/kernel-*-allsyms), \
      $(f):$(subst -allsyms,,$(notdir $(f))))), \
  $(eval PRODUCT_COPY_FILES += \
    $(call copy-files,$(filter-out *-allsyms,$(wildcard $(1)/kernel-*)),.)))
endef


# Output the debug kernel prebuilt files to dist folder
#
# $(1): the source folder contains the kernel prebuilt files
# $(2): the dist folder
#
# Note:
#   For mainline kernel, it outputs -allsyms kernel as release kernel,
#   so there is no need to output -allsyms again.
#
define _output-kernel-debug
$(if $(findstring mainline,$(1)),, \
  $(eval PRODUCT_COPY_FILES += \
    $(call copy-files,$(wildcard $(1)/kernel-*-allsyms),.)))
endef


# Output the kernel prebuilt files to dist folder
#
# $(1): the source folder contains the kernel prebuilt files
# $(2): the dist folder
#
# Example:
#  $(call output-kernel,kernel/prebuilts/5.10/arm64,kernel/5.10)
#
define output-kernel
$(call _output-kernel-user,$(1),$(2))
$(if $(filter userdebug eng,$(TARGET_BUILD_VARIANT)), \
  $(call _output-kernel-debug,$(1),$(2)))
$(call _output_kernel_files,$(_output-kernel-info-files),$(1),$(2))
endef

#
# Output boot.img and init-boot.img
#
$(call inherit-product, $(SRC_TARGET_DIR)/product/generic_ramdisk.mk)
PRODUCT_BUILD_BOOT_IMAGE := true
PRODUCT_BUILD_INIT_BOOT_IMAGE := true

PRODUCT_BUILD_CACHE_IMAGE := false
PRODUCT_BUILD_ODM_IMAGE := false
PRODUCT_BUILD_VENDOR_DLKM_IMAGE := false
PRODUCT_BUILD_ODM_DLKM_IMAGE := false
PRODUCT_BUILD_PRODUCT_IMAGE  := false
PRODUCT_BUILD_RAMDISK_IMAGE := true
PRODUCT_BUILD_SYSTEM_IMAGE := false
PRODUCT_BUILD_SYSTEM_EXT_IMAGE := false
PRODUCT_BUILD_SYSTEM_OTHER_IMAGE := false
PRODUCT_BUILD_USERDATA_IMAGE := false
PRODUCT_BUILD_VENDOR_IMAGE := false
PRODUCT_BUILD_VENDOR_BOOT_IMAGE := false
PRODUCT_BUILD_RECOVERY_IMAGE := false
PRODUCT_BUILD_VBMETA_IMAGE := false
PRODUCT_BUILD_DEBUG_BOOT_IMAGE := false
PRODUCT_BUILD_DEBUG_VENDOR_BOOT_IMAGE := false

MODULE_BUILD_FROM_SOURCE := true
PRODUCT_EXPORT_BOOT_IMAGE_TO_DIST := true
