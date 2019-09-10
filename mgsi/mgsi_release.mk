#
# Copyright (C) 2019 The Android Open-Source Project
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

#
# The makefile contains the special settings for MGSI.
# This makefile is used for the products/targets to release MGSI.
#
# For example:
# - MGSI contains skip_mount.cfg to skip mounting prodcut paritition
# - MGSI contains more VNDK packages to support old version vendors
# - etc.
#

# Exclude MGSI specific files
PRODUCT_ARTIFACT_PATH_REQUIREMENT_WHITELIST += \
    system/etc/init/config/skip_mount.cfg \
    system/etc/init/init.mgsi.rc \

# Exclude all files under system/product and system/system_ext
PRODUCT_ARTIFACT_PATH_REQUIREMENT_WHITELIST += \
    system/product/% \
    system/system_ext/%

# apex is not available before Q
# Properties set in system (here) take precedence over those in vendor.
PRODUCT_PRODUCT_PROPERTIES += \
    ro.apex.updatable=false

# Split selinux policy
PRODUCT_FULL_TREBLE_OVERRIDE := true

# Enable dynamic partition size
PRODUCT_USE_DYNAMIC_PARTITION_SIZE := true

# Needed by Pi newly launched device to pass VtsTrebleSysProp on MGSI
PRODUCT_COMPATIBLE_PROPERTY_OVERRIDE := true

# MGSI specific tasks on boot
PRODUCT_COPY_FILES += \
    device/generic/common/mgsi/skip_mount.cfg:system/etc/init/config/skip_mount.cfg \
    device/generic/common/mgsi/init.mgsi.rc:system/etc/init/init.mgsi.rc \

# Support additional P and Q VNDK packages
PRODUCT_EXTRA_VNDK_VERSIONS := 28 29

# The 64 bits MGSI build targets inhiert core_64_bit.mk to enable 64 bits and
# include the init.zygote64_32.rc.
# 64 bits MGSI needs to include different zygote settings for vendor.img to
# select its preference by setting ro.zygote to zygote64_32 or zygote32_64:
#   1. 64-bit primary, 32-bit secondary, or
#   2. 32-bit primary, 64-bit secondary
# Here includes the init.zygote32_64.rc if it had inhierted core_64_bit.mk.
ifeq (true|true,$(TARGET_SUPPORTS_32_BIT_APPS)|$(TARGET_SUPPORTS_64_BIT_APPS))
PRODUCT_COPY_FILES += \
    system/core/rootdir/init.zygote32_64.rc:root/init.zygote32_64.rc

PRODUCT_ARTIFACT_PATH_REQUIREMENT_WHITELIST += \
    root/init.zygote32_64.rc
endif
