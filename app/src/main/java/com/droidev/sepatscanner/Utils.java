package com.droidev.sepatscanner;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.core.content.FileProvider;

import com.google.zxing.integration.android.IntentIntegrator;

import org.w3c.dom.Text;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;

public class Utils {

    Boolean boo = false;

    public void realcarTexto(TextView tv, String textToHighlight, TextView tv2) {

        tv2.setText("Procurando...");

        final int[] count = {0};

        new Thread(new Runnable() {
            @Override
            public void run() {

                tv.post(new Runnable() {
                    @Override
                    public void run() {

                        SpannableString spannableString = new SpannableString(tv.getText().toString());
                        BackgroundColorSpan backgroundSpan = new BackgroundColorSpan(Color.WHITE);
                        spannableString.setSpan(backgroundSpan, 0, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                        tv.setText(spannableString);

                        String tvt = tv.getText().toString();

                        int ofe = tvt.indexOf(textToHighlight, 0);
                        Spannable wordToSpan = new SpannableString(tv.getText());
                        for (int ofs = 0; ofs < tvt.length() && ofe != -1; ofs = ofe + 1) {
                            ofe = tvt.indexOf(textToHighlight, ofs);

                            count[0] = count[0] + 1;

                            if (ofe == -1)
                                break;
                            else {

                                wordToSpan.setSpan(new BackgroundColorSpan(Color.YELLOW), ofe, ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                                tv.setText(wordToSpan, TextView.BufferType.SPANNABLE);
                            }
                        }

                        count[0] = count[0] - 1;
                    }
                });

                tv2.post(new Runnable() {
                    @Override
                    public void run() {

                        if (count[0] < 0) {

                            tv2.setText("ACHADOS: " + 0);
                        } else {

                            tv2.setText("ACHADOS: " + count[0]);
                        }
                    }
                });
            }
        }).start();
    }

    public void copiarTexto(Context context, String string) {

        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("Copiado", string);
        clipboard.setPrimaryClip(clip);

        Toast.makeText(context, "Copiado", Toast.LENGTH_SHORT).show();
    }

    public void campoEditavel(Context context, EditText editText) {

        if (!boo) {

            boo = true;

            editText.setFocusableInTouchMode(true);
            editText.setCursorVisible(true);

            Toast.makeText(context, "Campo editável", Toast.LENGTH_SHORT).show();
        } else {

            boo = false;

            editText.setFocusableInTouchMode(false);
            editText.clearFocus();
            editText.setCursorVisible(false);

            Toast.makeText(context, "Campo não editável", Toast.LENGTH_SHORT).show();

            InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(editText.getWindowToken(), 0);
        }
    }

    public void contadorLinhas(EditText relacao, TextView relacaoTV) {

        int contador;

        contador = relacao.getLineCount() - 1;

        relacaoTV.setText(contador + " ITENS");
    }

    public void scanner(Activity context) {

        IntentIntegrator intentIntegrator = new IntentIntegrator(context);
        intentIntegrator.setPrompt("Aponte a câmera para o código de barras ou QR code");
        intentIntegrator.setCaptureActivity(ScannerActivity.class);
        intentIntegrator.setCameraId(0);
        intentIntegrator.initiateScan();
    }

    public void salvarConfigScanner(Context context, String flashKey, String flash, String rotationKey, String rotation) {

        TinyDB tinydb = new TinyDB(context);

        tinydb.remove(flashKey);
        tinydb.remove(rotationKey);

        tinydb.putString(flashKey, flash);
        tinydb.putString(rotationKey, rotation);

    }

    public String[] carregarConfigScanner(Context context, String flashKey, String rotationKey) {

        TinyDB tinydb = new TinyDB(context);

        String tinyFlashKey = tinydb.getString(flashKey);
        String tinyRotationKey = tinydb.getString(rotationKey);

        if (tinyFlashKey.isEmpty()) {

            tinyFlashKey = "Off";
        }
        if (tinyRotationKey.isEmpty()) {

            tinyRotationKey = "Portrait";
        }

        return new String[]{tinyFlashKey, tinyRotationKey};
    }

    public void manterNaMemoria(Context context, String content, String file) {

        File path = context.getFilesDir();
        try {
            FileOutputStream writer = new FileOutputStream(new File(path, file));
            writer.write(content.getBytes());
            writer.close();
        } catch (Exception e) {
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public String recuperarDaMemoria(Context context, String filename) {
        File path = context.getFilesDir();
        File readFrom = new File(path, filename);
        byte[] content = new byte[(int) readFrom.length()];

        try {

            FileInputStream stream = new FileInputStream(readFrom);
            stream.read(content);
            return new String(content);

        } catch (Exception e) {
            return "";
        }
    }
}
