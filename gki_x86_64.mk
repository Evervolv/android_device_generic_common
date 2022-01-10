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

#
# TODO (b/212486689): The minimum system stuff for build pass.
#
$(call inherit-product, $(SRC_TARGET_DIR)/product/core_64_bit.mk)
$(call inherit-product, $(SRC_TARGET_DIR)/product/runtime_libart.mk)

#
# Build GKI boot images
#
include device/generic/common/gki_common.mk
$(call output-kernel,kernel/prebuilts/5.10/arm64,kernel/5.10)


PRODUCT_NAME := gki_x86_64
PRODUCT_DEVICE := gki_x86_64
PRODUCT_BRAND := Android
PRODUCT_MODEL := GKI on x86_64
