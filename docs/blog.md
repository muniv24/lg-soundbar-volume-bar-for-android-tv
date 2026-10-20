# My soundbar has no volume number, so I built one

I bought a new LG Dolby Atmos soundbar and connected it to my Xiaomi Android TV over HDMI eARC. The sound is great. But the first time I pressed volume up, nothing appeared on screen.

## The problem

Two things combine here:

1. **The TV hides its volume bar.** Once the soundbar takes over on eARC, my Xiaomi TV stops showing any volume level. Many TVs do the same, or only show a `+` / `−` with no number.
2. **The soundbar has no number display.** LG's LED-only soundbars show volume as coloured lights. LG's own manuals for these models describe it like this:
   - **1–33: green**
   - **34–66: yellow**
   - **67–99: red**
   - All lights blinking red means maximum, blinking orange means mute.

So "yellow" could be 35 or 65. At night that's the difference between fine and waking everyone up.

It isn't just me. People report the same missing volume number on Sony, Samsung, TCL, Hisense and LG TVs with soundbars:

- [TCL Google TV: volume levels no longer displayed over HDMI ARC](https://www.avsforum.com/threads/volume-levels-no-longer-displayed-when-using-hdmi-arc-for-audio.3322803/), AVS Forum
- [Can the LG CX display volume numbers while using eARC?](https://www.avforums.com/threads/can-the-lg-cx-display-volume-numbers-while-using-earc.2395546/), AVForums
- [Volume number not showing (Samsung, eARC)](https://eu.community.samsung.com/t5/audio-video/volume-number-not-showing/td-p/2583872), Samsung Community
- [No on-screen volume with Theatre Bar 8 on a Sony TV](https://community.sony.ch/t5/other-tvs/volume-on-screen-display-not-shown-using-theatre-bar-8-on-sony/td-p/3954606), Sony Community
- [Sony Android TV with ARC soundbar: no volume numbers](https://community.sony.bg/t5/android-tv/kdl-65w855c-arc-query/m-p/2225783), Sony Community
- [Hisense update removed the numbered volume slider](https://www.hometheaterforum.com/community/threads/hisense-65u6g-volume-control-following-update.379782/), Home Theater Forum

## Hasn't anyone fixed it?

I looked, and I couldn't find a fix that works for my TV:

- **Android itself can do it.** Newer Android TV versions have "absolute volume" over HDMI-CEC, where the TV asks the soundbar for its level and shows it ([Android docs](https://source.android.com/docs/core/audio/soundbar)). But it's up to each TV maker to ship it, and my 2021 Xiaomi TV doesn't.
- **Sony** added a soundbar volume display, then [took part of it back](https://www.sony.co.in/electronics/support/articles/00380212) because of false pop-ups.
- I found **no LG soundbar firmware update** and **no Xiaomi update** that fixes it, and no app on the Play Store that does it.

## The fix: a 20 KB app

The surprising part: **the TV already knows the volume.** Over HDMI-CEC the soundbar reports its level (0–100) back to the TV, and Android stores it. The TV just doesn't draw it.

So I built a tiny app that draws it: **Volume Bar for Android TV**.

- Press volume and a small box shows `Volume 29%` with a bar, top right, for 2.5 seconds.
- Hold the button and the number keeps moving, then settles on what the soundbar really reports.
- When sound plays on the TV's own speakers, it stays hidden, because the TV shows its own bar.
- It starts when the TV starts. No ads, no tracking, and it never goes on the internet.

It's free and open source: **[github.com/muniv24/lg-soundbar-volume-bar-for-android-tv](https://github.com/muniv24/lg-soundbar-volume-bar-for-android-tv)**

## Try it

No computer needed: you download it on the TV with the free Downloader app, and the app sets up its own permission. The [README](../README.md) has the steps.

I've tested it on a Xiaomi / Redmi Android TV with an LG soundbar. If you have another TV or soundbar, please try it and [open an issue](https://github.com/muniv24/lg-soundbar-volume-bar-for-android-tv/issues) with the result. That's how this gets better for everyone.

---

*Dear LG and Xiaomi: the volume is right there. Please show it.*
