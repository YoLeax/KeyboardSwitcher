package com.kunzisoft.keyboard.switcher;

interface ISecureSettingsGrantService {
    void destroy() = 16777114;
    boolean grantWriteSecureSettings(String packageName, int userId) = 1;
}
