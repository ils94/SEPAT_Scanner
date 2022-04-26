package com.droidev.sepatscanner;


import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;


import android.annotation.SuppressLint;
import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

import com.journeyapps.barcodescanner.CaptureManager;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;

public class ScannerActivity extends AppCompatActivity implements DecoratedBarcodeView.TorchListener {

    private CaptureManager capture;
    private DecoratedBarcodeView scanner;
    private String rotation, flash;
    private Utils utils;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scanner);

        setTitle("ESCANEAR");

        utils = new Utils();

        flash = utils.carregarConfigScanner(ScannerActivity.this, "flash", "rotation")[0];
        rotation = utils.carregarConfigScanner(ScannerActivity.this, "flash", "rotation")[1];

        scanner = findViewById(R.id.scanner);
        scanner.setTorchListener(this);

        capture = new CaptureManager(this, scanner);
        capture.initializeFromIntent(getIntent(), savedInstanceState);
        capture.decode();

        load();
    }

    public void switchFlashlight() {

        if (flash.equals("Off")) {

            scanner.setTorchOn();
        } else {

            scanner.setTorchOff();
        }
    }

    public void load() {

        if (flash.equals("On")) {

            scanner.setTorchOn();
        } else {

            scanner.setTorchOff();
        }

        if (rotation.equals("Portrait")) {

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

        } else {

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

        }

        capture = new CaptureManager(this, scanner);
        capture.decode();
    }

    public void rotate() {
        if (rotation.equals("Portrait")) {

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);

            capture = new CaptureManager(this, scanner);
            capture.decode();

            rotation = "Landscape";

        } else {

            setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);

            capture = new CaptureManager(this, scanner);
            capture.decode();

            rotation = "Portrait";

        }

        utils.salvarConfigScanner(ScannerActivity.this, "flash", flash, "rotation", rotation);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_scanner, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.flash:

                switchFlashlight();

                return true;

            case R.id.rotation:

                rotate();

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    public void onTorchOn() {
        flash = "On";

        utils.salvarConfigScanner(ScannerActivity.this, "flash", flash, "rotation", rotation);
    }

    @Override
    public void onTorchOff() {
        flash = "Off";

        utils.salvarConfigScanner(ScannerActivity.this, "flash", flash, "rotation", rotation);
    }

    @Override
    protected void onResume() {
        super.onResume();
        capture.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        capture.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        capture.onDestroy();
    }

    @Override
    protected void onStart() {
        super.onStart();

    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        capture.onSaveInstanceState(outState);
    }
}