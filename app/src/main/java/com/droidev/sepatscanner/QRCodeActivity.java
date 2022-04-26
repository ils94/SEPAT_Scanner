package com.droidev.sepatscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.WriterException;
import com.journeyapps.barcodescanner.BarcodeEncoder;

import java.io.FileOutputStream;

public class QRCodeActivity extends AppCompatActivity {

    ImageView imageView;
    Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qrcode);

        utils = new Utils();

        imageView = findViewById(R.id.qrcode);

        setTitle("Pastebin QR Code");

        Intent intent = getIntent();
        String content = intent.getStringExtra("content");

        if (content.contains("https://pastebin.com")) {

            try {

                BarcodeEncoder barcodeEncoder = new BarcodeEncoder();

                Bitmap bitmap = barcodeEncoder.encodeBitmap(content, BarcodeFormat.QR_CODE, 512, 512);

                salvarQRCode(bitmap);

                imageView.setImageBitmap(bitmap);

            } catch (WriterException e) {
                e.printStackTrace();
            }
        } else {

            imageView.setImageBitmap(BitmapFactory.decodeFile(content));
        }
    }

    public void salvarQRCode(Bitmap bmp) {

        try {
            FileOutputStream fileOutputStream = openFileOutput("QRCode.png", Context.MODE_PRIVATE);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, fileOutputStream);
            fileOutputStream.close();
        } catch (Exception e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}