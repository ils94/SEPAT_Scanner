package com.droidev.sepatscanner;

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

import org.json.JSONException;
import org.json.JSONObject;
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

    CaixaDialogo caixaDialogo = new CaixaDialogo();
    Utils utils = new Utils();

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
                    arguments.put("api_paste_expire_date", "10M");

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

                    result = result.replace("https://pastebin.com/", "");

                    result = "https://pastebin.com/raw/" + result;

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

        EditText elemento = new EditText(context);
        elemento.setHint("Elemento da pagina");
        elemento.setInputType(InputType.TYPE_CLASS_TEXT);

        LinearLayout lay = new LinearLayout(context);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.addView(login);
        lay.addView(senha);
        lay.addView(devKey);
        lay.addView(elemento);

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
        elemento.setText(tinyDB.getString("elemento"));

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        positiveButton.setOnClickListener(v -> {

            String loginString = login.getText().toString();
            String senhaString = senha.getText().toString();
            String devKeyString = devKey.getText().toString();
            String elementoString = elemento.getText().toString();

            if (!loginString.equals("") && !senhaString.equals("") && !devKeyString.equals("") && !elementoString.equals("")) {

                tinyDB.remove("login");
                tinyDB.remove("senha");
                tinyDB.remove("devKey");
                tinyDB.remove("elemento");

                tinyDB.putString("login", loginString);
                tinyDB.putString("senha", senhaString);
                tinyDB.putString("devKey", devKeyString);
                tinyDB.putString("elemento", elementoString);

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

    public void pastebin(Activity activity, String url, EditText editText, TextView textView) {

        Toast.makeText(activity, "Buscando no Pastebin...", Toast.LENGTH_SHORT).show();

        new Thread(() -> {

            final StringBuilder sb = new StringBuilder();

            try {

                TinyDB tinyDB = new TinyDB(activity.getBaseContext());

                Document doc = Jsoup.connect(url)
                        .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/105.0.0.0 Safari/537.36")
                        .get();

                String text = doc.select(tinyDB.getString("elemento")).text();

                sb.append(text);

                activity.runOnUiThread(() -> {

                    try {
                        JSONObject jsonObject = new JSONObject(String.valueOf(sb));

                        editText.setText(jsonObject.getString("relacao"));

                        utils.contadorLinhas(editText, textView);

                        utils.manterNaMemoria(activity, editText.getText().toString(), "relacao.txt");

                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                });

            } catch (Exception e) {

                activity.runOnUiThread(() -> Toast.makeText(activity, e.toString(), Toast.LENGTH_SHORT).show());
            }
        }).start();
    }
}
