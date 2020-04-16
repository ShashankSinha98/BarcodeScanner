package com.example.barcodescanner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.ml.vision.FirebaseVision;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcode;
import com.google.firebase.ml.vision.barcode.FirebaseVisionBarcodeDetector;
import com.google.firebase.ml.vision.common.FirebaseVisionImage;
import com.google.firebase.ml.vision.text.FirebaseVisionTextRecognizer;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.util.List;

public class MainActivity extends AppCompatActivity {

    private String TAG = MainActivity.class.getSimpleName();
    private ImageView imageView;
    private Bitmap bitmap;
    private FirebaseVisionImage firebaseVisionImage;
    private FirebaseVisionBarcodeDetector barcodeDetector;
    private TextView resultDisplayTV;
    GraphicOverlay mGraphicOverlay;
    private Button chooseImage, scanCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Getting ID's
        imageView = findViewById(R.id.text_image);
        resultDisplayTV = findViewById(R.id.results_tv);
        chooseImage = findViewById(R.id.add_img_btn);
        scanCode = findViewById(R.id.scan_code_btn);
        mGraphicOverlay = findViewById(R.id.graphicOverlay);


        // Choose Img
        chooseImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {

                if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
                {
                    if(ContextCompat.checkSelfPermission(MainActivity.this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
                    {
                        ActivityCompat.requestPermissions(MainActivity.this,new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},1);

                    } else {

                        bringImagePicker();
                    }
                } else {

                    bringImagePicker();
                }
            }
        });


        // Search Code
        scanCode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                resultDisplayTV.setText("");

                // Getting bitmap from Imageview
                BitmapDrawable drawable = (BitmapDrawable) imageView.getDrawable();
                bitmap = drawable.getBitmap();

                runBarcodeRecoginition();
            }
        });

    }

    private void runBarcodeRecoginition() {

        firebaseVisionImage = FirebaseVisionImage.fromBitmap(bitmap);

        barcodeDetector = FirebaseVision.getInstance().getVisionBarcodeDetector();

        barcodeDetector.detectInImage(firebaseVisionImage)
                .addOnSuccessListener(new OnSuccessListener<List<FirebaseVisionBarcode>>() {
                    @Override
                    public void onSuccess(List<FirebaseVisionBarcode> firebaseVisionBarcodes) {

                        chooseImage.setEnabled(true);
                        scanCode.setEnabled(true);

                        processBarcodeRecognitionResult(firebaseVisionBarcodes);
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                chooseImage.setEnabled(false);
                scanCode.setEnabled(false);
                resultDisplayTV.setText("Error : "+e.getMessage());
            }
        });

    }

    private void processBarcodeRecognitionResult(List<FirebaseVisionBarcode> barcodes) {

        if(barcodes.size()==0){
            resultDisplayTV.setText("No Barcode Found");
            return;
        }


        for (FirebaseVisionBarcode barcode: barcodes) {
            Rect bounds = barcode.getBoundingBox();
            Point[] corners = barcode.getCornerPoints();

            String rawValue = barcode.getRawValue();
            resultDisplayTV.setText(resultDisplayTV.getText()+"\n"+rawValue);

            GraphicOverlay.Graphic barcodeGraphic =new BarcodeGraphic(mGraphicOverlay);
            mGraphicOverlay.add(barcodeGraphic);

            int valueType = barcode.getValueType();
            // See API reference for complete list of supported types
            switch (valueType) {
                case FirebaseVisionBarcode.TYPE_WIFI:
                    String ssid = barcode.getWifi().getSsid();
                    String password = barcode.getWifi().getPassword();
                    int type = barcode.getWifi().getEncryptionType();

                    Log.d(TAG+" TYPE_WIFI : ssid ", ssid);
                    Log.d(TAG+" TYPE_WIFI : password ", password);
                    Log.d(TAG+" TYPE_WIFI : type", String.valueOf(type));

                    break;

                case FirebaseVisionBarcode.TYPE_URL:
                    String title = barcode.getUrl().getTitle();
                    String url = barcode.getUrl().getUrl();

                    Log.d(TAG+" TYPE_URL : title ", title);
                    Log.d(TAG+" TYPE_URL : url ", url);

                    break;
            }
        }
    }


    private void bringImagePicker() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON).setAspectRatio(1,1)
                .setCropShape(Build.VERSION.SDK_INT >= Build.VERSION_CODES.P ? CropImageView.CropShape.RECTANGLE : CropImageView.CropShape.OVAL)
                .start(MainActivity.this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                resultDisplayTV.setText("");
                mGraphicOverlay.clear();
                imageView.setImageURI(result.getUri());


            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {

                Exception error = result.getError();
                resultDisplayTV.setText("Error Loading Image: "+error);

            }
        }
    }
}
