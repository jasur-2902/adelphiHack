package uz.shukurov.carrecognition;

import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.FileProvider;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.google.android.gms.tasks.Continuation;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

import uz.shukurov.carrecognition.other.InternetCheck;
import uz.shukurov.carrecognition.other.MyBounceInterpolator;
import uz.shukurov.carrecognition.other.RequestCode;


public class MainActivity extends AppCompatActivity {


    private StorageReference mStorageImage;
    private ImageButton mCapture, mTakePicture;
    private String downloadUri;
    private Uri mImageUri = null;

    private String url = "http://18.217.108.249/cgi-bin/recognize2.py?url=";

    private static final String TAG = MainActivity.class.getSimpleName();

    private AlertDialog mDialog;
    private AlertDialog.Builder mBuilder;
    private LinearLayout mLinearLayout;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        mStorageImage = FirebaseStorage.getInstance().getReference().child("images");

        mCapture = findViewById(R.id.mCapture);

        mTakePicture = findViewById(R.id.mTakePicture);

        final Animation myAnim = AnimationUtils.loadAnimation(this, R.anim.bounce);
        MyBounceInterpolator interpolator = new MyBounceInterpolator(0.2, 20);
        myAnim.setInterpolator(interpolator);

        mCapture.startAnimation(myAnim);
        mTakePicture.startAnimation(myAnim);


        mLinearLayout = findViewById(R.id.linearLayout);

        if (!InternetCheck.isInternetAvailable(this)) {
            initSnackbar();
            mLinearLayout.setVisibility(View.INVISIBLE);
        }

        permissionRequest();

        mCapture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {


                Intent galleryIntent = new Intent();
                galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
                galleryIntent.setType("image/*");
                startActivityForResult(galleryIntent, RequestCode.GALLERY_REQUEST);

            }
        });


        mTakePicture.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dispatchTakePictureIntent();

            }
        });

        if (!isDeviceSupportCamera()) {
            Toast.makeText(getApplicationContext(),
                    "Sorry! Your device doesn't support camera",
                    Toast.LENGTH_LONG).show();
            // will close the app if the device does't have camera
            finish();
        }

    }


    private void initSnackbar() {
        final Snackbar snackbar = Snackbar.make(mLinearLayout, R.string.no_internet, Snackbar.LENGTH_INDEFINITE).setAction(R.string.retry, new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (InternetCheck.isInternetAvailable(view.getContext())) {
                    mLinearLayout.setVisibility(View.VISIBLE);
                } else initSnackbar();
            }
        });
