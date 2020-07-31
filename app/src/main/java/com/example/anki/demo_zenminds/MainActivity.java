package com.example.anki.demo_zenminds;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.amazonaws.mobileconnectors.s3.transferutility.TransferListener;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferObserver;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferState;
import com.amazonaws.mobileconnectors.s3.transferutility.TransferUtility;
import com.amazonaws.regions.Region;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3Client;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;

import android.provider.MediaStore;
import android.util.Log;
import android.view.View;

import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.amazonaws.auth.CognitoCredentialsProvider;
import com.amazonaws.auth.CognitoCachingCredentialsProvider;

import java.io.File;
import java.net.URI;

public class MainActivity extends AppCompatActivity {



    AmazonS3 s3;
    TransferUtility util;

    Button uploadButton;
    TextView progresss;
    ImageView selectedImage;

    private final static int PICK_FROM_GALLERY = 833;
    private final static int GALLERY_REQUEST = 318;


    String identityPool = "YOUR-POOL-ID";
    String bucket = "YOUR-BUCKET-NAME";
    Regions region = Regions.US_EAST_1;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        createAWSCredentials();

        uploadButton = (Button) findViewById(R.id.upload);
        progresss = (TextView) findViewById(R.id.progressValue);
        selectedImage = (ImageView) findViewById(R.id.imageView);


        uploadButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                //Upload Action TODO:-
                requestImageGallery();
            }
        });

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }



    private void createAWSCredentials() {
        CognitoCachingCredentialsProvider cognitoService = new CognitoCachingCredentialsProvider(getApplicationContext() ,
                identityPool, region);


        s3 = new AmazonS3Client(cognitoService);

        s3.setRegion(Region.getRegion(region));;

        prepareTransferUtility();
    }

    private void prepareTransferUtility() {
        util = TransferUtility.builder().s3Client(s3).context(getApplicationContext()).build();
    }


    private void requestImageGallery() {
        if (Build.VERSION.SDK_INT >= 23) {
            String[] PERMISSIONS = {android.Manifest.permission.WRITE_EXTERNAL_STORAGE, android.Manifest.permission.READ_EXTERNAL_STORAGE};
            if (!hasPermissions(MainActivity.this, PERMISSIONS)) {
                ActivityCompat.requestPermissions((Activity) MainActivity.this, PERMISSIONS, GALLERY_REQUEST);
            } else {
                openGallery();
            }
        } else {
            openGallery();
        }
    }

    private static boolean hasPermissions(Context context, String... permissions) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && context != null && permissions != null) {
            for (String permission : permissions) {
                if (ActivityCompat.checkSelfPermission(context, permission) != PackageManager.PERMISSION_GRANTED) {
                    return false;
                }
            }
        }
        return true;
    }


    private void openGallery() {
        Intent galleryIntent = new Intent();
        galleryIntent.setType("*/*");
        galleryIntent.setAction(Intent.ACTION_GET_CONTENT);
        galleryIntent.putExtra("return-data", true);
        startActivityForResult(galleryIntent, PICK_FROM_GALLERY);
    }

    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Toast.makeText(this, "Comes inside!", Toast.LENGTH_SHORT).show();


        if (requestCode == PICK_FROM_GALLERY) {

            if (resultCode == RESULT_OK) {
                Uri selectedImageUri = data.getData();

                String[] proj = {MediaStore.MediaColumns.DATA};
                Cursor cursor = getContentResolver().query(selectedImageUri, proj, null, null, null);



                if (cursor == null) {
                    File file =  new File(selectedImageUri.getPath());

                    String filePath = selectedImageUri.getPath();
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    selectedImage.setImageBitmap(bitmap);

                    uploadImageToAWS(file);
                } else {
                    File file =  new File(RealFilePath.getPath(this, selectedImageUri));


                    String filePath = RealFilePath.getPath(this, selectedImageUri);
                    Bitmap bitmap = BitmapFactory.decodeFile(filePath);
                    selectedImage.setImageBitmap(bitmap);


                    uploadImageToAWS(file);
                }
            } else {
                Toast.makeText(this, "Picture is not selected!", Toast.LENGTH_SHORT).show();
            }


        }





    }

    private void uploadImageToAWS(File imagedata) {

        String fileName = imagedata.getName();

        TransferObserver observer = util.upload(bucket,fileName , imagedata , CannedAccessControlList.PublicRead);


        observer.setTransferListener(new TransferListener() {
            @Override
            public void onStateChanged(int id, TransferState state) {
                Log.d( "State" , state.toString());
            }

            @Override
            public void onProgressChanged(int id, long bytesCurrent, long bytesTotal) {
                int percentage = (int) ((double) bytesCurrent * 100 / bytesTotal);
//                Toast.makeText(getApplicationContext(), percentage, Toast.LENGTH_SHORT).show();
//                progresss.setText(percentage);

                Log.d( "Percentage" , String.valueOf(percentage));
            }

            @Override
            public void onError(int id, Exception ex) {
//                Toast.makeText(getApplicationContext(), ex.toString(), Toast.LENGTH_SHORT).show();
            }
        });
    }

}

