package com.kunzisoft.keyboard.switcher;

import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;

import android.content.Intent;
import android.os.Build;
import android.service.quicksettings.TileService;

import com.kunzisoft.keyboard.switcher.utils.Utilities;

public class KeyboardSwitcherQSTileService extends TileService {
    @Override
    public void onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Intent intent = new Intent(this, KeyboardManagerActivity.class);
            intent.addFlags(FLAG_ACTIVITY_NEW_TASK);
            startActivityAndCollapse(intent);
        } else {
            Utilities.chooseAKeyboard(this);
        }
    }
}
