# Codex Macro

[简体中文](README.zh-CN.md)

Codex Macro is an experimental Android BLE controller compatible with the Codex Micro integration in ChatGPT Desktop for macOS. It presents the phone as a BLE HID-over-GATT peripheral and implements the vendor report protocol used by the reference Core2 firmware.

This project is unofficial and is not affiliated with or endorsed by OpenAI, Work Louder, Google, or Apple. The compatibility protocol is undocumented and may stop working after a ChatGPT Desktop update.

## Requirements

- Android 9 or newer
- A phone with Bluetooth LE peripheral advertising support
- macOS with ChatGPT Desktop and Codex Micro support
- Nearby devices permission

## Build

```shell
./gradlew assembleDebug
```

The APK is generated at `app/build/outputs/apk/debug/app-debug.apk`.

## Pairing

1. Open Codex Macro and tap **Start**.
2. Approve the Nearby devices permission and allow Bluetooth to turn on if requested.
3. On the Mac, open Bluetooth settings and pair with **Codex Micro**.
4. Open ChatGPT Desktop and configure the controller under **Settings > Codex Micro**.
5. If macOS cached an older descriptor, forget the device and pair it again.

By default, the app temporarily changes the Android Bluetooth name to `Codex Micro` while the controller is running and restores the previous name after **Stop**. Enable **Stable connection mode** in the app settings to keep the name, GATT services, and host connection active while controller input is paused. **Auto resume** restores a controller that was still running after process recovery or a device reboot. Both compatibility options are disabled by default.

## Controls

- **Agent Keys:** Agent 1 through Agent 6 with host-provided status colors and breathing effects.
- **Command Keys:** Fast, Approve, Decline, Fork, Mic, and Codex Send.
- **Analog stick:** Four digital directions selected by dragging from the center.
- **Dial:** Press and hold the center, or drag around the outer ring for continuous rotation steps.

The phone does not send text as a conventional Bluetooth keyboard. Command remapping and press-duration behavior are handled by ChatGPT Desktop.

## Q&A

### Q: macOS can see the device, but it does not connect or ChatGPT does not detect Codex Micro. What happened?

macOS may cache the Bluetooth identity, GATT services, and HID report descriptor for a device address. This was encountered during development: macOS could discover the Android device and its Human Interface Device service, but an older cached descriptor or device name prevented a clean Codex Micro connection. Reinstalling the APK does not clear the Mac's Bluetooth cache.

Use this recovery sequence:

1. Tap **Stop** in Codex Macro.
2. In macOS Bluetooth settings, forget every stale **Codex Micro** or corresponding Android device entry.
3. Turn Bluetooth off and on on the Mac. If the stale entry remains, restart the Mac.
4. Reopen Codex Macro, tap **Start**, and wait until it shows **PAIRING**.
5. Confirm the advertised name is **Codex Micro**, then pair it again from macOS.
6. Quit and reopen ChatGPT Desktop after pairing.
7. In macOS **System Settings > Privacy & Security > Input Monitoring**, allow ChatGPT when prompted.

### Q: The app shows PAIRING even though the device is visible on the Mac. Is that connected?

No. **PAIRING** means Android is advertising the BLE HID service and waiting for a GATT host connection. **CONNECTED** means macOS opened the GATT connection. A Bluetooth entry being visible in macOS does not mean the HID service is connected or that ChatGPT has subscribed to its reports.

### Q: macOS pairs successfully, but Codex Micro is missing from ChatGPT settings. What should I check?

- Use a ChatGPT Desktop build that includes Codex Micro support.
- Restart ChatGPT after Bluetooth pairing completes.
- Grant ChatGPT **Input Monitoring** permission on macOS.
- Forget and pair the device again if it was previously paired while advertising under the Android device's original name.

### Q: Why does the device sometimes appear as the phone or tablet name instead of Codex Micro?

Android and macOS may take time to refresh the advertised name, and macOS may continue showing a cached name. Enable **Stable connection mode** if the host repeatedly loses the BLE identity after Stop and Start; while enabled, the adapter remains named **Codex Micro** until the option is disabled.

### Q: The device is discoverable but never reaches CONNECTED. Is every Android device supported?

No. The Android device must support BLE peripheral advertising, and vendor Bluetooth stacks differ in how completely they expose app-owned HID-over-GATT services. Discoverable advertising alone does not guarantee that macOS will enumerate the device as compatible HOGP hardware. Try the cache-reset sequence first; if it still fails, test with another Android device.

## Compatibility notes

Android exposes the HID service through an app-owned GATT server. Bluetooth controller implementations vary by device vendor, so successful advertising does not guarantee that macOS will enumerate every phone as HOGP hardware. The first compatibility target is macOS with ChatGPT Desktop; Windows, USB HID, and generic keyboard hosts are out of scope.

Protocol behavior is based on [imliubo/codex-micro-4-core2](https://github.com/imliubo/codex-micro-4-core2). See [THIRD_PARTY_NOTICES.md](THIRD_PARTY_NOTICES.md) for attribution.

## License

Codex Macro is licensed under the [GNU Lesser General Public License v3.0 only](LICENSE) (`LGPL-3.0-only`).
