package com.kunzisoft.keyboard.switcher;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.view.WindowManager.LayoutParams;
import android.widget.ImageView;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.core.content.ContextCompat;

import com.google.gson.Gson;
import com.kunzisoft.keyboard.switcher.utils.Utilities;

class FloatingButtonController implements View.OnTouchListener, View.OnClickListener {

    interface WindowTypeProvider {
        int getWindowType();
    }

    private static final String POSITION_PORTRAIT = "POSITION_PORTRAIT";
    private static final String POSITION_LANDSCAPE = "POSITION_LANDSCAPE";
    private static final String POSITION_DIRECT_FIRST_PORTRAIT = "POSITION_DIRECT_FIRST_PORTRAIT";
    private static final String POSITION_DIRECT_FIRST_LANDSCAPE = "POSITION_DIRECT_FIRST_LANDSCAPE";
    private static final String POSITION_DIRECT_SECOND_PORTRAIT = "POSITION_DIRECT_SECOND_PORTRAIT";
    private static final String POSITION_DIRECT_SECOND_LANDSCAPE = "POSITION_DIRECT_SECOND_LANDSCAPE";

    private final Context context;
    private final WindowManager windowManager;
    private final SharedPreferences preferences;
    private final WindowTypeProvider windowTypeProvider;

    private View topLeftView;
    private View bottomRightView;
    private ImageView overlayedButton;
    private boolean moving;
    private boolean lockedButton;
    private PositionOrientation currentPosition = new PositionOrientation();
    private KeyboardSwitchController.PositionScope currentPositionScope =
            KeyboardSwitchController.PositionScope.DEFAULT;

    private static class PositionOrientation implements Parcelable {
        @DrawableRes
        int overlayedButtonResourceId = R.drawable.ic_keyboard_white_32dp;
        int[] positionToSave = {0, 0};
        float[] offset = {0F, 0F};
        int[] originalPosition = {0, 0};

        PositionOrientation() {}

        protected PositionOrientation(Parcel in) {
            overlayedButtonResourceId = in.readInt();
            positionToSave = in.createIntArray();
            offset = in.createFloatArray();
            originalPosition = in.createIntArray();
        }

