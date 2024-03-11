package com.droidev.sepatscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

public class MainActivity extends AppCompatActivity {

    Button scan, ok;

    EditText relacao, patrimonio, numSerie;

    TextView relacaoTV;

    ScrollView relacaoScrollView;

    String modo = "padrao", ultimo, atual, newIntentResult;

    Boolean ultimoItem = false, voltarItem = false, quickScan = false;

    Utils utils;
    CaixaDialogo caixaDialogo;
    Arquivos arquivos;
    Pastebin pastebin;
    JSON json;

    TinyDB tinyDB;

    String num_sequencia = "";

    private static final int LER_ARQUIVO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        utils = new Utils();
        caixaDialogo = new CaixaDialogo();
        arquivos = new Arquivos();
        pastebin = new Pastebin();
        json = new JSON();

        tinyDB = new TinyDB(MainActivity.this);

        AppCompatDelegate.setDefaultNightMode(
                AppCompatDelegate.MODE_NIGHT_YES);

        this.setTitle("SEPAT Scanner");

        scan = findViewById(R.id.scan);
        ok = findViewById(R.id.ok);

        relacaoTV = findViewById(R.id.relacaoTV);
        relacao = findViewById(R.id.relacao);
        patrimonio = findViewById(R.id.patrimonio);
        numSerie = findViewById(R.id.NumSerie);

        relacaoScrollView = findViewById(R.id.relacaoScrollView);

        patrimonio.setEnabled(false);
        numSerie.setEnabled(false);
        ok.setEnabled(false);

        scan.setOnClickListener(v -> utils.scanner(MainActivity.this));

        ok.setOnClickListener(v -> {

            String s = StringUtils.leftPad(patrimonio.getText().toString(), 6, '0');

            if (patrimonio.getText().toString().equals("") || numSerie.getText().toString().equals("")) {

                Toast.makeText(MainActivity.this, "Os campos Patrimônio e Número de série não podem está vazios", Toast.LENGTH_SHORT).show();
            } else if (relacao.getText().toString().contains(patrimonio.getText())) {

                Toast.makeText(getBaseContext(), patrimonio.getText() + " Já foi escaneado", Toast.LENGTH_LONG).show();
            } else {

                verificarSequencia(s);
            }
        });

        String content = utils.recuperarDaMemoria(MainActivity.this, "relacao.txt");

        relacao.setText(content);
        relacao.setFocusableInTouchMode(false);
        relacao.clearFocus();
        relacao.setCursorVisible(false);

        if (!tinyDB.getString("Fonte").isEmpty()) {

            relacao.setTextSize(TypedValue.COMPLEX_UNIT_SP, Integer.parseInt(tinyDB.getString("Fonte")));
        }

        Intent intent = getIntent();

        Uri data = intent.getData();

        if (data != null) {

            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setCancelable(false);
            builder.setTitle("Abrir nova relação");
            builder.setMessage("Abrir uma nova relação irá apagar tudo da relação atual no App. Deseja continuar?");

            builder.setPositiveButton("ABRIR", (dialog, which) -> {

                if (intent.getType().equals("text/comma-separated-values") || intent.getType().equals("text/csv")) {

                    utils.csvDataStream(MainActivity.this, relacao, data);

                }

                contadorLinhas();
            });

            builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

            AlertDialog dialog = builder.create();
            dialog.show();
        }

