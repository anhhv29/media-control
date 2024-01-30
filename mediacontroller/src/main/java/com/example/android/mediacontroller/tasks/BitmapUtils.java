package com.example.android.mediacontroller.tasks;

import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;

import com.example.android.mediacontroller.R;

public final class BitmapUtils {

    private BitmapUtils() {
        // Utility class.
    }

    public static Bitmap convertDrawable(@NonNull final Resources resources,
                                         @NonNull final Drawable drawable,
                                         final boolean downScale) {

        final Bitmap bitmap;
        if (drawable instanceof BitmapDrawable) {
            bitmap = ((BitmapDrawable) drawable).getBitmap();
        } else {
            bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(),
                    drawable.getIntrinsicHeight(),
                    Bitmap.Config.ARGB_8888);
            final Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);
        }

        if (!downScale) {
            return bitmap;
        }

        final int iconSize = resources.getDimensionPixelSize(R.dimen.app_icon_size);
        if (bitmap.getHeight() > iconSize || bitmap.getWidth() > iconSize) {
            // Which needs to be scaled to fit.
            final int height = bitmap.getHeight();
            final int width = bitmap.getWidth();

            final int scaleHeight;
            final int scaleWidth;

            // Calculate the new size based on which dimension is larger.
            if (height > width) {
                scaleHeight = iconSize;
                scaleWidth = (int) (width * ((float) iconSize) / height);
            } else {
                scaleWidth = iconSize;
                scaleHeight = (int) (height * ((float) iconSize) / width);
            }

            return Bitmap.createScaledBitmap(bitmap, scaleWidth, scaleHeight, false);
        } else {
            return bitmap;
        }
    }

    public static Bitmap createToolbarIcon(@NonNull Resources resources,
                                           @NonNull final Bitmap icon) {
        final int padding = resources.getDimensionPixelSize(R.dimen.margin_small);
        final int iconSize = resources.getDimensionPixelSize(R.dimen.toolbar_icon_size);
        final int sizeWithPadding = iconSize + (2 * padding);

        // Create a Bitmap backed Canvas to be the toolbar icon.
        final Bitmap toolbarIcon =
                Bitmap.createBitmap(sizeWithPadding, sizeWithPadding, Bitmap.Config.ARGB_8888);
        final Canvas canvas = new Canvas(toolbarIcon);
        canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR);

        // Resize the app icon to Material Design size.
        final Bitmap scaledIcon = Bitmap.createScaledBitmap(icon, iconSize, iconSize, false);
        canvas.drawBitmap(scaledIcon, padding, padding, null);

        return toolbarIcon;
    }
}