        public static final Creator<PositionOrientation> CREATOR = new Creator<PositionOrientation>() {
            @Override
            public PositionOrientation createFromParcel(Parcel in) {
                return new PositionOrientation(in);
            }

            @Override
            public PositionOrientation[] newArray(int size) {
                return new PositionOrientation[size];
            }
        };

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int i) {
            parcel.writeInt(overlayedButtonResourceId);
            parcel.writeIntArray(positionToSave);
            parcel.writeFloatArray(offset);
            parcel.writeIntArray(originalPosition);
        }
    }

    FloatingButtonController(
            Context context,
            WindowManager windowManager,
            SharedPreferences preferences,
            WindowTypeProvider windowTypeProvider
    ) {
        this.context = context;
        this.windowManager = windowManager;
        this.preferences = preferences;
        this.windowTypeProvider = windowTypeProvider;
    }

    boolean isCreated() {
        return overlayedButton != null;
    }

    @SuppressLint("ClickableViewAccessibility")
    void create() {
        erase();
        try {
            lockedButton = preferences.getBoolean(
                    context.getString(R.string.settings_floating_button_lock_key),
                    false
            );

            int typeFilter = windowTypeProvider.getWindowType();

            overlayedButton = new ImageView(context);
            @ColorInt int color = preferences.getInt(
                    context.getString(R.string.settings_colors_key),
                    ContextCompat.getColor(context, R.color.colorPrimaryLight)
            );
            @ColorInt int backgroundColor = preferences.getInt(
                    context.getString(R.string.settings_floating_background_color_key),
                    ContextCompat.getColor(context, R.color.colorFloatingButtonBackgroundDefault)
            );
            overlayedButton.setImageResource(R.drawable.ic_keyboard_white_32dp);
            overlayedButton.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
            overlayedButton.setColorFilter(color);
            overlayedButton.setAlpha((color >> 24) & 0xff);
            overlayedButton.setBackground(createButtonBackground(backgroundColor));
            overlayedButton.setOnTouchListener(this);
            overlayedButton.setOnClickListener(this);

            topLeftView = new View(context);
            LayoutParams topLeftParams =
                    new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT,
                            typeFilter,
                            floatingButtonFlags(),
                            PixelFormat.TRANSLUCENT);
            topLeftParams.gravity = Gravity.START | Gravity.TOP;
            topLeftParams.x = 0;
            topLeftParams.y = 0;
            topLeftParams.width = 0;
            topLeftParams.height = 0;
            windowManager.addView(topLeftView, topLeftParams);

            bottomRightView = new View(context);
            LayoutParams bottomRightParams =
                    new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT,
                            typeFilter,
                            floatingButtonFlags(),
                            PixelFormat.TRANSLUCENT);
            bottomRightParams.gravity = Gravity.END | Gravity.BOTTOM;
            bottomRightParams.x = 0;
            bottomRightParams.y = 0;
            bottomRightParams.width = 0;
            bottomRightParams.height = 0;
            windowManager.addView(bottomRightView, bottomRightParams);

            LayoutParams overlayedButtonParams =
                    new LayoutParams(LayoutParams.WRAP_CONTENT,
                            LayoutParams.WRAP_CONTENT,
                            typeFilter,
                            floatingButtonFlags(),
                            PixelFormat.TRANSLUCENT);
            overlayedButtonParams.gravity = Gravity.CENTER;
            overlayedButtonParams.x = 0;
            overlayedButtonParams.y = 0;

            currentPositionScope = KeyboardSwitchController.getCurrentPositionScope(
                    context,
                    preferences
            );
            restorePosition(overlayedButtonParams, currentPositionScope);

            int defaultSize = (int) (32 * context.getResources().getDisplayMetrics().density);
            int sizeMultiplier = preferences.getInt(
                    context.getString(R.string.settings_floating_size_key),
                    50
            );
            int buttonSize = defaultSize * sizeMultiplier / 100;
            overlayedButtonParams.width = buttonSize;
            overlayedButtonParams.height = buttonSize;
            applyButtonIconPadding(buttonSize);
            overlayedButtonParams.softInputMode = LayoutParams.SOFT_INPUT_ADJUST_NOTHING;

            windowManager.addView(overlayedButton, overlayedButtonParams);
        } catch (Exception e) {
            Log.e("FloatingButtonController", "Unable to show floating button", e);
            erase();
        }
    }

    void setVisible(boolean visible) {
        if (overlayedButton == null) {
            return;
        }

        overlayedButton.setVisibility(visible ? View.VISIBLE : View.INVISIBLE);
        LayoutParams params = (LayoutParams) overlayedButton.getLayoutParams();
        if (visible) {
            params.flags &= ~LayoutParams.FLAG_NOT_TOUCHABLE;
        } else {
            params.flags |= LayoutParams.FLAG_NOT_TOUCHABLE;
        }
        windowManager.updateViewLayout(overlayedButton, params);
    }

    void erase() {
        removeView(overlayedButton);
        removeView(topLeftView);
        removeView(bottomRightView);
        overlayedButton = null;
        topLeftView = null;
        bottomRightView = null;
    }

    private void removeView(View view) {
        if (view == null) {
            return;
        }

        try {
            windowManager.removeView(view);
        } catch (Exception ignored) {}
    }

    void destroy() {
        if (overlayedButton != null) {
            savePreferencePosition(context.getResources().getConfiguration().orientation);
        }
        erase();
    }

    private int floatingButtonFlags() {
        return LayoutParams.FLAG_NOT_FOCUSABLE
                | LayoutParams.FLAG_NOT_TOUCH_MODAL
                | LayoutParams.FLAG_LAYOUT_IN_SCREEN;
    }

    private GradientDrawable createButtonBackground(@ColorInt int color) {
        GradientDrawable background = new GradientDrawable();
        background.setShape(GradientDrawable.OVAL);
        background.setColor(color);
        return background;
    }

    private void applyButtonIconPadding(int buttonSize) {
        int padding = Math.max(1, buttonSize / 5);
        overlayedButton.setPadding(padding, padding, padding, padding);
    }

    private void restorePosition(
            LayoutParams overlayedButtonParams,
            KeyboardSwitchController.PositionScope scope
    ) {
        currentPosition = new PositionOrientation();
        String positionKey = positionKey(
                context.getResources().getConfiguration().orientation,
                scope
        );
        if (!preferences.contains(positionKey) && scope != KeyboardSwitchController.PositionScope.DEFAULT) {
            positionKey = positionKey(
                    context.getResources().getConfiguration().orientation,
                    KeyboardSwitchController.PositionScope.DEFAULT
            );
        }
        PositionOrientation savedPosition = (new Gson()).fromJson(
                preferences.getString(positionKey, null),
                PositionOrientation.class
        );
        if (savedPosition != null) {
            currentPosition = savedPosition;
            overlayedButtonParams.x = currentPosition.positionToSave[0];
            overlayedButtonParams.y = currentPosition.positionToSave[1];
        }
        overlayedButton.setImageResource(currentPosition.overlayedButtonResourceId);
    }

    private void setOverlayedDrawableResource(@DrawableRes int newDrawableResourceId) {
        if (newDrawableResourceId != currentPosition.overlayedButtonResourceId) {
            currentPosition.overlayedButtonResourceId = newDrawableResourceId;
            overlayedButton.setImageResource(currentPosition.overlayedButtonResourceId);
        }
    }

    private void getPositionOnScreen(MotionEvent event) {
        int[] location = new int[2];
        if (overlayedButton != null) {
            overlayedButton.getLocationOnScreen(location);
        }

        currentPosition.originalPosition[0] = (int) (location[0] + event.getX());
        currentPosition.originalPosition[1] = (int) (location[1] + event.getY());
    }

    private void savePreferencePosition(int position) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(positionKey(position, currentPositionScope), (new Gson()).toJson(currentPosition));
        editor.apply();
    }

    private String positionKey(
            int orientation,
            KeyboardSwitchController.PositionScope scope
    ) {
        boolean landscape = orientation == Configuration.ORIENTATION_LANDSCAPE;
        if (scope == KeyboardSwitchController.PositionScope.FIRST) {
            return landscape ? POSITION_DIRECT_FIRST_LANDSCAPE : POSITION_DIRECT_FIRST_PORTRAIT;
        }
        if (scope == KeyboardSwitchController.PositionScope.SECOND) {
            return landscape ? POSITION_DIRECT_SECOND_LANDSCAPE : POSITION_DIRECT_SECOND_PORTRAIT;
        }
        return landscape ? POSITION_LANDSCAPE : POSITION_PORTRAIT;
    }

    private void drawButton(View view, int x, int y) {
        if (topLeftView != null && bottomRightView != null) {
            int[] topLeftLocationOnScreen = new int[2];
            topLeftView.getLocationOnScreen(topLeftLocationOnScreen);

            int[] bottomRightLocationOnScreen = new int[2];
            bottomRightView.getLocationOnScreen(bottomRightLocationOnScreen);

            LayoutParams params = (LayoutParams) overlayedButton.getLayoutParams();

            if (x <= view.getMeasuredWidth() / 2) {
                x = topLeftLocationOnScreen[0];
                setOverlayedDrawableResource(R.drawable.ic_keyboard_left_white_32dp);
            } else if (x >= bottomRightLocationOnScreen[0] - view.getMeasuredWidth() / 2) {
                x = bottomRightLocationOnScreen[0];
                setOverlayedDrawableResource(R.drawable.ic_keyboard_right_white_32dp);
            } else {
                setOverlayedDrawableResource(R.drawable.ic_keyboard_white_32dp);
            }

            params.x = x - (bottomRightLocationOnScreen[0] + topLeftLocationOnScreen[0]) / 2;
            params.y = y - (bottomRightLocationOnScreen[1] + topLeftLocationOnScreen[1]) / 2;
            currentPosition.positionToSave[0] = params.x;
            currentPosition.positionToSave[1] = params.y;

            windowManager.updateViewLayout(overlayedButton, params);
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, MotionEvent event) {
        if (lockedButton) {
            if (event.getAction() == MotionEvent.ACTION_UP) {
                view.playSoundEffect(android.view.SoundEffectConstants.CLICK);
                onClick(view);
            }
            return true;
        }

        float x = event.getRawX();
        float y = event.getRawY();
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            moving = false;

            getPositionOnScreen(event);

            currentPosition.offset[0] = currentPosition.originalPosition[0] - x;
            currentPosition.offset[1] = currentPosition.originalPosition[1] - y;
        } else if (event.getAction() == MotionEvent.ACTION_MOVE) {
            int newX = (int) (currentPosition.offset[0] + x);
            int newY = (int) (currentPosition.offset[1] + y);

            int deltaMoveX = view.getMeasuredWidth() * 3 / 4;
            int deltaMoveY = view.getMeasuredHeight() * 3 / 4;

            if (Math.abs(newX - currentPosition.originalPosition[0]) < deltaMoveX
                    && Math.abs(newY - currentPosition.originalPosition[1]) < deltaMoveY
                    && !moving) {
                return false;
            }

            drawButton(view, newX, newY);
            moving = true;
        } else if (event.getAction() == MotionEvent.ACTION_UP) {
            savePreferencePosition(context.getResources().getConfiguration().orientation);
            return moving;
        }

        return false;
    }

    @Override
    public void onClick(final View view) {
        savePreferencePosition(context.getResources().getConfiguration().orientation);
        if (context instanceof KeyboardVisibilityAccessibilityService) {
            ((KeyboardVisibilityAccessibilityService) context)
                    .prepareKeyboardRequestAfterDirectSwitch();
        }
        KeyboardSwitchController.Result result = KeyboardSwitchController.perform(
                context,
                preferences,
                KeyboardSwitchController.Trigger.FLOATING_BUTTON
        );
        if (result.isSwitched()) {
            applyPositionForKeyboard(result.getTargetKeyboardId());
            if (context instanceof KeyboardVisibilityAccessibilityService) {
                ((KeyboardVisibilityAccessibilityService) context)
                        .requestKeyboardAfterDirectSwitch();
            }
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            KeyboardManagerActivity.launch(context);
        } else {
            Utilities.chooseAKeyboard(context);
        }
    }

    private void applyPositionForKeyboard(String keyboardId) {
        if (overlayedButton == null) {
            return;
        }

        currentPositionScope = KeyboardSwitchController.getPositionScope(
                context,
                preferences,
                keyboardId
        );
        LayoutParams params = (LayoutParams) overlayedButton.getLayoutParams();
        restorePosition(params, currentPositionScope);
        windowManager.updateViewLayout(overlayedButton, params);
    }
}
