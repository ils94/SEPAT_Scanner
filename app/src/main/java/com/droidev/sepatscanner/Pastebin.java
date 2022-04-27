package com.droidev.sepatscanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.text.InputType;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collectors;

public class Pastebin {

    Utils utils = new Utils();
    CaixaDialogo caixaDialogo = new CaixaDialogo();

    public String gerarChave(String login, String senha, String devKey) {

        String result = "";

        try {

            URL url = new URL("https://pastebin.com/api/api_login.php");
            URLConnection con = url.openConnection();
            HttpURLConnection http = (HttpURLConnection) con;
            http.setRequestMethod("POST");
            http.setDoOutput(true);
            http.setDoInput(true);
            Map<String, String> arguments = new HashMap<>();

            arguments.put("api_dev_key", devKey);
            arguments.put("api_user_name", login);
            arguments.put("api_user_password", senha);

            StringJoiner sj = new StringJoiner("&");

            for (Map.Entry<String, String> entry : arguments.entrySet())
                sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                        + URLEncoder.encode(entry.getValue(), "UTF-8"));

            byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
            int length = out.length;

            http.setFixedLengthStreamingMode(length);
            http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
            http.connect();

            OutputStream os = http.getOutputStream();
            os.write(out);

            InputStream is = http.getInputStream();

            result = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

        } catch (IOException e) {
            e.printStackTrace();
        }

        return result;
    }

    public void checarQrCode(Activity activity, String content) {

        File path = activity.getFilesDir();
        File qrCode = new File(path, "QRCode.png");

        if (qrCode.isFile()) {

            caixaDialogo.simples(activity, "Qr Code existente encontrado", "Foi encontrado um QR Code criado anteriormente, deseja abri-lo?", "Sim", "Não", i -> {

                if (i.equals("true")) {

                    Intent myIntent = new Intent(activity.getBaseContext(), QRCodeActivity.class);
                    myIntent.putExtra("content", String.valueOf(qrCode));
                    activity.startActivity(myIntent);
                } else if (i.equals("false")) {

                    gerarQRCode(activity, content);
                }
            });
        } else {

            gerarQRCode(activity, content);
        }

    }

    public void gerarQRCode(Activity activity, String content) {

        new Thread(() -> {

            TinyDB tinyDB = new TinyDB(activity.getBaseContext());

            String login = tinyDB.getString("login");
            String senha = tinyDB.getString("senha");
            String devKey = tinyDB.getString("devKey");

            if (login.isEmpty() || senha.isEmpty() || devKey.isEmpty()) {

                activity.runOnUiThread(() -> Toast.makeText(activity.getBaseContext(), "Erro, salve uma conta pastebin primeiro", Toast.LENGTH_SHORT).show());

            } else {

                try {

                    String userKey = gerarChave(login, senha, devKey);

                    URL url = new URL("https://pastebin.com/api/api_post.php");
                    URLConnection con = url.openConnection();
                    HttpURLConnection http = (HttpURLConnection) con;
                    http.setRequestMethod("POST");
                    http.setDoOutput(true);
                    http.setDoInput(true);
                    Map<String, String> arguments = new HashMap<>();

                    arguments.put("api_dev_key", devKey);
                    arguments.put("api_user_key", userKey);
                    arguments.put("api_option", "paste");
                    arguments.put("api_paste_code", content);
                    arguments.put("api_paste_expire_date", "1D");

                    StringJoiner sj = new StringJoiner("&");

                    for (Map.Entry<String, String> entry : arguments.entrySet())
                        sj.add(URLEncoder.encode(entry.getKey(), "UTF-8") + "="
                                + URLEncoder.encode(entry.getValue(), "UTF-8"));

                    byte[] out = sj.toString().getBytes(StandardCharsets.UTF_8);
                    int length = out.length;

                    http.setFixedLengthStreamingMode(length);
                    http.setRequestProperty("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8");
                    http.connect();

                    OutputStream os = http.getOutputStream();
                    os.write(out);

                    InputStream is = http.getInputStream();

                    String result = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8)).lines().collect(Collectors.joining("\n"));

                    System.out.println(result);

                    Intent myIntent = new Intent(activity.getBaseContext(), QRCodeActivity.class);
                    myIntent.putExtra("content", result);
                    activity.startActivity(myIntent);

                } catch (IOException e) {

                    e.printStackTrace();

                    activity.runOnUiThread(() -> Toast.makeText(activity.getBaseContext(), "Ocorreu um erro", Toast.LENGTH_SHORT).show());
                }
            }
        }).start();
    }

    public void salvarPastebinLogin(Context context) {

        EditText login = new EditText(context);
        login.setHint("Seu login");
        login.setInputType(InputType.TYPE_CLASS_TEXT);

        EditText senha = new EditText(context);
        senha.setHint("Sua senha");
        senha.setInputType(InputType.TYPE_CLASS_TEXT);

        EditText devKey = new EditText(context);
        devKey.setHint("Sua chave dev api");
        devKey.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout lay = new LinearLayout(context);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.addView(login);
        lay.addView(senha);
        lay.addView(devKey);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle("Salvar Conta Pastebin")
                .setMessage("Insira seu login, senha e api_dev_key para salvar.")
                .setPositiveButton("Ok", null)
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Limpar Tudo", null)
                .setView(lay)
                .show();

        TinyDB tinyDB = new TinyDB(context);

        login.setText(tinyDB.getString("login"));
        senha.setText(tinyDB.getString("senha"));
        devKey.setText(tinyDB.getString("devKey"));

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        positiveButton.setOnClickListener(v -> {

            String loginString = login.getText().toString();
            String senhaString = senha.getText().toString();
            String devKeyString = devKey.getText().toString();

            if (!loginString.equals("") && !senhaString.equals("") && !devKeyString.equals("")) {

                tinyDB.remove("login");
                tinyDB.remove("senha");
                tinyDB.remove("devKey");

                tinyDB.putString("login", loginString);
                tinyDB.putString("senha", senhaString);
                tinyDB.putString("devKey", devKeyString);

                Toast.makeText(context, "Conta salva", Toast.LENGTH_SHORT).show();

                dialog.dismiss();

            } else {

                Toast.makeText(context, "Erro, campo vazio", Toast.LENGTH_SHORT).show();
            }
        });

        neutralButton.setOnClickListener(v -> {

            login.setText("");
            senha.setText("");
            devKey.setText("");
        });
    }

    @SuppressLint("SetTextI18n")
    public void pastebin(Activity activity, String url, EditText editText, TextView textView) {

        editText.setText("Buscando no pastebin...");

        new Thread(() -> {

            final StringBuilder sb = new StringBuilder();

            try {

                Document doc = Jsoup.connect(url).get();

                String text = doc.select("textarea[class=textarea]").text().replace(",", ": ") + "\n";

                sb.append(text);

                activity.runOnUiThread(() -> {

                    editText.setText(sb);

                    utils.contadorLinhas(editText, textView);

                    utils.manterNaMemoria(activity, editText.getText().toString(), "bens.txt");
                });

            } catch (Exception e) {
                e.printStackTrace();

                editText.post(() -> editText.setText(e.toString()));
            }
        }).start();
    }
}
