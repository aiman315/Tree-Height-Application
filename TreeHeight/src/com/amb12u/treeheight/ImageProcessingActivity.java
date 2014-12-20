package com.amb12u.treeheight;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;

public class ImageProcessingActivity extends Activity implements CvCameraViewListener2{


	private final String TAG = "ImageProcessingActivity";
	private CameraBridgeViewBase mOpenCvCameraView;

	private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
		@Override
		public void onManagerConnected(int status) {
			switch (status) {
			case LoaderCallbackInterface.SUCCESS:
				Log.i(TAG, "OpenCV loaded successfully");
				mOpenCvCameraView.enableView();
				break;
			default:
				super.onManagerConnected(status);
				break;
			}
		}
	};

	public void onClickCaptureImage(View v) {
		Log.d(TAG, "onClickCaptureImage");
		//TODO:
	}

	public void onClickResetCapture(View v) {
		Log.d(TAG, "onClickResetCapture");
		//TODO:
	}

	public void onClickCalculateHeight(View v) {
		Log.d(TAG, "onClickCalculateHeight");
		//TODO:
	}

	//	---------------- CvCameraViewListener2 Interface Methods ---------------- //    

	@Override
	public void onCameraViewStarted(int width, int height) {
		Log.d(TAG, "onCameraViewStarted");
	}

	@Override
	public void onCameraViewStopped() {
		Log.d(TAG, "onCameraViewStopped");
	}

	@Override
	public Mat onCameraFrame(CvCameraViewFrame inputFrame) {
		Log.d(TAG, "onCameraFrame");
		return inputFrame.rgba();
	}

	//	---------------- Activity Methods ---------------- //

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		Log.d(TAG, "onCreate");
		super.onCreate(savedInstanceState);
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		setContentView(R.layout.activity_image_processing);

		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		setContentView(R.layout.activity_image_processing);

		mOpenCvCameraView.setVisibility(SurfaceView.VISIBLE);
		mOpenCvCameraView.setCvCameraViewListener(this);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		Log.d(TAG, "onCreateOptionsMenu");
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.image_processing, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		Log.d(TAG, "onOptionsItemSelected");
		// Handle action bar item clicks here. The action bar will
		// automatically handle clicks on the Home/Up button, so long
		// as you specify a parent activity in AndroidManifest.xml.
		int id = item.getItemId();
		if (id == R.id.action_settings) {
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	protected void onDestroy() {
		Log.d(TAG, "onDestroy");
		super.onDestroy();

		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();	
		}
	}

	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();

		if (mOpenCvCameraView != null) {
			mOpenCvCameraView.disableView();
		}
	}

	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();

		OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_2_4_3, this, mLoaderCallback);
	}

	@Override
	protected void onStart() {
		Log.d(TAG, "onStart");
		super.onStart();
	}

	@Override
	protected void onStop() {
		Log.d(TAG, "onStop");
		super.onStop();
	}
}
