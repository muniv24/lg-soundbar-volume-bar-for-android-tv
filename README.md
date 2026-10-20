# Volume Bar for Android TV

A tiny Android TV app (about 20 KB) that shows your **soundbar's volume level on screen** as `Volume 29%` with a bar.

It is for TVs that hide their own volume bar once a soundbar is connected over **HDMI eARC / ARC**, and for soundbars with no number display. Many LG soundbars only show the level as a coloured light (green, yellow, red), so you never know the real number.

## How it works

When a soundbar is connected over HDMI-CEC, it reports its volume (0–100) back to the TV, and Android keeps that as the music volume. The TV often just doesn't draw it. This app listens for volume changes and draws a small box in the top-right corner for 2.5 seconds.

- **Live number while you hold the button.** When you hold volume up or down, the number keeps moving, then settles on the level the soundbar really reports.
- **Hidden on TV speakers.** When sound plays on the TV's own speakers, the TV already shows its bar, so this app stays out of the way.
- **Starts at boot**, with no need to open the app.
- No internet permission, no ads, no tracking.

## Tested on

| TV | Soundbar | Result |
|---|---|---|
| Xiaomi / Redmi Android TV 10 (2021) | LG soundbar on eARC | Works |

If you try it on another TV or soundbar, please open an issue with the result. It should work on any Android TV 8.0+ that receives the soundbar's volume over CEC.

## Install

You need a computer with `adb` and the TV's network debugging turned on (Settings → Device Preferences → About → press *Build* 7 times, then Developer options → USB / network debugging).

```sh
adb connect <TV_IP>:5555
adb install volumebar.apk
# Allow the box to show over other apps (most TVs have no screen for this):
adb shell appops set com.muniv24.volumebar SYSTEM_ALERT_WINDOW allow
```

Then open **Volume Bar** from the TV's apps once, and press **Show a test**. From then on it starts by itself when the TV starts.

To remove it: `adb uninstall com.muniv24.volumebar`, or uninstall it from the TV's Settings → Apps.

## Build

Needs Java 17 and the Android SDK (platform 34, build-tools 34). No Gradle or Android Studio needed:

```sh
sh android/build.sh        # makes android/out/volumebar.apk
```

Keep `android/key.jks`, which the first build makes: updates must be signed with the same key.

## Code

All in `android/src/com/muniv24/volumebar/`:

| File | What it does |
|---|---|
| `SoundbarVolume.java` | Reads the soundbar's level (0–100), mute, and whether sound is on the TV speakers |
| `VolumeBarView.java` | Draws the card with `Volume 29%` and the bar |
| `VolumeService.java` | Listens for volume changes and shows the bar; handles a held button |
| `SettingsActivity.java` | The app's screen: on/off, test, permission |
| `Prefs.java` | The on/off setting |
| `BootReceiver.java` | Starts the bar when the TV starts |

## License

MIT
