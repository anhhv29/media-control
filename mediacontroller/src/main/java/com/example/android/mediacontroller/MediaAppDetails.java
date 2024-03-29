package com.example.android.mediacontroller;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.FeatureInfo;
import android.content.pm.PackageItemInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.content.pm.ServiceInfo;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.media.session.MediaSession;
import android.os.Build;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.media.MediaBrowserServiceCompat;

import com.example.android.mediacontroller.tasks.BitmapUtils;

import java.util.List;

public class MediaAppDetails implements Parcelable {
    public final String packageName;
    public final String appName;
    public final Bitmap icon;
    @Nullable
    public Bitmap banner;
    public final MediaSessionCompat.Token sessionToken;
    public final ComponentName componentName;
    public boolean supportsAutomotive = false;
    public boolean supportsAuto = false;

    @SuppressLint("ObsoleteSdkInt")
    public MediaAppDetails(String packageName, String name, Bitmap appIcon,
                           @Nullable Bitmap appBanner, MediaSessionCompat.Token token) {
        this.packageName = packageName;
        appName = name;
        sessionToken = token;
        icon = appIcon;
        // This TV app targets min sdk version 21, and a banner will only be present for the TV app
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            banner = appBanner;
        }
        componentName = null;
    }

    public MediaAppDetails(String packageName, String name, Bitmap appIcon,
                           @Nullable Bitmap appBanner, MediaSession.Token token) {
        this(packageName, name, appIcon, appBanner, MediaSessionCompat.Token.fromToken(token));
    }

    public MediaAppDetails(PackageItemInfo info, PackageManager pm, Resources resources,
                           MediaSession.Token token) {
        packageName = info.packageName;
        appName = info.loadLabel(pm).toString();
        Drawable appIcon = info.loadIcon(pm);
        icon = BitmapUtils.convertDrawable(resources, appIcon, true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP) {
            Drawable appBanner = info.loadBanner(pm);
            if (appBanner != null) {
                banner = BitmapUtils.convertDrawable(resources, appBanner, false);
            }
        }

        if (token != null) {
            // If we have a MediaSession Token, then we don't need to connect to the
            // MediaBrowserService implementation, so componentName is null.
            componentName = null;
            sessionToken = MediaSessionCompat.Token.fromToken(token);
        } else {
            // If we don't have a MediaSession Token, then we need to connect to the
            // MediaBrowserService implementation.
            componentName = new ComponentName(info.packageName, info.name);
            sessionToken = null;
        }

        try {
            FeatureInfo[] features = pm.getPackageInfo(
                    packageName, PackageManager.GET_CONFIGURATIONS).reqFeatures;

            supportsAutomotive = false;
            if (features != null) {
                for (FeatureInfo f : features) {
                    if (f.name != null && f.name.equals("android.hardware.type.automotive")) {
                        supportsAutomotive = true;
                        break;
                    }
                }
            }

            Bundle metaData = pm.getApplicationInfo(packageName,
                    PackageManager.GET_META_DATA).metaData;

            if (metaData != null) {
                if (metaData.containsKey("com.google.android.gms.car.application")) {
                    supportsAuto = true;
                }
            }
        } catch (PackageManager.NameNotFoundException e) {
            Log.w("MediaAppDetails", "package name not found" + packageName);
        }
    }

    public MediaAppDetails(PackageItemInfo info, PackageManager pm, Resources resources) {
        this(info, pm, resources, null);
    }

    public static ServiceInfo findServiceInfo(String packageName, PackageManager pm) {
        final Intent mediaBrowserIntent = new Intent(MediaBrowserServiceCompat.SERVICE_INTERFACE);
        final List<ResolveInfo> services =
                pm.queryIntentServices(mediaBrowserIntent,
                        PackageManager.GET_RESOLVED_FILTER);
        for (ResolveInfo info : services) {
            if (info.serviceInfo.packageName.equals(packageName)) {
                return (info.serviceInfo);
            }
        }
        return null;
    }

    public static List<ResolveInfo> findResolveInfo(
            String packageName, PackageManager pm, String action) {
        if (packageName != null) {
            Intent prefsIntent = new Intent(action);
            prefsIntent.setPackage(packageName);

            return pm.queryIntentActivities(prefsIntent, 0);
        }
        return null;
    }

    private MediaAppDetails(final Parcel parcel) {
        packageName = parcel.readString();
        appName = parcel.readString();
        icon = parcel.readParcelable(MediaAppDetails.class.getClassLoader());
        sessionToken = parcel.readParcelable(MediaAppDetails.class.getClassLoader());
        componentName = parcel.readParcelable(MediaAppDetails.class.getClassLoader());
        supportsAuto = parcel.readInt() == 1;
        supportsAutomotive = parcel.readInt() == 1;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(packageName);
        dest.writeString(appName);
        dest.writeParcelable(icon, flags);
        dest.writeParcelable(sessionToken, flags);
        dest.writeParcelable(componentName, flags);
        dest.writeInt(supportsAuto ? 1 : 0);
        dest.writeInt(supportsAutomotive ? 1 : 0);
    }

    public static final Parcelable.Creator<MediaAppDetails> CREATOR =
            new Parcelable.Creator<MediaAppDetails>() {

                public MediaAppDetails createFromParcel(Parcel source) {
                    return new MediaAppDetails(source);
                }

                public MediaAppDetails[] newArray(int size) {
                    return new MediaAppDetails[size];
                }
            };
}
