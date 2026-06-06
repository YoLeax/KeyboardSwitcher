package com.kunzisoft.keyboard.switcher;

import android.Manifest;
import android.content.Context;

import androidx.annotation.Keep;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class SecureSettingsGrantService extends ISecureSettingsGrantService.Stub {

    public SecureSettingsGrantService() {}

    @Keep
    public SecureSettingsGrantService(Context context) {}

    @Override
    public boolean grantWriteSecureSettings(String packageName, int userId) {
        if (packageName == null || packageName.isEmpty()) {
            return false;
        }

        Process process = null;
        try {
            process = new ProcessBuilder(
                    "pm",
                    "grant",
                    "--user",
                    String.valueOf(userId),
                    packageName,
                    Manifest.permission.WRITE_SECURE_SETTINGS
            ).redirectErrorStream(true).start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    process.getInputStream(),
                    StandardCharsets.UTF_8
            ))) {
                while (reader.readLine() != null) {}
            }

            return process.waitFor() == 0;
        } catch (Throwable ignored) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    @Override
    public void destroy() {
        System.exit(0);
    }
}
