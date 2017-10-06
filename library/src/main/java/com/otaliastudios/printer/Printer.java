package com.otaliastudios.printer;


import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.pm.PackageManager;
import android.os.Build;
import android.support.annotation.CallSuper;
import android.support.annotation.NonNull;
import android.view.ViewTreeObserver;

import java.io.File;
import java.io.IOException;

/**
 * A general interface for engines that can print view hierarchies to some sort or file or folder.
 *
 * The printer will try to automatically request write and read permissions for your file,
 * but you need to pass the result of {@link #onRequestPermissionRequest(int, String[], int[])}
 * from your activity or fragment, and if true, call print again.
 */
public abstract class Printer {

    protected DocumentView mDocument;
    protected PrintCallback mCallback;
    protected boolean mPrintBackground = true;
    private int mPermissionCode;

    Printer(int permissionCode, @NonNull DocumentView document, @NonNull PrintCallback callback) {
        mPermissionCode = permissionCode;
        mDocument = document;
        mCallback = callback;
    }

    public void setPrintPageBackground(boolean printPageBackground) {
        mPrintBackground = printPageBackground;
    }

    boolean checkPreview(final String id, final File directory, final String name) {
        if (!mDocument.isAttachedToWindow()) {
            mCallback.onPrintFailed(id, new IllegalStateException("Preview not added to window."));
            return false;
        } else if (!mDocument.isLaidOut()) {
            mDocument.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
                @Override
                public void onGlobalLayout() {
                    mDocument.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    print(id, directory, name);
                }
            });
            return false;
        }
        // Remove focus from preview children.
        mDocument.requestFocus();
        return true;
    }

    boolean checkPermission(Context context) {
        if (Build.VERSION.SDK_INT < 23) return true;

        String[] permissions = new String[2];
        permissions[0] = Manifest.permission.READ_EXTERNAL_STORAGE;
        permissions[1] = Manifest.permission.WRITE_EXTERNAL_STORAGE;

        if (context.checkSelfPermission(permissions[0]) == PackageManager.PERMISSION_GRANTED &&
                context.checkSelfPermission(permissions[1]) == PackageManager.PERMISSION_GRANTED) {
            return true;
        }

        // Ask for them.
        Activity activity = null;
        while (context instanceof ContextWrapper) {
            if (context instanceof Activity) {
                activity = (Activity) context;
                break;
            }
            context = ((ContextWrapper) context).getBaseContext();
        }
        if (activity != null) {
            activity.requestPermissions(permissions, mPermissionCode);
        }
        return false;
    }

    public boolean onRequestPermissionRequest(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == mPermissionCode) {
            boolean granted = true;
            for (int grantResult : grantResults) {
                granted = granted && grantResult == PackageManager.PERMISSION_GRANTED;
            }
            return granted;
        }
        return false;
    }

    boolean checkFile(String id, File file) {
        try {
            file.getParentFile().mkdirs();
            if (file.exists()) file.delete();
            file.createNewFile();
            return true;
        } catch (IOException e) {
            mCallback.onPrintFailed(id, new RuntimeException("Could not create file.", e));
            return false;
        }
    }

    public abstract void print(String printId, @NonNull File directory, String filename);

}