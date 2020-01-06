package com.rectanglescanner.views;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.widget.FrameLayout;

import com.rectanglescanner.R;
import com.rectanglescanner.helpers.CapturedImage;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.imgcodecs.Imgcodecs;

import java.util.UUID;
import java.io.File;

/**
  Created by Jake on Jan 6, 2020.

  Wraps up the camera and rectangle detection code into a simple interface.
  Allows you to call start, stop, cleanup, and capture. Also is responsible
  for deterining how to cache the output images.
*/
public class RNRectangleScannerView extends RectangleDetectionController {

    //================================================================================
    // Setup
    //================================================================================

    public RNRectangleScannerView(Context context, Integer numCam, Activity activity, FrameLayout frameLayout) {
        super(context, numCam, activity, frameLayout);
    }

    private MainView parentView = null;

    public void setParent(MainView view) {
      this.parentView = view;
    }

    /**
    Call to capture an image
    */
    public void capture() {
      captureImageLater();
    }

    /**
     Called after a picture was captured
     */
    private void pictureWasTaken(WritableMap pictureDetails) {
      Log.d(TAG, "picture taken");
      this.parentView.pictureWasTaken(pictureDetails);
    }

    /**
     Called after a picture was captured and finished processing
     */
    private void pictureWasProcessed(WritableMap pictureDetails) {
      Log.d(TAG, "picture processed");
      this.parentView.pictureWasProcessed(pictureDetails);
    }

    /**
     Called after the torch/flash state was changed
     */
    @Override
    protected void torchWasChanged(boolean torchEnabled) {
      Log.d(TAG, "torch changed");
      this.parentView.torchWasChanged(torchEnabled);
    }

    /**
     Called after the camera and session are set up. This lets you check if a
     camera is found and permission is granted to use it.
     */
    @Override
    protected void deviceWasSetup(WritableMap config) {
      Log.d(TAG, "device setup");
      this.parentView.deviceWasSetup(config);
    }


    /**
     Called after a frame is processed and a rectangle was found
     */
    @Override
    public void rectangleWasDetected(WritableMap detection) {
      this.parentView.rectangleWasDetected(detection);
    }


    /**
     After an image is captured and cropped, this method is called
     */
    @Override
    public void onProcessedCapturedImage(CapturedImage capturedImage) {
      WritableMap pictureWasTakenConfig = new WritableNativeMap();
      WritableMap pictureWasProcessedConfig = new WritableNativeMap();
      String croppedImageFileName = generateStoredFileName("cropped");
      String originalImageFileName = generateStoredFileName("original");
      pictureWasTakenConfig.putString("croppedImage", "file://" + croppedImageFileName);
      pictureWasTakenConfig.putString("initialImage", "file://" + originalImageFileName);
      pictureWasProcessedConfig.putString("croppedImage", "file://" + croppedImageFileName);
      pictureWasProcessedConfig.putString("initialImage", "file://" + originalImageFileName);
      pictureWasTaken(pictureWasTakenConfig);

      Mat doc = (capturedImage.processed != null) ? capturedImage.processed : capturedImage.original;
      String fileName = this.saveToDirectory(doc, croppedImageFileName);
      String initialFileName = this.saveToDirectory(capturedImage.original, originalImageFileName);

      pictureWasProcessed(pictureWasProcessedConfig);

      Log.d(TAG, "wrote: " + fileName);
    }

    private String generateStoredFileName(String name) {
      String folderName = "documents";
      String folderDir = this.mContext.getCacheDir().toString();
      File folder = new File( folderDir + "/" + folderName);
      if (!folder.exists()) {
          boolean result = folder.mkdirs();
          if (result) Log.d(TAG, "wrote: created folder " + folder.getPath());
          else Log.d(TAG, "Not possible to create folder"); // TODO: Manage this error better
      }
      return folderDir + "/" + folderName + "/" + UUID.randomUUID() + '-' + name + ".jpg";
    }

    /**
    Saves a file to a folder
    */
    private String saveToDirectory(Mat doc, String fileName) {
        Mat endDoc = new Mat(Double.valueOf(doc.size().width).intValue(), Double.valueOf(doc.size().height).intValue(),
                CvType.CV_8UC4);

        Core.flip(doc.t(), endDoc, 1);

        Imgcodecs.imwrite(fileName, endDoc);

        endDoc.release();

        return fileName;
    }

}