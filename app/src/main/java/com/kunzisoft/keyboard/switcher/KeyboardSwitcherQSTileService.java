package com.kunzisoft.keyboard.switcher;

import android.annotation.SuppressLint;
import android.os.Build;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

import com.kunzisoft.keyboard.switcher.utils.Utilities;

@RequiresApi(api = Build.VERSION_CODES.N)
public class KeyboardSwitcherQSTileService extends TileService {

    @Override
    @SuppressLint("StartActivityAndCollapseDeprecated")
    public void onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                startActivityAndCollapse(
                        KeyboardManagerActivity.getPendingIntent(this, 1100L)
                );
            } else {
                startActivityAndCollapse(KeyboardManagerActivity.getIntent(this));
            }
        } else {
            Utilities.chooseAKeyboard(this);
        }
    }
}
