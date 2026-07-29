---
name: connecting-android-device
description: Use when work needs the app on real hardware - installing a debug build, checking a screen on device, capturing a screenshot - or when adb reports no device, an unauthorized device, or more than one device/emulator.
---

# Connecting the Android Device

## Overview

The development device is a Pixel 8a (codename `akita`) running Android 17, paired over wireless debugging.

Which device gets targeted comes from `ANDROID_SERIAL` in `.env`, which mise loads automatically and both adb and Gradle honor. `.env` is git-ignored because the value is specific to one machine and phone; `.env.example` documents the format. Nothing else is device-specific, so the tasks work unchanged on another device once `.env` points at it.

Everything routes through mise tasks, and `Bash(mise run *)` is already allowed, so none of these prompt for permission.

## Quick Reference

| Task | Does |
| --- | --- |
| `mise run android-connect` | Waits for the target device, then prints `adb devices -l`. |
| `mise run android-install` | Runs android-connect, then `installDebug`, then launches MainActivity. |
| `mise run android-screenshot [PATH]` | Captures a screenshot. Defaults to `screenshot.png`. |
| `mise run android-status` | Reports wakefulness, keyguard (lock) state, and the foreground app. |

Start with `android-connect`. `android-install` already calls it, so running both is redundant.

## When android-connect Fails

It exits 1 with one of three messages, each pointing at a different fix.

| Message | Means |
| --- | --- |
| `No device found` | USB or wireless debugging is off, the device is unauthorized, or nothing is connected. |
| `... ANDROID_SERIAL is unset` | Several devices are attached and adb cannot pick one. Set `ANDROID_SERIAL` in `.env` to one of the names it lists. |
| `... is not among the attached devices` | `.env` points at a device that is not connected. |

`No device found` can mean USB debugging is off, wireless debugging is off, the device is plugged in but unauthorized, or nothing is connected at all. For the paired Pixel 8a, wireless debugging getting switched off is the most common cause — adb rediscovers a paired device over mDNS on its own. **Fixing it needs the user; it cannot be done from the shell.** Ask them to open 設定 → システム → 開発者向けオプション → ワイヤレスデバッグ and switch it on, or plug in the USB cable and approve the authorization prompt, then re-run. Only if that fails does pairing need redoing, which `docs/running-on-device.md` covers.

Run `android-connect` once, then ask. Do not sit in a retry loop — it already retries for 10 seconds internally.

## Gotchas

- **`more than one device/emulator`.** Wireless debugging registers the device twice, once as `192.168.x.x:PORT` and once as `adb-<serial>-XXXX._adb-tls-connect._tcp`. `ANDROID_SERIAL` resolves this by naming the target, so there is no need to disconnect anything. Prefer the mDNS name in `.env`: the IP port changes every time wireless debugging is toggled, the mDNS name does not.
- **mise's `.env` beats the caller's environment.** `ANDROID_SERIAL=other mise run android-connect` silently uses the `.env` value. To override, run the script directly: `ANDROID_SERIAL=other ./mise-tasks/android-connect`.
- **Screenshots come out black.** `adb exec-out screencap -p` returns a blank image on this device. `android-screenshot` writes the file on the device and pulls it instead. Do not reach for `exec-out`.
- **A black screenshot can also mean the screen is off or locked**, which is not an app bug. Check before concluding anything: `mise run android-status`.

- **The app targets SDK 36 on an API 37 device.** That is a deliberate decision recorded in ADR 0001, not drift to correct.
- **Screenshot coordinates are not device coordinates.** `android-screenshot`'s image is displayed scaled down (e.g. a 1080x2400 device shows as 900x2000), so reading a tap target's position off the displayed image and passing it to `adb shell input tap X Y` misses — the multiplier needed to convert varies per image. Get exact coordinates instead:

  ```bash
  adb shell uiautomator dump /sdcard/ui.xml
  adb shell cat /sdcard/ui.xml | grep -o 'text="TARGET"[^/]*bounds="[^"]*"'
  ```

  `bounds="[x1,y1][x2,y2]"` is in real device pixels; tap the center of that rect.

## Verifying the App Actually Ran

`BUILD SUCCESSFUL` only proves the install succeeded. Confirm the process exists and the activity reached the foreground:

```bash
adb shell pidof com.okkey.fitnesskpitracker
```

`mise run android-status` reports the foreground app (among other things).