//
        snackbar.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle item selection
        switch (item.getItemId()) {
            case R.id.menu_item1:
                return true;
            case R.id.menu_item2:
                AlertDialog dialog;
                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
                builder.setTitle("About")
                        .setMessage("This is one of the best vehicle recognition applications. It can identify the car's license plate number, color, model, brand and year. The project was created by Jasur Shukurov 2018-2019")
                        .setPositiveButton("Okay!", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });


                dialog = builder.create();
                dialog.show();

                return true;
            case R.id.menu_item3:
                Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://home.adelphi.edu/~ja21947/car/privacy_policy.html"));
                startActivity(browserIntent);
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void permissionRequest() {

        if (ContextCompat.checkSelfPermission(MainActivity.this,
                android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // Permission is not granted
            // Should we show an explanation?
            if (ActivityCompat.shouldShowRequestPermissionRationale(MainActivity.this,
                    android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                // Show an explanation to the user *asynchronously* -- don't block
                // this thread waiting for the user's response! After the user
                // sees the explanation, try again to request the permission.
            } else {
                // No explanation needed; request the permission
                ActivityCompat.requestPermissions(MainActivity.this,
                        new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE},
                        RequestCode.MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE);

                // MY_PERMISSIONS_REQUEST_READ_CONTACTS is an
                // app-defined int constant. The callback method gets the
                // result of the request.
            }
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        switch (requestCode) {
            case RequestCode.MY_PERMISSIONS_REQUEST_EXTERNAL_STORAGE: {
                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0
                        && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    // permission was granted, yay! Do the
                    // contacts-related task you need to do.
                } else {
                    // permission denied, boo! Disable the
                    // functionality that depends on this permission.
                }
                return;
            }

            // other 'case' lines to check for other
            // permissions this app might request.
        }
    }

    String mCurrentPhotoPath;

    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );

        // Save a file: path for use with ACTION_VIEW intents
        mCurrentPhotoPath = image.getAbsolutePath();
        return image;
    }

    Uri photoURI;


    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            // Create the File where the photo should go
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
            }
            // Continue only if the File was successfully created
            if (photoFile != null) {
                photoURI = FileProvider.getUriForFile(this,
                        "uz.shukurov.carrecognition.fileprovider", photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, RequestCode.REQUEST_TAKE_PHOTO);
            }
        }
    }

    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // this device has a camera
            return true;
        } else {
            // no camera on this device
            return false;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RequestCode.GALLERY_REQUEST && resultCode == RESULT_OK) {
            mImageUri = data.getData();
            uploadImage();
        }

        if (requestCode == RequestCode.REQUEST_TAKE_PHOTO && resultCode == RESULT_OK) {

            mImageUri = photoURI;
            uploadImage();

        }

    }

    //Uploading Image to Firebase
    private void uploadImage() {

        if (mImageUri != null) {
            final StorageReference filepath = mStorageImage.child(mImageUri.getLastPathSegment());

            mBuilder = new AlertDialog.Builder(MainActivity.this);
            mBuilder.setTitle("Loading...")
                    .setMessage("Please wait, it will take some time!")
                    .setCancelable(false);

            mDialog = mBuilder.create();
            mDialog.show();

            filepath.putFile(mImageUri).continueWithTask(new Continuation<UploadTask.TaskSnapshot, Task<Uri>>() {
                @Override
                public Task<Uri> then(@NonNull Task<UploadTask.TaskSnapshot> task) throws Exception {
                    if (!task.isSuccessful()) {
                        mDialog.show();
                        throw task.getException();
                    }
                    return filepath.getDownloadUrl();
                }
            }).addOnCompleteListener(new OnCompleteListener<Uri>() {
                @Override
                public void onComplete(@NonNull Task<Uri> task) {
                    if (task.isSuccessful()) {
                        downloadUri = task.getResult().toString();
                        String query = downloadUri.substring(87, downloadUri.length());
                        new DownloadTask().execute(url + query);

                    } else {
                        mDialog.cancel();
                        Toast.makeText(MainActivity.this, "Upload failed: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

        } else {
            mDialog.cancel();
            Toast.makeText(MainActivity.this, "Sorry, something went wrong!", Toast.LENGTH_SHORT).show();
        }
    }

    private class DownloadTask extends AsyncTask<String, Void, String> {

        @Override
        protected String doInBackground(String... params) {
            //do your request in here so that you don't interrupt the UI thread
            try {
                return downloadContent(params[0]);
            } catch (IOException e) {
                return "Unable to retrieve data. URL may be invalid.";
            }
        }

        @Override
        protected void onPostExecute(String result) {
            //Here you are done with the task

            Intent intent = new Intent(MainActivity.this, ResultActivity.class);

            String[] extra = new String[2];
            extra[0] = result;
            extra[1] = downloadUri;


            intent.putExtra("EXTRA_SESSION_ID", extra);

            mDialog.cancel();
            if (result.length() < 600) {

                final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);

                builder.setMessage("Sorry, we can't identify this car! Please try one more time!")
                        .setTitle("Error, cannot detect!")
                        .setPositiveButton("Okay", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                            }
                        });

                AlertDialog alertDialog = builder.create();
                alertDialog.show();

            } else
                startActivity(intent);


        }
    }

    private String downloadContent(String myurl) throws IOException {
        InputStream is = null;

        try {
            URL url = new URL(myurl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setReadTimeout(5000 /* milliseconds */);
            conn.setConnectTimeout(5500 /* milliseconds */);
            conn.setRequestMethod("GET");
            conn.setDoOutput(true);
            conn.connect();
            int response = conn.getResponseCode();
            Log.d(TAG, "The response is: " + response);
            is = conn.getInputStream();

            // Convert the InputStream into a string

            System.out.println("\nSending 'Get' request to URL : " + url + "--" + response);

            BufferedReader in = new BufferedReader(
                    new InputStreamReader(conn.getInputStream()));
            String inputLine;
            StringBuffer response2 = new StringBuffer();

            while ((inputLine = in.readLine()) != null) {
                response2.append(inputLine);
            }
            in.close();

            System.out.println("Response : -- " + response2.toString());

            mDialog.cancel();

            return response2.toString();


        } finally {
            if (is != null) {
                is.close();
            }
        }
    }


}
