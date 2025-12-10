package com.kunzisoft.keyboard.switcher;

import android.annotation.SuppressLint;
import android.os.Build;
import android.service.quicksettings.Tile;
import android.service.quicksettings.TileService;

import androidx.annotation.RequiresApi;

@RequiresApi(api = Build.VERSION_CODES.N)
public class KeyboardSwitcherQSTileService extends TileService {

    @Override
    public void onTileAdded() {
        super.onTileAdded();
        getQsTile().setState(Tile.STATE_ACTIVE);
    }

    @Override
    @SuppressLint("StartActivityAndCollapseDeprecated")
    public void onClick() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            startActivityAndCollapse(
                    KeyboardManagerActivity.getPendingIntent(this, 1100L)
            );
        } else {
            startActivityAndCollapse(KeyboardManagerActivity.getIntent(this));
        }
    }
}
