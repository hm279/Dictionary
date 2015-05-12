package com.dict.hm.dictionary.lib;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
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
 * Created by hm on 15-3-18.
 */
public class ZBarFragment extends Fragment {
    private static final String TAG = "ZBar";

    private Camera mCamera;
    private ImageScanner scanner;

    private SurfaceView mPreview;
    private CheckBox checkBox;
    boolean scanned = false;
    boolean previewing = true;
    boolean isSurfaceViewDestroyed = true;
    boolean autoFocus;
    boolean reverse;
    private Handler autoFocusHandler;
    Context context;

    static {
        System.loadLibrary("iconv");
    }

    public interface ZBarListener {
        void finishAndSetResult(ArrayList<String> resultList);
    }
    ZBarListener listener;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
        try {
            listener = (ZBarListener) activity;
        } catch (ClassCastException e) {
            throw new ClassCastException(activity.toString()
                    + " must implement interface ZBarListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.activity_camera, container,false);
        mPreview = (SurfaceView) view.findViewById(R.id.camera_preview);
        checkBox = (CheckBox) view.findViewById(R.id.camera_checkbox);

        return view;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        if (checkCameraHardware() && safeCameraOpen(0)) {
            autoFocus = checkCameraAutoFocus();
            mPreview.getHolder().addCallback(holderCallback);
            autoFocusHandler = new Handler();
            scanner = new ImageScanner();
            scanner.setConfig(0, Config.X_DENSITY, 3);
            scanner.setConfig(0, Config.Y_DENSITY, 3);

            checkBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    reverse = isChecked;
                }
            });
        } else {
            Toast.makeText(getActivity(), "failed to open Camera", Toast.LENGTH_LONG).show();
            listener.finishAndSetResult(null);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        releaseCameraAndPreview();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (previewing && !isSurfaceViewDestroyed) {
            mCamera.startPreview();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (previewing) {
            mCamera.stopPreview();
        }
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
            Log.d(TAG, "Numbers " + Camera.getNumberOfCameras());
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
    }

    Camera.PreviewCallback previewCallback = new Camera.PreviewCallback() {
        @Override
        public void onPreviewFrame(byte[] data, Camera camera) {
            long s,e;
            s = System.currentTimeMillis();
            Camera.Parameters parameters = camera.getParameters();
            Camera.Size size = parameters.getPreviewSize();
            Image image = new Image(size.width, size.height, "Y800");
            image.setData(data);
            if (reverse) {
                reverseImageData(data);
            }
            int result = scanner.scanImage(image);
            if (result != 0) {
                previewing = false;
                mCamera.setPreviewCallback(null);
                mCamera.stopPreview();
                SymbolSet symbolSet = scanner.getResults();
                ArrayList<String> list = new ArrayList<>();
                for (Symbol symbol : symbolSet) {
                    list.add(symbol.getData());
                    scanned = true;
                    mCamera.stopPreview();
                    mCamera.setPreviewCallback(null);

                    e = System.currentTimeMillis();
                    Log.v(TAG, "decode time is " + (e - s));
                    Log.v(TAG, symbol.getData());
                }
                listener.finishAndSetResult(list);
            }
        }
    };

    Camera.AutoFocusCallback autoFocusCallback = new Camera.AutoFocusCallback() {
        @Override
        public void onAutoFocus(boolean success, Camera camera) {
            autoFocusHandler.post(doAutoFocus);
            Log.d(TAG, "auto focus");
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
            Log.d(TAG, "surfaceCreated");
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
            Log.d(TAG, "surfaceChanged");
        }

        @Override
        public void surfaceDestroyed(SurfaceHolder holder) {
            isSurfaceViewDestroyed = true;
        }
    };

    private boolean checkCameraHardware() {
        PackageManager manager = context.getPackageManager();
        if (manager.hasSystemFeature(PackageManager.FEATURE_CAMERA)
                || manager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)) {
            return true;
        }
        Log.d(TAG, "No Camera Found");
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

    private void reverseImageData(byte[] data) {
        for (int i = 0; i < data.length; i++) {
//            data[i] = (byte) (~data[i] & 0xff);
            data[i] = (byte) (255 - data[i]);
        }
    }

}
