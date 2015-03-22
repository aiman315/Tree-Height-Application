package com.amb12u.treeheight;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class DescActivity extends Activity {

	private final String TAG = "DescActivity";

	private final int REQUEST_CODE_GALLERY = 999;
	private final int REQUEST_CODE_CAMERA = 1000;
	private final int INVALID_METHOD_SELECTION = -999;
	private final int MATH_METHOD_SELECTION = 1;
	private final int IMAGE_PROCESSING_METHOD_SELECTION = 2;

	private TextView descHeightPerson;
	private TextView descTree1;
	private TextView descTree2;
	private TextView descSnap1;
	private TextView descSnap2;
	private ImageView imageViewHeightPerson;
	private ImageView imageViewTree1;
	private ImageView imageViewTree2;
	private ImageView imageViewSnap1;
	private ImageView imageViewSnap2;

	private int selectedMethod;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_desc);

		descHeightPerson = (TextView) findViewById(R.id.textViewDescHeightPerson);
		descTree1 = (TextView) findViewById(R.id.textViewDescTree1);
		descTree2 = (TextView) findViewById(R.id.textViewDescTree2);
		descSnap1 = (TextView) findViewById(R.id.textViewDescSnap1);
		descSnap2 = (TextView) findViewById(R.id.textViewDescSnap2);

		imageViewHeightPerson = (ImageView) findViewById(R.id.imageViewMathTreetop);
		imageViewTree1 = (ImageView) findViewById(R.id.imageViewTree1);
		imageViewTree2 = (ImageView) findViewById(R.id.imageViewTree2);
		imageViewSnap1 = (ImageView) findViewById(R.id.imageViewSnap1);
		imageViewSnap2 = (ImageView) findViewById(R.id.imageViewSnap2);

		selectedMethod = INVALID_METHOD_SELECTION;

		Intent intent = getIntent();
		Bundle bundle = intent.getExtras();

		if(bundle!=null) {
			int buttonId = (Integer) bundle.get("button_id");
			switch(buttonId) {
			case R.id.buttonMath: 
				setTitle(R.string.button_math);
				descHeightPerson.setText(R.string.math_approach_desc);
				descTree1.setText(R.string.math_approach_desc); 
				descTree2.setText(R.string.math_approach_desc);
				descSnap1.setText(R.string.math_approach_desc);
				descSnap2.setText(R.string.math_approach_desc);

				imageViewHeightPerson.setBackgroundResource(R.drawable.height_person);
				imageViewTree1.setBackgroundResource(R.drawable.math_treetop);
				imageViewTree2.setBackgroundResource(R.drawable.math_tree_bottom);
				imageViewSnap1.setBackgroundResource(R.drawable.snap_top);
				imageViewSnap2.setBackgroundResource(R.drawable.snap_bottom);

				selectedMethod = MATH_METHOD_SELECTION;
				break;
			case R.id.buttonIP: 
				setTitle(R.string.button_image_processing);
				descTree1.setText(R.string.img_processing_approach_desc);
				descTree2.setText(R.string.img_processing_approach_desc);
				descSnap2.setText(R.string.img_processing_approach_desc);

				imageViewTree1.setBackgroundResource(R.drawable.ip_tree_man);
				imageViewTree2.setBackgroundResource(R.drawable.ip_tree);
				imageViewSnap2.setBackgroundResource(R.drawable.snap_ip);

				selectedMethod = IMAGE_PROCESSING_METHOD_SELECTION;
				break;
			default:
				//TODO: what to do here?
				setTitle(R.string.button_something_else);
				descTree1.setText("Error");
				selectedMethod = INVALID_METHOD_SELECTION;
				break;
			}
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
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
	}


	@Override
	protected void onPause() {
		Log.d(TAG, "onPause");
		super.onPause();
	}


	@Override
	protected void onResume() {
		Log.d(TAG, "onResume");
		super.onResume();
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

	public void onClickTry(View v) {
		Log.d(TAG, "onClickTry");
		switch(selectedMethod) {
		case MATH_METHOD_SELECTION: {
			Intent cameraIntent = new Intent(this, CameraActivity.class);
			startActivity(cameraIntent);
			break;
		}
		case IMAGE_PROCESSING_METHOD_SELECTION: {

			AlertDialog.Builder photoSourceDialog = new AlertDialog.Builder(this);
			photoSourceDialog.setMessage("Select Photo source");
			photoSourceDialog.setPositiveButton("Gallery", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					Intent galleryIntent = new Intent(Intent.ACTION_GET_CONTENT, null);
					galleryIntent.setType("image/*");
					galleryIntent.putExtra("return-data", true);
					startActivityForResult(galleryIntent,REQUEST_CODE_GALLERY);
				}
			});

			photoSourceDialog.setNegativeButton("Camera", new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface arg0, int arg1) {
					Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
					startActivityForResult(cameraIntent,REQUEST_CODE_CAMERA);
				}
			});
			photoSourceDialog.show();
			break;
		}	
		default:
			Toast.makeText(this, "Invalid method selection", Toast.LENGTH_SHORT).show();
			break;
		}
	}

	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult");
		Log.d(TAG, String.format("requestCode: %d | resultCode: %d", requestCode, resultCode));

		switch (requestCode) {
		case REQUEST_CODE_CAMERA:
		case REQUEST_CODE_GALLERY:

			if (resultCode == RESULT_CANCELED) {
				Toast.makeText(this, "Canceled", Toast.LENGTH_SHORT).show();

				return;
			} else {

				Uri selectedImageUri = data.getData();
				// Pass image uri and start the activity 
				Intent intent = new Intent(this, ImageProcessingActivity.class);
				intent.putExtra("ImgUri", selectedImageUri);
				startActivity(intent);	
			}
			break;
		default:
			Toast.makeText(this, "Invalid request code", Toast.LENGTH_SHORT).show();
		}

	}
}
