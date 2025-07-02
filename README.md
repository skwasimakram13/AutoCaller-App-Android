# 📞 AutoCaller App (Android)

**AutoCaller** is an Android app designed for businesses like CSPs, Reatil shop owners, or field agents who need to call a large number of customers automatically using pre-recorded voice messages. This app allows you to initiate calls from your own device and plays a selected voice message when the call is picked up by the recipient.

---

## ✨ Features

- 📂 Import contacts from CSV files (multiple numbers supported)
- 🔊 Play your pre-recorded audio automatically after the call is picked
- 📞 Uses your own SIM and default dialer to make calls
- ✅ Automatically retries failed calls up to 2 times
- 🛑 Stop calling anytime or skip to the next contact manually
- 🗂 Export call logs (CSV) with status and retry count
- 🔒 No server involved, all data stays on your phone

---

## 🛠 Permissions Required

- `CALL_PHONE` - To make phone calls from your device
- `READ_PHONE_STATE` - To monitor call status (picked, ended)
- `READ_EXTERNAL_STORAGE` / `READ_MEDIA_AUDIO` - To read the audio file
- `FOREGROUND_SERVICE` - To run the calling in background

---

## 📁 CSV Format for Contacts

The CSV should be a simple plain text `.csv` file with phone numbers. For example:

```
9876543210
9123456789
9998887777
```

Only digits are extracted automatically, so formatting like `+91-98765 43210` is also accepted.

---

## ▶️ How to Use

1. **Install the APK** on an Android device (Android 7+ recommended)
2. **Open the app** and grant all required permissions
3. **Select an audio file** (MP3/WAV) from your storage
4. **Import contacts** from a CSV file
5. **Click Start Calling** to begin automated calls
6. Use **Skip** or **Stop** buttons as needed
7. Export logs from the **Export Log** button

---

## 🚫 Limitations

- Audio playback works only after detecting call is connected (OFFHOOK)
- Cannot detect if call is picked by human vs IVR system
- Cannot hang up the call (restricted by Android for non-system apps)
- Call state detection may vary slightly depending on Android version and phone model

---

## 📋 Technologies Used

- Java (Android SDK)
- MediaPlayer API for audio playback
- BroadcastReceiver for phone state monitoring
- ForegroundService for background calling loop

---

## 📦 Project Structure

```
📁 app/
 ┣ 📄 MainActivity.java
 ┣ 📄 CallService.java
 ┣ 📄 AudioPlayer.java
 ┣ 📁 res/
 ┣ 📄 AndroidManifest.xml
 ┗ 📄 README.md
```

---

## Badges
[![trophy](https://github-profile-trophy.vercel.app/?username=ryo-ma)](https://github.com/ryo-ma/github-profile-trophy)

---

## Author
**Develope By** - [Sk Wasim Akram](https://github.com/skwasimakram13)

- 👨‍💻 All of my projects are available at [https://skwasimakram.com](https://skwasimakram.com)

- 📝 I regularly write articles on [https://blog.skwasimakram.com](https://blog.skwasimakram.com)

- 📫 How to reach me **hello@skwasimakram.com**

- 🧑‍💻 Google Developer Profile [https://g.dev/skwasimakram](https://g.dev/skwasimakram)

- 📲 LinkedIn [https://www.linkedin.com/in/sk-wasim-akram](https://www.linkedin.com/in/sk-wasim-akram)

---

## 🤝 Contribution

Pull requests, feature suggestions, and bug reports are welcome.

---

## 📄 License

This project is under the [MIT License](LICENSE).
