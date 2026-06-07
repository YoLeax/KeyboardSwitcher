# Android Keyboard Switcher

<img alt="Logo" src="https://gitlab.com/kunzisoft/Android-KeyboardSwitcher/raw/master/art/icon.png"> Keyboard Switcher 是一款用于**选择和切换 Android 虚拟键盘**的应用。你可以通过屏幕上的浮动按钮、桌面小部件、快捷设置磁贴、通知，或第三方应用 Intent 打开系统键盘选择器。

<img alt="Screenshot" src="https://gitlab.com/kunzisoft/Android-KeyboardSwitcher/raw/master/art/screen.jpg" width="220">

## 切换键盘的方式

Keyboard Switcher 支持通过多种入口打开系统虚拟键盘选择对话框：

- 从状态栏使用**快捷设置磁贴**
- 使用 1x1 图标大小的**桌面小部件**
- 使用显示在其他应用上方的**浮动按钮**
- 从通知列表点击**通知**
- 从**第三方应用**发送指定 Intent

## 浮动按钮

浮动按钮可用于快速切换键盘或打开系统键盘选择器。

可配置项包括：

- 启用或关闭浮动按钮
- 仅在软键盘打开时显示浮动按钮
- 锁定浮动按钮位置
- 调整按钮大小
- 设置图标颜色
- 设置圆形背景颜色


## 双输入法直接切换

启用“双输入法直接切换”后，可以选择第一个键盘和第二个键盘。触发切换时，应用会根据当前默认键盘在两者之间切换；如果当前键盘不在这两个配置项中，则优先切换到第一个键盘。

直接切换模式下，浮动按钮会为两个配置键盘分别保存位置，并同时区分横屏和竖屏。

## 权限说明

直接切换键盘需要 `WRITE_SECURE_SETTINGS` 权限。可以用以下方式授予：

- 在电脑上使用 ADB 命令授权
- 在支持 Shizuku 的设备上，通过应用设置触发 Shizuku 授权

ADB 授权命令：

```sh
adb shell pm grant com.kunzisoft.keyboard.switcher android.permission.WRITE_SECURE_SETTINGS
```

| 权限 | 用途 | 是否必需 |
|------|------|----------|
| android.permission.WRITE_SECURE_SETTINGS | 实现两个键盘之间的直接切换（绕过系统输入法选择对话框）。 | 仅直接切换模式需要 |
| 无障碍服务（AccessibilityService） | 检测当前输入法变化，控制悬浮按钮的显示。 | 仅输入法拉起时显示浮动按钮需要 |
| SYSTEM_ALERT_WINDOW（如适用） | 在其他应用上层显示悬浮按钮。 | 仅显示浮动按钮需要 |

所有权限均不会用于收集任何个人信息。如果权限未授予、配置不完整，或目标键盘不可用，应用会回退到系统键盘选择对话框。

## 从第三方应用调用

### 打开键盘选择对话框

第三方应用可以发送 `com.android.keyboard.SWITCH_KEYBOARD` Intent 来打开键盘选择器：

```kotlin
Intent("com.android.keyboard.SWITCH_KEYBOARD").apply {
    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
```

### 自动切换到指定键盘

默认情况下，应用会显示系统键盘选择对话框。授予安全设置权限后，也可以通过 `KEYBOARD_ID` 参数请求切换到指定键盘：

1. 按上方权限说明授予 `WRITE_SECURE_SETTINGS` 权限
2. 在第三方应用中发送带 `KEYBOARD_ID` 的 Intent：

    ```kotlin
    Intent("com.android.keyboard.SWITCH_KEYBOARD").apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        putExtra("KEYBOARD_ID", getKeyboardId(context))
    }
    ```

## 它真的是免费的吗？

是的。Keyboard Switcher 使用**自由软件许可证 GPLv3+** 发布，并且**没有广告**。你可以查看完整源代码。

## 贡献

你可以通过以下方式帮助改进这个项目：

- 通过 **[Merge Request](https://docs.gitlab.com/ee/gitlab-basics/add-merge-request.html)** 添加功能或修复问题
- **[捐赠](https://www.kunzisoft.com/donation)** 支持开发者

## 下载

请仅从自动化构建的 [GitHub](https://github.com/Sight-wcg/KeyboardSwitcher/releases/latest) 地址下载安装。

社区交流

- [LinuxDo](https://linux.do/) —— 有问题、有想法，或者就是想来聊聊？

## 许可证

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
