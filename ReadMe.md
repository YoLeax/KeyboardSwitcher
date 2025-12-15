# Android Keyboard Switcher

<img alt="Logo" src="https://gitlab.com/kunzisoft/Android-KeyboardSwitcher/raw/master/art/icon.png"> Keyboard Switcher is a **virtual keyboard selection application**, it allows to switch easily from the current keyboard to another in several ways, thanks to a discreet floating button available on the screen, a widget, a tile, or a notification.

<img alt="Screenshot" src="https://gitlab.com/kunzisoft/Android-KeyboardSwitcher/raw/master/art/screen.jpg" width="220">

## Ways to switch keyboards

Keyboard Switcher allows you to open the system dialog box to change the virtual keyboard in several ways:
- A **system tile** accessible from your device's status bar
- A **widget** with an icon size 1*1
- A **floating button** visible above other applications
- A **notification** accessible in the notification list
- From **another app** using the specific intent

### From a third party app

#### Keyboard switch dialog

From an external application, it is possible to call keyboard switcher to easily change the current keyboard to the desired keyboard using the intent `com.android.keyboard.SWITCH_KEYBOARD`.

```
Intent("com.android.keyboard.SWITCH_KEYBOARD").apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
```

#### Auto switch

By default, the dialog box for selecting the keyboard will be displayed, but you can request to switch to a specific keyboard by enabling security permissions and calling the intent with the KEYBOARD_ID parameter containing the ID of the keyboard to switch to.

1. Enable developer mode on your Android device _(usually by pressing the build parameter 7 times in your OS settings)_
2. Connect your phone to your computer with a high-quality USB cable
3. Install ADB on your computer
4. Run the following command to enable elevated security privileges for the Keyboard Switcher application:
    ```
    adb shell pm grant com.kunzisoft.keyboard.switcher android.permission.WRITE_SECURE_SETTINGS
    ```
5. Use the call to this intent from your application with the KEYBOARD_ID of the desired keyboard to automatically switch to that keyboard.:
    ```
    Intent("com.android.keyboard.SWITCH_KEYBOARD").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("KEYBOARD_ID", getMagikeyboardId(context))
    }
    ```

## Is it really free?

Yes, Keyboard Switcher is under **free license (GPLv3+)** and **without advertising**. You can have a look at its full source.

## Contributions

You can contribute in different ways to help me on my work.

* Add features by a **[merge request](https://docs.gitlab.com/ee/gitlab-basics/add-merge-request.html)**.
* **[Donate](https://www.kunzisoft.com/donation)**  ♥‿♥ to support me.

## Download

*We recommend the installation from [F-Droid](https://f-droid.org/) which verifies that all libraries and application code are open source.*

[<img src="https://f-droid.org/badge/get-it-on.png"
      alt="Get it on F-Droid"
      height="80">](https://f-droid.org/en/packages/com.kunzisoft.keyboard.switcher/)

[<img src="https://play.google.com/intl/en_us/badges/images/generic/en_badge_web_generic.png"
      alt="Get it on Google Play"
	height="80">](https://play.google.com/store/apps/details?id=com.kunzisoft.keyboard.switcher)

## License

 Copyright (c) 2025 Jeremy Jamet / [Kunzisoft](https://www.kunzisoft.com).

 This file is part of Keyboard Switcher.

  Keyboard Switcher is free software: you can redistribute it and/or modify
  it under the terms of the GNU General Public License as published by
  the Free Software Foundation, either version 3 of the License, or
  (at your option) any later version.

  Keyboard Switcher is distributed in the hope that it will be useful,
  but WITHOUT ANY WARRANTY; without even the implied warranty of
  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
  GNU General Public License for more details.

  You should have received a copy of the GNU General Public License
  along with Keyboard Switcher.  If not, see <http://www.gnu.org/licenses/>.
