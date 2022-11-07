package com.droidev.sepatscanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.BackgroundColorSpan;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.integration.android.IntentIntegrator;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class Utils {

    Boolean boo = false;

    @SuppressLint("SetTextI18n")
    public void procurarTexto(EditText et, String textToHighlight, TextView tv2) {

        tv2.setText("Procurando...");

        final int[] count = {0};

        new Thread(() -> {

            et.post(() -> {

                SpannableString spannableString = new SpannableString(et.getText().toString());
                BackgroundColorSpan backgroundSpan = new BackgroundColorSpan(Color.WHITE);
                spannableString.setSpan(backgroundSpan, 0, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                et.setText(spannableString);

                String tvt = et.getText().toString();

                int ofe = tvt.indexOf(textToHighlight);
                Spannable wordToSpan = new SpannableString(et.getText());
                for (int ofs = 0; ofs < tvt.length() && ofe != -1; ofs = ofe + 1) {
                    ofe = tvt.indexOf(textToHighlight, ofs);

                    count[0] = count[0] + 1;

                    if (ofe == -1) {
                        break;
                    } else {

                        wordToSpan.setSpan(new BackgroundColorSpan(Color.YELLOW), ofe, ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                        et.setText(wordToSpan, TextView.BufferType.SPANNABLE);
                    }
                }

                count[0] = count[0] - 1;
            });

            tv2.post(() -> {

                if (count[0] < 0) {

                    tv2.setText("ACHADOS: " + 0);
                } else {

                    tv2.setText("ACHADOS: " + count[0]);
                }
            });
        }).start();
    }

    public void realcarTexto(EditText et, String textToHighlight) {

        SpannableString spannableString = new SpannableString(et.getText().toString());
        BackgroundColorSpan backgroundSpan = new BackgroundColorSpan(Color.WHITE);
        spannableString.setSpan(backgroundSpan, 0, spannableString.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        et.setText(spannableString);

        String tvt = et.getText().toString();

        int ofe = tvt.indexOf(textToHighlight);
        Spannable wordToSpan = new SpannableString(et.getText());
        for (int ofs = 0; ofs < tvt.length() && ofe != -1; ofs = ofe + 1) {
            ofe = tvt.indexOf(textToHighlight, ofs);

            if (ofe == -1) {
                break;
            } else {

                wordToSpan.setSpan(new BackgroundColorSpan(Color.YELLOW), ofe, ofe + textToHighlight.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                et.setText(wordToSpan, TextView.BufferType.SPANNABLE);
            }
        }
    }

    public void autoScroll(ScrollView scrollView, EditText editText, String s) {

        editText.post(() -> {

            realcarTexto(editText, s);

            int index = editText.getText().toString().indexOf(s);

            int line = editText.getLayout().getLineForOffset(index);

            int y = editText.getLayout().getLineTop(line);

            scrollView.smoothScrollTo(0, y);

        });
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

    @SuppressLint("SetTextI18n")
    public void contadorLinhas(EditText editText, TextView textView) {

        int contador;

        contador = editText.getLineCount() - 1;

        textView.setText(contador + " ITENS");
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

    public String remover45(String string) {

        StringBuilder sb = new StringBuilder();

        sb.append(string);

        sb.deleteCharAt(0);
        sb.deleteCharAt(0);

        return sb.toString();
    }

    public void csvDataStream(Activity activity, EditText editText, Uri data) {

        try {
            InputStream inputStream = activity.getContentResolver().openInputStream(data);
            BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));

            StringBuilder relacao = new StringBuilder();

            editText.setText("");

            String mLine;

            while ((mLine = r.readLine()) != null) {
                if (!mLine.equals("")) {
                    relacao.append(mLine.toUpperCase().replace(",", " : ").replace("  ", " ")).append("\n");
                }
            }

            editText.setText(relacao);

        } catch (IOException e) {
            e.printStackTrace();
            Toast.makeText(activity.getBaseContext(), e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
}