        if (tinyDB.getString("modo") != null) {

            if (!tinyDB.getString("modo").equals("")) {

                modo = tinyDB.getString("modo");

                switch (modo) {
                    case "padrao":

                        patrimonio.setEnabled(false);
                        numSerie.setEnabled(false);
                        ok.setEnabled(false);

                        tinyDB.putString("modo", modo);

                        setTitle("Modo: Padrão");

                        break;
                    case "descricao":

                        patrimonio.setEnabled(false);
                        numSerie.setEnabled(false);
                        ok.setEnabled(false);

                        tinyDB.putString("modo", modo);

                        setTitle("Modo: P com D");

                        break;
                    case "checking":

                        patrimonio.setEnabled(false);
                        numSerie.setEnabled(false);
                        ok.setEnabled(false);

                        tinyDB.putString("modo", modo);

                        setTitle("Modo: Checking");

                        break;
                    case "ns":

                        patrimonio.setEnabled(true);
                        numSerie.setEnabled(true);
                        ok.setEnabled(true);

                        tinyDB.putString("modo", modo);

                        setTitle("Modo: N° de Série");

                        break;
                }
            } else {

                setTitle("Modo: Padrão");
            }
        }

    }

    @Override
    protected void onStart() {
        super.onStart();

        esperar();
    }

    @Override
    public void onBackPressed() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Sair");
        builder.setMessage("Desejar sair da aplicação?");

        builder.setPositiveButton("Sair e Salvar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                manterNaMemoria();

                MainActivity.this.finish();
            }
        });
        builder.setNegativeButton("Sair sem Salvar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                MainActivity.this.finish();
            }
        });
        builder.setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {

                dialog.cancel();
            }
        });

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu, menu);
        return true;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {

            case R.id.procurar:

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);
                builder.setTitle("Procurar");

                final EditText input = new EditText(this);
                input.setInputType(InputType.TYPE_CLASS_TEXT);
                builder.setView(input);

                builder.setPositiveButton("PROCURAR", (dialog, which) -> {
                    String text = input.getText().toString();

                    if (!text.isEmpty()) {

                        utils.procurarTexto(relacao, text.toUpperCase(), relacaoTV);
                    }
                });
                builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

                AlertDialog dialog = builder.create();
                dialog.show();

                return true;

            case R.id.copiar:

                utils.copiarTexto(MainActivity.this, relacao.getText().toString());

                return true;

            case R.id.manual:

                inserirManualmente();

                return true;

            case R.id.editavel:

                utils.campoEditavel(MainActivity.this, relacao);

                return true;

            case R.id.enviar:

                arquivos.enviarArquivo(MainActivity.this, relacao.getText().toString());

                return true;

            case R.id.abrir:

                arquivos.abrirArquivo(MainActivity.this, LER_ARQUIVO);

                return true;

            case R.id.salvar:

                arquivos.salvarArquivo(MainActivity.this, 3);

                return true;

            case R.id.fonte:

                tamanhoFonte();

                return true;

            case R.id.contarLinhas:

                contadorLinhas();

                return true;

            case R.id.forcarSalvar:

                caixaDialogo.simples(MainActivity.this, "Salvar", "Salvar todas as alterações na relação atual?", "Sim", "Não", i -> {
                    if (i.equals("true")) {

                        manterNaMemoria();

                        Toast.makeText(getBaseContext(), "Salvo", Toast.LENGTH_SHORT).show();
                    }
                });

                return true;

            case R.id.autoScan:

                if (quickScan) {

                    quickScan = false;

                    Toast.makeText(this, "Quick Scan OFF.", Toast.LENGTH_SHORT).show();
                } else {

                    quickScan = true;

                    Toast.makeText(this, "Quick Scan ON.", Toast.LENGTH_SHORT).show();
                }

                return true;

            case R.id.padrao:

                modo = "padrao";

                patrimonio.setEnabled(false);
                numSerie.setEnabled(false);
                ok.setEnabled(false);

                tinyDB.putString("modo", modo);

                setTitle("Modo: Padrão");

                return true;

            case R.id.descricao:

                modo = "descricao";

                patrimonio.setEnabled(false);
                numSerie.setEnabled(false);
                ok.setEnabled(false);

                tinyDB.putString("modo", modo);

                setTitle("Modo: P com D");

                return true;

            case R.id.checking:

                modo = "checking";

                patrimonio.setEnabled(false);
                numSerie.setEnabled(false);
                ok.setEnabled(false);

                tinyDB.putString("modo", modo);

                setTitle("Modo: Checking");

                return true;

            case R.id.ns:

                modo = "ns";

                patrimonio.setEnabled(true);
                numSerie.setEnabled(true);
                ok.setEnabled(true);

                tinyDB.putString("modo", modo);

                setTitle("Modo: N° de Série");

                return true;

            case R.id.apagar:

                caixaDialogo.simples(MainActivity.this, "Apagar", "Apagar todos os campos?", "Sim", "Cancelar", i -> {

                    if (i.equals("true")) {

                        relacao.setText("");
                        patrimonio.setText("");
                        numSerie.setText("");

                        contadorLinhas();

                        num_sequencia = "";
                    }
                });

                return true;

            case R.id.apagarUltimo:

                if (ultimoItem) {

                    relacao.setText(ultimo);

                    contadorLinhas();
                }

                return true;

            case R.id.voltarUltimo:

                if (voltarItem) {

                    relacao.setText(atual);

                    contadorLinhas();
                }

                return true;

            case R.id.gerarQrCode:

                pastebin.checarQrCode(MainActivity.this, json.criarJson(MainActivity.this, relacao.getText().toString()).toString());

                return true;

            case R.id.salvarConta:

                pastebin.salvarPastebinLogin(MainActivity.this);

                return true;

            case R.id.criarConta:

                Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://pastebin.com/signup"));
                startActivity(i);

                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        IntentResult intentResult = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);

        if (intentResult != null) {
            if (intentResult.getContents() == null) {
                Toast.makeText(getBaseContext(), "Cancelado", Toast.LENGTH_SHORT).show();
            } else {

                newIntentResult = intentResult.getContents();

                if (newIntentResult.contains("APLC:TSE") || newIntentResult.contains("TIPO=UE")) {

                    newIntentResult = newIntentResult.substring(Math.max(0, newIntentResult.length() - 8));
                }


                if (!intentResult.getContents().contains("pastebin")) {

                    newIntentResult = newIntentResult.replace(".", "").replace(",", "");

                    newIntentResult = StringUtils.leftPad(newIntentResult, 6, '0');

                    if (newIntentResult.length() == 10 && newIntentResult.startsWith("45")) {

                        newIntentResult = utils.filtrarDigitos(newIntentResult);

                    } else if (newIntentResult.length() == 8 && newIntentResult.startsWith("57")) {

                        newIntentResult = utils.filtrarDigitos(newIntentResult);
                    }
                }

                switch (modo) {

                    case "padrao":

                        if (relacao.getText().toString().contains(newIntentResult)) {

                            Toast.makeText(getBaseContext(), newIntentResult + " Já foi escaneado", Toast.LENGTH_LONG).show();

                            utils.autoScroll(relacaoScrollView, relacao, newIntentResult);

                        } else if (newIntentResult.contains("pastebin")) {

                            caixaDialogo.simples(MainActivity.this, "Carregar nova relação", "Carregar uma nova relação do pastebin?", "Sim", "Cancelar", i -> {

                                if (i.equals("true")) {

                                    pastebin.pastebin(MainActivity.this, newIntentResult, relacao, relacaoTV);
                                }
                            });

                        } else {

                            ultimoRelacao();

                            relacao.append(newIntentResult + "\n");

                            manterNaMemoria();

                            contadorLinhas();

                            atualRelacao();

                            ultimoItem = true;

                            voltarItem = true;
                        }

                        autoScan(newIntentResult);

                        break;

                    case "descricao":

                        if (relacao.getText().toString().contains(newIntentResult)) {

                            Toast.makeText(getBaseContext(), newIntentResult + " Já foi escaneado", Toast.LENGTH_LONG).show();

                            utils.autoScroll(relacaoScrollView, relacao, newIntentResult);

                        } else {

                            caixaDialogo.simplesComView(MainActivity.this, "Descrição", "Patrimônio: " + newIntentResult + "\n\nInsira a descrição do bem abaixo:", "Exemplo: mesa reta", "Ok", "Cancelar", InputType.TYPE_CLASS_TEXT, true, false, i -> {

                                ultimoRelacao();

                                relacao.append(i.toUpperCase() + " : " + newIntentResult + "\n");

                                manterNaMemoria();

                                contadorLinhas();

                                atualRelacao();

                                ultimoItem = true;

                                voltarItem = true;
                            });
                        }

                        break;

                    case "ns":

                        if (relacao.getText().toString().contains(newIntentResult)) {

                            Toast.makeText(getBaseContext(), newIntentResult + " Já foi escaneado", Toast.LENGTH_LONG).show();

                            utils.autoScroll(relacaoScrollView, relacao, newIntentResult);

                        } else {

                            if (patrimonio.getText().toString().equals("")) {

                                patrimonio.setText(newIntentResult);
                            } else {

                                if (numSerie.getText().toString().equals("")) {

                                    numSerie.setText(newIntentResult);
                                } else {

                                    caixaDialogo.simples(MainActivity.this, "Atenção", "Substituir o número de série atual por ''" + newIntentResult + "'' ?", "Sim", "Cancelar", i -> {

                                        if (i.equals("true")) {

                                            numSerie.setText(newIntentResult);
                                        }
                                    });
                                }
                            }
                        }
                        break;

                    case "checking":

                        if (relacao.getText().toString().contains(newIntentResult + " : [OK]")) {

                            Toast.makeText(getBaseContext(), newIntentResult + " consta na lista, e já foi escaneado", Toast.LENGTH_LONG).show();

                        } else if (relacao.getText().toString().contains(newIntentResult)) {

                            ultimoRelacao();

                            String relacao_check = relacao.getText().toString().replace(newIntentResult, newIntentResult + " : [OK]");

                            relacao.setText(relacao_check);

                            Toast.makeText(getBaseContext(), newIntentResult + " consta na relação", Toast.LENGTH_LONG).show();

                            manterNaMemoria();

                            atualRelacao();

                            ultimoItem = true;

                            voltarItem = true;

                        } else {

                            Toast.makeText(getBaseContext(), newIntentResult + " não consta na relação", Toast.LENGTH_LONG).show();
                        }

                        utils.autoScroll(relacaoScrollView, relacao, newIntentResult);

                        autoScan(newIntentResult);

                        break;
                }
            }

        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }

        if (requestCode == LER_ARQUIVO
                && resultCode == Activity.RESULT_OK) {
            Uri uri;
            if (data != null) {
                uri = data.getData();

                utils.csvDataStream(MainActivity.this, relacao, uri);
            }

            manterNaMemoria();
        }

        if (requestCode == 3) {
            if (resultCode == RESULT_OK) {
                try {

                    assert data != null;
                    Uri uri = data.getData();

                    OutputStream outputStream = getContentResolver().openOutputStream(uri);

                    outputStream.write(relacao.getText().toString().getBytes());

                    outputStream.close();

                    Toast.makeText(getBaseContext(), "Arquivo TXT salvo", Toast.LENGTH_SHORT).show();

                } catch (IOException e) {
                    Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
            } else {

                Toast.makeText(getBaseContext(), "Não foi possível salvar o arquivo TXT", Toast.LENGTH_SHORT).show();
            }
        }
    }

    public void esperar() {

        Handler handler = new Handler();
        handler.postDelayed(this::contadorLinhas, 3000);
    }

    public void ultimoRelacao() {

        ultimo = relacao.getText().toString();

        contadorLinhas();
    }

    public void atualRelacao() {

        atual = relacao.getText().toString();

        contadorLinhas();
    }

    public void contadorLinhas() {

        utils.contadorLinhas(relacao, relacaoTV);
    }

    public void inserirManualmente() {

        caixaDialogo.simplesComView(MainActivity.this, "Modo manual", "Insira o número patrimonial abaixo:", "Mínimo 6 dígitos.", "Ok", "Cancelar", InputType.TYPE_CLASS_NUMBER, false, true,
                i -> {

                    switch (modo) {

                        case "checking":

                            if (relacao.getText().toString().contains(i + " : [OK]")) {

                                Toast.makeText(getBaseContext(), i + " consta na lista, e já foi escaneado", Toast.LENGTH_LONG).show();

                            } else if (relacao.getText().toString().contains(i)) {

                                ultimoRelacao();

                                String relacao_check = relacao.getText().toString().replace(i, i + " : [OK]");

                                relacao.setText(relacao_check);

                                Toast.makeText(getBaseContext(), i + " consta na relação", Toast.LENGTH_LONG).show();

                                manterNaMemoria();

                                atualRelacao();

                                ultimoItem = true;

                                voltarItem = true;

                            } else {

                                Toast.makeText(getBaseContext(), i + " não consta na relação", Toast.LENGTH_LONG).show();
                            }

                            if (relacao.getText().toString().contains(i)) {

                                utils.autoScroll(relacaoScrollView, relacao, i);
                            }

                            break;

                        case "padrao":

                            if (relacao.getText().toString().contains(i)) {

                                Toast.makeText(getBaseContext(), i + " Já foi escaneado", Toast.LENGTH_LONG).show();

                                if (relacao.getText().toString().contains(i)) {

                                    utils.autoScroll(relacaoScrollView, relacao, i);
                                }

                            } else {

                                ultimoRelacao();

                                relacao.append(i + "\n");

                                manterNaMemoria();

                                contadorLinhas();

                                atualRelacao();

                                ultimoItem = true;

                                voltarItem = true;

                            }

                            break;

                        case "descricao":

                            if (relacao.getText().toString().contains(i)) {

                                Toast.makeText(getBaseContext(), i + " Já foi escaneado", Toast.LENGTH_LONG).show();

                                if (relacao.getText().toString().contains(i)) {

                                    utils.autoScroll(relacaoScrollView, relacao, i);
                                }

                            } else {

                                caixaDialogo.simplesComView(MainActivity.this, "Descrição", "Patrimônio: " + i + "\n\nInsira a descrição do bem abaixo:", "Exemplo: mesa reta", "Ok", "Cancelar", InputType.TYPE_CLASS_TEXT, true, false, j -> {

                                    ultimoRelacao();

                                    relacao.append(j.toUpperCase() + " : " + i + "\n");

                                    manterNaMemoria();

                                    contadorLinhas();

                                    atualRelacao();

                                    ultimoItem = true;

                                    voltarItem = true;
                                });
                            }

                            break;
                    }
                });
    }

    public void manterNaMemoria() {

        utils.manterNaMemoria(MainActivity.this, relacao.getText().toString(), "relacao.txt");
    }

    public void verificarSequencia(String s) {

        int num_comparar;

        char lastChar = s.charAt(s.length() - 1);

        int num = Integer.parseInt(String.valueOf(lastChar));

        if (num_sequencia.isEmpty()) {

            num_sequencia = String.valueOf(lastChar);

            inserirNaRelacao(s);

            patrimonio.setText("");

            numSerie.setText("");

        } else {

            num_comparar = Integer.parseInt(num_sequencia);

            if (String.valueOf(lastChar).equals("0") && num_sequencia.equals("9")) {

                num = 10;
            }

            if ((num_comparar + 1) != num) {

                caixaDialogo.simples(MainActivity.this, "Sequência Quebrada", "Parece que a sequência dos números patrimoniais foi quebrada, deseja reiniciar a sequência?", "Sim", "Não", i -> {

                    if (i.equals("true")) {

                        num_sequencia = String.valueOf(lastChar);

                        inserirNaRelacao(s);

                        patrimonio.setText("");

                        numSerie.setText("");

                    } else {

                        patrimonio.setText("");

                        numSerie.setText("");
                    }
                });

            } else {

                num_sequencia = String.valueOf(lastChar);

                inserirNaRelacao(s);

                patrimonio.setText("");

                numSerie.setText("");
            }
        }
    }

    public void inserirNaRelacao(String s) {

        ultimoRelacao();

        relacao.append(s + " : " + numSerie.getText().toString().toUpperCase() + "\n");

        manterNaMemoria();

        contadorLinhas();

        atualRelacao();

        ultimoItem = true;

        voltarItem = true;
    }

    public void autoScan(String s) {

        if (s.length() <= 8) {

            if (quickScan) {

                utils.scanner(MainActivity.this);
            }
        }
    }

    public void tamanhoFonte() {

        float scaledDensity = MainActivity.this.getResources().getDisplayMetrics().scaledDensity;
        float sp = relacao.getTextSize() / scaledDensity;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Alterar tamanho da fonte");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setHint("O tamanho atual é: " + sp);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString();

            try {

                relacao.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(text));

                TinyDB tinyDB = new TinyDB(MainActivity.this);

                tinyDB.remove("Fonte");
                tinyDB.putString("Fonte", text);

            } catch (Exception e) {

                Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

    }
}