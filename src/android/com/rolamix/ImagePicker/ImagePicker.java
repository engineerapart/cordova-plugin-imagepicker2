/**
 * An Image Picker Plugin for Cordova/PhoneGap.
 */
package com.rolamix;

import android.Manifest;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.annotation.SuppressLint;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import static android.app.Activity.RESULT_OK;


public class ImagePicker extends CordovaPlugin {

    // I really don't know why this is here. Some repos have it, some removed it.
    // It certainly doesn't hurt anything... I believe this used to be how Cordova
    // identifies the plugin name. This is no longer how it does it but hey... history.
    public static String TAG = "ImagePicker";

    private static final String ACTION_GET_PICTURES = "getPictures";
    private static final String ACTION_HAS_READ_PERMISSION = "hasReadPermission";
    private static final String ACTION_REQUEST_READ_PERMISSION = "requestReadPermission";
    private static final String ACTION_CLEANUP_TEMP_FILES = "cleanupTempFiles";

    private static final int PERMISSION_REQUEST_CODE = 100;
    public static final int PERMISSION_DENIED_ERROR = 20;

    private int maxImages = 0;
    private int desiredWidth = 0;
    private int desiredHeight = 0;
    private int quality = 100;
    private int outputType = 0;

    private CallbackContext callbackContext;
    private Intent imagePickerIntent;

