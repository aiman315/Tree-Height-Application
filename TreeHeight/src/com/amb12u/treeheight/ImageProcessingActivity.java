package com.amb12u.treeheight;

import java.io.ByteArrayOutputStream;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewFrame;
import org.opencv.android.CameraBridgeViewBase.CvCameraViewListener2;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
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
	private static final int SELECT_PICTURE = 999;
	private CameraBridgeViewBase mOpenCvCameraView;
	private Mat currentFrameMat;

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

	/**
	 * Capture current frame, and launches a new activity with the image
	 * @param v: The view that invoked the method
	 */
	public void onClickCaptureImage(View v) {
		Log.d(TAG, "onClickCaptureImage");
		//FIXME: Error "FAILED BINDER TRANSACTION" sometimes
		
		if (currentFrameMat != null) {
			// Converting bitmap
			Bitmap image = Bitmap.createBitmap(currentFrameMat.cols(), currentFrameMat.rows(), Bitmap.Config.ARGB_8888);
			Utils.matToBitmap(currentFrameMat, image);
			ByteArrayOutputStream stream = new ByteArrayOutputStream();
		    image.compress(Bitmap.CompressFormat.JPEG, 100, stream);
		    
		    
			// Pass bytes array and start the activity 
			Intent intent = new Intent(this, StillImageProcessingActivity.class);
			Bundle bundle = new Bundle();
			bundle.putByteArray("CapturedImage", stream.toByteArray());
			intent.putExtras(bundle);
			startActivity(intent);	
		}
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
		currentFrameMat = inputFrame.gray();
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
	
		mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view_java);
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
		} else if (id == R.id.action_galleryImg) {
			Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent,"Select Picture"), SELECT_PICTURE);
		}
		return super.onOptionsItemSelected(item);
	}
	
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode == RESULT_OK) {
            if (requestCode == SELECT_PICTURE) {
                Uri selectedImageUri = data.getData();
               
                // Pass bytes array and start the activity 
    			Intent intent = new Intent(this, StillImageProcessingActivity.class);
    			intent.putExtra("ImgUri", selectedImageUri);
    			startActivity(intent);	
            }
        }
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
