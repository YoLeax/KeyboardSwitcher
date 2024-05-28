package com.kunzisoft.keyboard.switcher;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.annotation.SuppressLint;
import android.content.Intent;
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
                startActivityAndCollapse(Utilities.getPendingIntent(this));
            } else {
                Intent intent = new Intent(this, KeyboardManagerActivity.class);
                intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
                startActivityAndCollapse(intent);
            }
        } else {
            Utilities.chooseAKeyboard(this);
        }
    }
}