    public boolean execute(String action, final JSONArray args, final CallbackContext callbackContext) throws JSONException {
        this.callbackContext = callbackContext;

        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        ActivityManager activityManager = (ActivityManager) this.cordova.getActivity().getApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
        activityManager.getMemoryInfo(mi);
        long totalMegs = mi.totalMem / 1048576L;
        System.out.println("[ImagePicker] current memory: " + totalMegs);

        if (ACTION_HAS_READ_PERMISSION.equals(action)) {
            callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.OK, hasReadPermission()));
            return true;

        } else if (ACTION_REQUEST_READ_PERMISSION.equals(action)) {
            requestReadPermission();
            return true;

        } else if (ACTION_GET_PICTURES.equals(action)) {
            final JSONObject params = args.getJSONObject(0);
            imagePickerIntent = new Intent(cordova.getActivity(), MultiImageChooserActivity.class);

            this.maxImages = 15;
            this.desiredWidth = 0;
            this.desiredHeight = 0;
            this.quality = 100;
            this.outputType = 0;

            if (params.has("maximumImagesCount")) {
                this.maxImages = params.getInt("maximumImagesCount");
            }
            if (params.has("width")) {
                this.desiredWidth = params.getInt("width");
            }
            if (params.has("height")) {
                this.desiredHeight = params.getInt("height");
            }
            if (params.has("quality")) {
                this.quality = params.getInt("quality");
            }
            if (params.has("outputType")) {
                this.outputType = params.getInt("outputType");
            }

            imagePickerIntent.putExtra("MAX_IMAGES", this.maxImages);
            imagePickerIntent.putExtra("WIDTH", this.desiredWidth);
            imagePickerIntent.putExtra("HEIGHT", this.desiredHeight);
            imagePickerIntent.putExtra("QUALITY", this.quality);
            imagePickerIntent.putExtra("OUTPUT_TYPE", this.outputType);
            // imagePickerIntent.addFlags(Intent.FLAG_ACTIVITY_NO_ANIMATION);

            // some day, when everybody uses a cordova version supporting 'hasPermission', enable this:
            if (cordova != null) {
              if (cordova.hasPermission(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                cordova.startActivityForResult(this, imagePickerIntent, MultiImageChooserActivity.REQUEST_IMAGEPICKER);
              } else {
                cordova.requestPermission(
                        this,
                        PERMISSION_REQUEST_CODE,
                        Manifest.permission.READ_EXTERNAL_STORAGE
                );
              }
            }

            // // .. until then use:
            // if (hasReadPermission()) {
            //     cordova.startActivityForResult(this, imagePickerIntent, MultiImageChooserActivity.REQUEST_IMAGEPICKER);
            // } else {
            //     requestReadPermission();
            //     // The downside is the user needs to re-invoke this picker method.
            //     // The best thing to do for the dev is check 'hasReadPermission' manually and
            //     // run 'requestReadPermission' or 'getPictures' based on the outcome.
            // }
            return true;
        } else if (ACTION_CLEANUP_TEMP_FILES.equals(action)) {
          cleanupTempFiles();
        }

        return false;
    }

    @SuppressLint("InlinedApi")
    private boolean hasReadPermission() {
        return Build.VERSION.SDK_INT < 23 ||
            PackageManager.PERMISSION_GRANTED == ContextCompat.checkSelfPermission(this.cordova.getActivity(), Manifest.permission.READ_EXTERNAL_STORAGE);
    }

    @SuppressLint("InlinedApi")
    private void requestReadPermission() {
        if (!hasReadPermission()) {
            ActivityCompat.requestPermissions(
                this.cordova.getActivity(),
                new String[] {Manifest.permission.READ_EXTERNAL_STORAGE},
                PERMISSION_REQUEST_CODE);
        }
        // This method executes async and we seem to have no known way to receive the result
        // (that's why these methods were later added to Cordova), so simply returning ok now.
        callbackContext.success();
    }

    private void cleanupTempFiles() {
        File filePath = new File(System.getProperty("java.io.tmpdir"));
        for (final File fileEntry : filePath.listFiles()) {
            System.out.println("File Entry: " + fileEntry);
            fileEntry.delete();
        }
        this.callbackContext.success(new JSONObject());
    }

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == Activity.RESULT_OK && data != null) { //  && requestCode == MultiImageChooserActivity.REQUEST_IMAGEPICKER
            ArrayList<String> fileNames = data.getStringArrayListExtra("MULTIPLEFILENAMES");
            JSONArray res = new JSONArray(fileNames);
            callbackContext.success(res);

        } else if (resultCode == Activity.RESULT_CANCELED && data != null) {
            String error = data.getStringExtra("ERRORMESSAGE");
            if (error == null) {
                callbackContext.error("No images selected");
            } else {
              this.callbackContext.error(error);
            }

        } else if (resultCode == Activity.RESULT_CANCELED) {
            JSONArray res = new JSONArray();
            callbackContext.success(res);

        } else {
            callbackContext.error("No images selected");
        }
    }


    public Bundle onSaveInstanceState() {
        Bundle state = new Bundle();

        state.putInt("maxImages", this.maxImages);
        state.putInt("desiredWidth", this.desiredWidth);
        state.putInt("desiredHeight", this.desiredHeight);
        state.putInt("quality", this.quality);
        state.putInt("outputType", this.outputType);

        return state;
    }

    /**
     * Choosing a picture launches another Activity, so we need to implement the
     * save/restore APIs to handle the case where the CordovaActivity is killed by the OS
     * before we get the launched Activity's result.
     *
     * @see http://cordova.apache.org/docs/en/dev/guide/platforms/android/plugin.html#launching-other-activities
     */
    public void onRestoreStateForActivityResult(Bundle state, CallbackContext callbackContext) {
        this.maxImages = state.getInt("maxImages");
        this.desiredWidth = state.getInt("desiredWidth");
        this.desiredHeight = state.getInt("desiredHeight");
        this.quality = state.getInt("quality");
        this.outputType = state.getInt("outputType");

        this.callbackContext = callbackContext;
    }

    @Override
    public void onRequestPermissionResult(int requestCode,
                                          String[] permissions,
                                          int[] grantResults) throws JSONException {
      // // For now we just have one permission, so things can be kept simple...
      // if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
      //     cordova.startActivityForResult(this, imagePickerIntent, MultiImageChooserActivity.REQUEST_IMAGEPICKER);
      // } else {
      //     // Tell the JS layer that something went wrong...
      //     callbackContext.error("Permission denied");
      // }

      // Slightly more flexible solution, thanks to nixplay:
      for (int r : grantResults) {
          if (r == PackageManager.PERMISSION_DENIED) {
              this.callbackContext.sendPluginResult(new PluginResult(PluginResult.Status.ERROR, PERMISSION_DENIED_ERROR));
              return;
          }
      }
      switch (requestCode) {
        case PERMISSION_REQUEST_CODE:
          if (imagePickerIntent != null) {
              this.cordova.startActivityForResult(this, imagePickerIntent, MultiImageChooserActivity.REQUEST_IMAGEPICKER);
          }
          break;
      }
    }
}
