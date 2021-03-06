/*===============================================================================
Copyright (c) 2012-2014 Qualcomm Connected Experiences, Inc. All Rights Reserved.

Vuforia is a trademark of PTC Inc., registered in the United States and other 
countries.
===============================================================================*/

package com.qualcomm.vuforia.samples.VuforiaSamples.app.MultiTargets;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.GestureDetector;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.widget.RelativeLayout;

import com.qualcomm.vuforia.CameraDevice;
import com.qualcomm.vuforia.DataSet;
import com.qualcomm.vuforia.ImageTarget;
import com.qualcomm.vuforia.Matrix34F;
import com.qualcomm.vuforia.MultiTarget;
import com.qualcomm.vuforia.MultiTargetResult;
import com.qualcomm.vuforia.ObjectTracker;
import com.qualcomm.vuforia.STORAGE_TYPE;
import com.qualcomm.vuforia.State;
import com.qualcomm.vuforia.Tool;
import com.qualcomm.vuforia.Tracker;
import com.qualcomm.vuforia.TrackerManager;
import com.qualcomm.vuforia.Vec3F;
import com.qualcomm.vuforia.Vuforia;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationControl;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationException;
import com.qualcomm.vuforia.samples.SampleApplication.SampleApplicationSession;
import com.qualcomm.vuforia.samples.SampleApplication.utils.LoadingDialogHandler;
import com.qualcomm.vuforia.samples.SampleApplication.utils.SampleApplicationGLView;
import com.qualcomm.vuforia.samples.SampleApplication.utils.Texture;
import com.qualcomm.vuforia.samples.VuforiaSamples.R;
import com.qualcomm.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenu;
import com.qualcomm.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenuGroup;
import com.qualcomm.vuforia.samples.VuforiaSamples.ui.SampleAppMenu.SampleAppMenuInterface;

import java.util.Vector;

