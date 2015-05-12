package com.dict.hm.dictionary.lib;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Toast;

import com.dict.hm.dictionary.R;

import net.sourceforge.zbar.Config;
import net.sourceforge.zbar.Image;
import net.sourceforge.zbar.ImageScanner;
import net.sourceforge.zbar.Symbol;
import net.sourceforge.zbar.SymbolSet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


/**
 * Created by hm on 15-3-16.
 */
public class ZBarActivity extends Activity{
    private static final String TAG = "ZBar";
    public static final String RESULT = "result";
    private Camera mCamera;
    private ImageScanner scanner;

    private SurfaceView mPreview;
    private Handler autoFocusHandler;
//    boolean scanned = false;
    boolean previewing = true;
    boolean isSurfaceViewDestroyed = true;
    boolean autoFocus;
    boolean reverse;

    static {
        System.loadLibrary("iconv");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);

        if (checkCameraHardware() && safeCameraOpen(0)) {
            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            autoFocus = checkCameraAutoFocus();
            mPreview = (SurfaceView) findViewById(R.id.camera_preview);
            mPreview.getHolder().addCallback(holderCallback);
            autoFocusHandler = new Handler();
            scanner = new ImageScanner();
            scanner.setConfig(0, Config.X_DENSITY, 3);
            scanner.setConfig(0, Config.Y_DENSITY, 3);

            CheckBox checkBox = (CheckBox) findViewById(R.id.camera_checkbox);
            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    reverse = isChecked;
                }
            });

            mPreview.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    if (!previewing) {
                        previewing = true;
                        mCamera.setPreviewCallback(previewCallback);
                        mCamera.startPreview();
                    }
                    return true;
                }
            });
        } else {
            Toast.makeText(this, "failed to open Camera", Toast.LENGTH_LONG).show();
            finish();
        }
//        Log.d(TAG, "onCreate");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        releaseCameraAndPreview();
//        Log.d(TAG, "onDestroy");
    }

    @Override
    protected void onStop() {
        super.onStop();
        if (previewing) {
            mCamera.stopPreview();
        }
//        Log.d(TAG, "onStop");
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (previewing && !isSurfaceViewDestroyed) {
            mCamera.startPreview();
        }
//        Log.d(TAG, "onStart");
    }

    private boolean safeCameraOpen(int id) {
        boolean bOpened = false;
        try {
            releaseCameraAndPreview();
            mCamera = Camera.open(id);
            bOpened = (mCamera != null);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!bOpened) {
            Log.e(TAG, "failed to open Camera");
//            Log.d(TAG, "Numbers " + Camera.getNumberOfCameras());
        }
        return bOpened;
    }

    private void releaseCameraAndPreview() {
        if (mCamera != null) {
            previewing = false;
            mCamera.setPreviewCallback(null);
            mCamera.release();
            mCamera = null;
        }
//        Log.d(TAG, "releaseCameraAndPreview");
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            if (reverse) {
                reverseImageData(data);
            }
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            Image image = new Image(size.width, size.height, "Y800");
            image.setData(data);
            int result = scanner.scanImage(image);
            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                SymbolSet symbolSet = scanner.getResults();
                ArrayList<String> list = new ArrayList<>();
                for (Symbol symbol : symbolSet) {
                    list.add(symbol.getData());
//                    scanned = true;
                    Log.v(TAG, symbol.getData());
                }
                finishAndSetResult(list);
            }
        }
    };

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.post(doAutoFocus);
//            Log.d(TAG, "auto focus");
        }
    };

    private Runnable doAutoFocus = new Runnable() {
        @Override
        public void run() {
            if (previewing) {
                mCamera.autoFocus(autoFocusCallback);
            }
        }
    };

    SurfaceHolder.Callback holderCallback = new SurfaceHolder.Callback() {
        @Override
        public void surfaceCreated(SurfaceHolder holder) {
            try {
                mCamera.setPreviewDisplay(holder);
            } catch (IOException e) {
                e.printStackTrace();
            }
            isSurfaceViewDestroyed = false;
//            Log.d(TAG, "surfaceCreated");
        }

        @Override
        public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
            if (holder.getSurface() == null) {
                return;
            }
            mCamera.stopPreview();
            mCamera.setDisplayOrientation(90);
            mCamera.setPreviewCallback(previewCallback);
            mCamera.startPreview();
            if (autoFocus) {
                mCamera.autoFocus(autoFocusCallback);
            }
//            Log.d(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            isSurfaceViewDestroyed = true;
//            Log.d(TAG, "surfaceDestroy");
        }
    };

    private boolean checkCameraHardware() {
        PackageManager manager = getPackageManager();
        if (manager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return true;
        }
//        Log.d(TAG, "No Camera Found");
        return false;
    }

    private boolean checkCameraAutoFocus() {
        Camera.Parameters parameters = mCamera.getParameters();
        List<String> modes = parameters.getSupportedFocusModes();
        if (modes.contains(Camera.Parameters.FOCUS_MODE_AUTO) ||
                modes.contains(Camera.Parameters.FOCUS_MODE_MACRO)) {
            return true;
        }
        return false;
    }

    private void finishAndSetResult(ArrayList<String> resultList) {
        Intent intent = new Intent();
        intent.putStringArrayListExtra(RESULT, resultList);
        setResult(Activity.RESULT_OK, intent);
        finish();
    }

    private void reverseImageData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
//            data[i] = (byte) (~data[i] & 0xff);
            data[i] = (byte) (255 - data[i]);
        }
    }

}
