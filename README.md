# GSI

This document introduces special GSI settings for facilitating xTS-on-GSI with
a single image.

## Changes in runtime resource overlays (RRO)

### SystemUI overlays

Some devices access the private android framework resource by `@*android:`
while overlaying their SystemUI setting `status_bar_header_height_keyguard`.
However, referencing private framework resource IDs from RRO packages in the
vendor partition crashes on these devices when GSI is used. This is because
private framework resource don't have a stable ID, and these vendor RRO
packages would be referencing to dangling resource references after GSI is
used (b/245806899).

In order to prevent SystemUI crash, GSI adds a runtime resource overlay in
the system_ext partition, which have higher overlay precedence than RROs on
vendor partition, so the problematic vendor RROs would be overridden.

Lifetime of this package:
* Starts at: Android 14.
* Deprecation plan: TBD, depends on b/254581880.