// The main activity for the MultiTargets sample.
public class MultiTargets extends Activity
      implements SampleApplicationControl, SampleAppMenuInterface {
   final public static int CMD_BACK = -1;

   // Process Single Tap event to trigger autofocus
   private class GestureListener extends GestureDetector.SimpleOnGestureListener {
      // Used to set autofocus one second after a manual focus is triggered
      private final Handler autofocusHandler = new Handler();

      @Override
      public boolean onDown(MotionEvent e) {
         return true;
      }

      @Override
      public boolean onSingleTapUp(MotionEvent e) {
         // Generates a Handler to trigger autofocus
         // after 1 second
         autofocusHandler.postDelayed(new Runnable() {
            public void run() {
               boolean result = CameraDevice.getInstance()
                     .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_TRIGGERAUTO);

               if (!result) {
                  Log.e("SingleTapUp", "Unable to trigger focus");
               }
            }
         }, 1000L);

         return true;
      }
   }
   private static final String LOGTAG = "MultiTargets";
   boolean mIsDroidDevice = false;
   SampleApplicationSession vuforiaAppSession;
   private DataSet dataSet = null;
   private LoadingDialogHandler loadingDialogHandler = new LoadingDialogHandler(this);
   // Alert Dialog used to display SDK errors
   private AlertDialog mErrorDialog;
   private GestureDetector mGestureDetector;
   // Our OpenGL view:
   private SampleApplicationGLView mGlView;
   // Our renderer:
   private MultiTargetRenderer mRenderer;
   private SampleAppMenu mSampleAppMenu;
   // The textures we will use for rendering:
   private Vector<Texture> mTextures;
   private RelativeLayout mUILayout;
   private MultiTarget mit = null;

   @Override
   public boolean doDeinitTrackers() {
      // Indicate if the trackers were deinitialized correctly
      boolean result = true;

      TrackerManager tManager = TrackerManager.getInstance();
      tManager.deinitTracker(ObjectTracker.getClassType());

      return result;
   }

   @Override
   public boolean doInitTrackers() {
      // Indicate if the trackers were initialized correctly
      boolean result = true;

      TrackerManager tManager = TrackerManager.getInstance();
      Tracker tracker;

      tracker = tManager.initTracker(ObjectTracker.getClassType());
      if (tracker == null) {
         Log.e(LOGTAG,
               "Tracker not initialized. Tracker already initialized or the camera is already " +
                     "started");
         result = false;
      } else {
         Log.i(LOGTAG, "Tracker successfully initialized");
      }

      return result;
   }

   @Override
   public boolean doLoadTrackersData() {
      // Get the image tracker:
      TrackerManager trackerManager = TrackerManager.getInstance();
      ObjectTracker objectTracker =
            (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());
      if (objectTracker == null) {
         Log.d(LOGTAG,
               "Failed to load tracking data set because the ObjectTracker has not been " +
                     "initialized.");
         return false;
      }

      // Create the data set:
      dataSet = objectTracker.createDataSet();
      if (dataSet == null) {
         Log.d(LOGTAG, "Failed to create a new tracking data.");
         return false;
      }

      // Load the data set:
      if (!dataSet.load("MultiTargets/FlakesBox.xml", STORAGE_TYPE.STORAGE_APPRESOURCE)) {
         Log.d(LOGTAG, "Failed to load data set.");
         return false;
      }

      Log.d(LOGTAG, "Successfully loaded the data set.");

      // Validate the MultiTarget and setup programmatically if required:
      initMIT();

      if (!objectTracker.activateDataSet(dataSet)) {
         return false;
      }

      return true;
   }

   @Override
   public boolean doStartTrackers() {
      // Indicate if the trackers were started correctly
      boolean result = true;

      Tracker objectTracker = TrackerManager.getInstance()
            .getTracker(ObjectTracker.getClassType());
      if (objectTracker != null) {
         objectTracker.start();
      }

      return result;
   }

   @Override
   public boolean doStopTrackers() {
      // Indicate if the trackers were stopped correctly
      boolean result = true;

      Tracker objectTracker = TrackerManager.getInstance()
            .getTracker(ObjectTracker.getClassType());
      if (objectTracker != null) {
         objectTracker.stop();
      }

      return result;
   }

   @Override
   public boolean doUnloadTrackersData() {
      // Indicate if the trackers were unloaded correctly
      boolean result = true;

      // Get the image tracker:
      TrackerManager trackerManager = TrackerManager.getInstance();
      ObjectTracker objectTracker =
            (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());
      if (objectTracker == null) {
         Log.d(LOGTAG,
               "Failed to destroy the tracking data set because the ObjectTracker has not been " +
                     "initialized.");
         return false;
      }

      if (dataSet != null) {
         if (!objectTracker.deactivateDataSet(dataSet)) {
            Log.d(LOGTAG,
                  "Failed to destroy the tracking data set because the data set could not be " +
                        "deactivated.");
            result = false;
         } else if (!objectTracker.destroyDataSet(dataSet)) {
            Log.d(LOGTAG, "Failed to destroy the tracking data set.");
            result = false;
         }

         if (result) {
            Log.d(LOGTAG, "Successfully destroyed the data set.");
         }

         dataSet = null;
         mit = null;
      }

      return result;
   }

   @Override
   public boolean menuProcess(int command) {
      boolean result = true;

      switch (command) {
         case CMD_BACK:
            finish();
            break;
      }

      return result;
   }

   @Override
   public void onConfigurationChanged(Configuration config) {
      Log.d(LOGTAG, "onConfigurationChanged");
      super.onConfigurationChanged(config);

      vuforiaAppSession.onConfigurationChanged();
   }

   @Override
   public void onInitARDone(SampleApplicationException exception) {

      if (exception == null) {
         initApplicationAR();

         mRenderer.mIsActive = true;

         // Now add the GL surface view. It is important
         // that the OpenGL ES surface view gets added
         // BEFORE the camera is started and video
         // background is configured.
         addContentView(mGlView,
               new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));

         // Sets the UILayout to be drawn in front of the camera
         mUILayout.bringToFront();

         // Hides the Loading Dialog
         loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.HIDE_LOADING_DIALOG);

         // Sets the layout background to transparent
         mUILayout.setBackgroundColor(Color.TRANSPARENT);

         try {
            vuforiaAppSession.startAR(CameraDevice.CAMERA.CAMERA_DEFAULT);
         } catch (SampleApplicationException e) {
            Log.e(LOGTAG, e.getString());
         }

         boolean result = CameraDevice.getInstance()
               .setFocusMode(CameraDevice.FOCUS_MODE.FOCUS_MODE_CONTINUOUSAUTO);

         if (!result) {
            Log.e(LOGTAG, "Unable to enable continuous autofocus");
         }

         mSampleAppMenu = new SampleAppMenu(this, this, "Multi Targets", mGlView, mUILayout, null);
         setSampleAppMenuSettings();
      } else {
         Log.e(LOGTAG, exception.getString());
         showInitializationErrorMessage(exception.getString());
      }
   }

   @Override
   public void onQCARUpdate(State state) {
   }

   @Override
   public boolean onTouchEvent(MotionEvent event) {
      // Process the Gestures
      if (mSampleAppMenu != null && mSampleAppMenu.processEvent(event)) {
         return true;
      }

      return mGestureDetector.onTouchEvent(event);
   }

   // Shows initialization error messages as System dialogs
   public void showInitializationErrorMessage(String message) {
      final String errorMessage = message;
      runOnUiThread(new Runnable() {
         public void run() {
            if (mErrorDialog != null) {
               mErrorDialog.dismiss();
            }

            // Generates an Alert Dialog to show the error message
            AlertDialog.Builder builder = new AlertDialog.Builder(MultiTargets.this);
            builder.setMessage(errorMessage)
                  .setTitle(getString(R.string.INIT_ERROR))
                  .setCancelable(false)
                  .setIcon(0)
                  .setPositiveButton(getString(R.string.button_OK),
                        new DialogInterface.OnClickListener() {
                           public void onClick(DialogInterface dialog, int id) {
                              finish();
                           }
                        });

            mErrorDialog = builder.create();
            mErrorDialog.show();
         }
      });
   }

   // Called when the activity first starts or the user navigates back to an
   // activity.
   @Override
   protected void onCreate(Bundle savedInstanceState) {
      Log.d(LOGTAG, "onCreate");
      super.onCreate(savedInstanceState);

      vuforiaAppSession = new SampleApplicationSession(this);

      startLoadingAnimation();

      vuforiaAppSession.initAR(this, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

      // Load any sample specific textures:
      mTextures = new Vector<Texture>();
      loadTextures();

      mGestureDetector = new GestureDetector(this, new GestureListener());

      mIsDroidDevice = android.os.Build.MODEL.toLowerCase()
            .startsWith("droid");
   }

   // The final call you receive before your activity is destroyed.
   @Override
   protected void onDestroy() {
      Log.d(LOGTAG, "onDestroy");
      super.onDestroy();

      try {
         vuforiaAppSession.stopAR();
      } catch (SampleApplicationException e) {
         Log.e(LOGTAG, e.getString());
      }

      // Unload texture:
      mTextures.clear();
      mTextures = null;

      System.gc();
   }

   // Called when the system is about to start resuming a previous activity.
   @Override
   protected void onPause() {
      Log.d(LOGTAG, "onPause");
      super.onPause();

      if (mGlView != null) {
         mGlView.setVisibility(View.INVISIBLE);
         mGlView.onPause();
      }

      try {
         vuforiaAppSession.pauseAR();
      } catch (SampleApplicationException e) {
         Log.e(LOGTAG, e.getString());
      }
   }

   // Called when the activity will start interacting with the user.
   @Override
   protected void onResume() {
      Log.d(LOGTAG, "onResume");
      super.onResume();

      // This is needed for some Droid devices to force portrait
      if (mIsDroidDevice) {
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
         setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
      }

      try {
         vuforiaAppSession.resumeAR();
      } catch (SampleApplicationException e) {
         Log.e(LOGTAG, e.getString());
      }

      // Resume the GL view:
      if (mGlView != null) {
         mGlView.setVisibility(View.VISIBLE);
         mGlView.onResume();
      }
   }

   ImageTarget findImageTarget(String name) {
      TrackerManager trackerManager = TrackerManager.getInstance();
      ObjectTracker objectTracker =
            (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());

      if (objectTracker != null) {
         for (int i = 0; i < dataSet.getNumTrackables(); i++) {
            if (dataSet.getTrackable(i)
                  .getType() == MultiTargetResult.getClassType()) {
               if (dataSet.getTrackable(i)
                     .getName()
                     .compareTo(name) == 0) {
                  return (ImageTarget) (dataSet.getTrackable(i));
               }
            }
         }
      }
      return null;
   }

   void initMIT() {
      //
      // This function checks the current tracking setup for completeness. If
      // it finds that something is missing, then it creates it and configures
      // it:
      // Any MultiTarget and Part elements missing from the config.xml file
      // will be created.
      //

      Log.d(LOGTAG, "Beginning to check the tracking setup");

      // Configuration data - identical to what is in the config.xml file
      //
      // If you want to recreate the trackable assets using the on-line TMS
      // server using the original images provided in the sample's media
      // folder, use the following trackable sizes on creation to get
      // identical visual results:
      // create a cuboid with width = 90 ; height = 120 ; length = 60.

      String names[] = { "FlakesBox.Front", "FlakesBox.Back", "FlakesBox.Left", "FlakesBox.Right",
            "FlakesBox.Top", "FlakesBox.Bottom" };
      float trans[] =
            { 0.0f, 0.0f, 30.0f, 0.0f, 0.0f, -30.0f, -45.0f, 0.0f, 0.0f, 45.0f, 0.0f, 0.0f, 0.0f,
                  60.0f, 0.0f, 0.0f, -60.0f, 0.0f };
      float rots[] =
            { 1.0f, 0.0f, 0.0f, 0.0f, 0.0f, 1.0f, 0.0f, 180.0f, 0.0f, 1.0f, 0.0f, -90.0f, 0.0f,
                  1.0f, 0.0f, 90.0f, 1.0f, 0.0f, 0.0f, -90.0f, 1.0f, 0.0f, 0.0f, 90.0f };

      TrackerManager trackerManager = TrackerManager.getInstance();
      ObjectTracker objectTracker =
            (ObjectTracker) trackerManager.getTracker(ObjectTracker.getClassType());

      if (objectTracker == null || dataSet == null) {
         return;
      }

      // Go through all Trackables to find the MultiTarget instance
      //
      for (int i = 0; i < dataSet.getNumTrackables(); i++) {
         if (dataSet.getTrackable(i)
               .getType() == MultiTargetResult.getClassType()) {
            Log.d(LOGTAG, "MultiTarget exists -> no need to create one");
            mit = (MultiTarget) (dataSet.getTrackable(i));
            break;
         }
      }

      // If no MultiTarget was found, then let's create one.
      if (mit == null) {
         Log.d(LOGTAG, "No MultiTarget found -> creating one");
         mit = dataSet.createMultiTarget("FlakesBox");

         if (mit == null) {
            Log.d(LOGTAG,
                  "ERROR: Failed to create the MultiTarget - probably the Tracker is running");
            return;
         }
      }

      // Try to find each ImageTarget. If we find it, this actually means that
      // it is not part of the MultiTarget yet: ImageTargets that are part of
      // a MultiTarget don't show up in the list of Trackables.
      // Each ImageTarget that we found, is then made a part of the
      // MultiTarget and a correct pose (reflecting the pose of the
      // config.xml file) is set).
      //
      int numAdded = 0;
      for (int i = 0; i < 6; i++) {
         ImageTarget it = findImageTarget(names[i]);
         if (it != null) {
            Log.d(LOGTAG, "ImageTarget '%s' found -> adding it as to the MultiTarget" + names[i]);

            int idx = mit.addPart(it);
            Vec3F t = new Vec3F(trans[i * 3], trans[i * 3 + 1], trans[i * 3 + 2]);
            Vec3F a = new Vec3F(rots[i * 4], rots[i * 4 + 1], rots[i * 4 + 2]);
            Matrix34F mat = new Matrix34F();

            Tool.setTranslation(mat, t);
            Tool.setRotation(mat, a, rots[i * 4 + 3]);
            mit.setPartOffset(idx, mat);
            numAdded++;
         }
      }

      Log.d(LOGTAG, "Added " + numAdded + " ImageTarget(s) to the MultiTarget");

      if (mit.getNumParts() != 6) {
         Log.d(LOGTAG,
               "ERROR: The MultiTarget should have 6 parts, but it reports " + mit.getNumParts() +
                     " parts");
      }

      Log.d(LOGTAG, "Finished checking the tracking setup");
   }

   // Initializes AR application components.
   private void initApplicationAR() {
      // Create OpenGL ES view:
      int depthSize = 16;
      int stencilSize = 0;
      boolean translucent = Vuforia.requiresAlpha();

      mGlView = new SampleApplicationGLView(this);
      mGlView.init(translucent, depthSize, stencilSize);

      mRenderer = new MultiTargetRenderer(vuforiaAppSession);
      mRenderer.setTextures(mTextures);
      mGlView.setRenderer(mRenderer);
   }

   // We want to load specific textures from the APK, which we will later use
   // for rendering.
   private void loadTextures() {
      mTextures.add(Texture.loadTextureFromApk("MultiTargets/TextureWireframe.png", getAssets()));
      mTextures.add(
            Texture.loadTextureFromApk("MultiTargets/TextureBowlAndSpoon.png", getAssets()));
   }

   // This method sets the menu's settings
   private void setSampleAppMenuSettings() {
      SampleAppMenuGroup group;

      group = mSampleAppMenu.addGroup("", false);
      group.addTextItem(getString(R.string.menu_back), -1);

      mSampleAppMenu.attachMenu();
   }

   private void startLoadingAnimation() {
      LayoutInflater inflater = LayoutInflater.from(this);
      mUILayout = (RelativeLayout) inflater.inflate(R.layout.camera_overlay, null, false);

      mUILayout.setVisibility(View.VISIBLE);
      mUILayout.setBackgroundColor(Color.BLACK);

      // Gets a reference to the loading dialog
      loadingDialogHandler.mLoadingDialogContainer = mUILayout.findViewById(R.id.loading_indicator);

      // Shows the loading indicator at start
      loadingDialogHandler.sendEmptyMessage(LoadingDialogHandler.SHOW_LOADING_DIALOG);

      // Adds the inflated layout to the view
      addContentView(mUILayout,
            new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
   }
}
