package com.droidev.sepatscanner;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Objects;

public class MainActivity extends AppCompatActivity {

    Button scan, ok;
    EditText relacao, patrimonio, numSerie;
    TextView relacaoTV;
    ScrollView relacaoScrollView;
    String modo = "padrao", ultimo, atual, newIntentResult;
    Boolean ultimoItem = false, voltarItem = false, quickScan = false;
    Utils utils;
    Arquivos arquivos;
    Pastebin pastebin;
    JSON json;
    TinyDB tinyDB;
    String num_sequencia = "";
    ArrayList<String> historicoBens = new ArrayList<>();

    private static final int LER_ARQUIVO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        utils = new Utils();
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

                if (Objects.equals(intent.getType(), "text/comma-separated-values") || Objects.equals(intent.getType(), "text/csv")) {

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

        builder.setPositiveButton("Sair e Salvar", (dialog, which) -> {

            manterNaMemoria();

            MainActivity.this.finish();
        });
        builder.setNegativeButton("Sair sem Salvar", (dialog, which) -> MainActivity.this.finish());
        builder.setNeutralButton("Cancelar", (dialog, which) -> dialog.cancel());

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

                procurar();

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

                forcarSalvar();

                return true;

            case R.id.quickScan:

                if (quickScan) {

                    quickScan = false;

                    Toast.makeText(this, "Quick Scan: OFF", Toast.LENGTH_SHORT).show();
                } else {

                    quickScan = true;

                    Toast.makeText(this, "Quick Scan: ON", Toast.LENGTH_SHORT).show();
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

                apagarRelacao();

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

                    switchMode(newIntentResult, "scan");
                } else {

                    carregarPastebin(newIntentResult);
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
        }

        if (requestCode == 3) {
            if (resultCode == RESULT_OK) {
                try {

                    assert data != null;
                    Uri uri = data.getData();

                    assert uri != null;
                    OutputStream outputStream = getContentResolver().openOutputStream(uri);

                    assert outputStream != null;
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

        manterNaMemoria();
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

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Insira o número patrimonial abaixo:");

        final EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString();

            text = StringUtils.leftPad(text, 6, '0');

            switchMode(text, "manual");
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
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

                AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setCancelable(false);
                builder.setTitle("Sequência Quebrada");
                builder.setMessage("Parece que a sequência dos números patrimoniais foi quebrada, deseja reiniciar a sequência?");

                builder.setPositiveButton("SIM", (dialog, which) -> {

                    num_sequencia = String.valueOf(lastChar);

                    inserirNaRelacao(s);

                    patrimonio.setText("");

                    numSerie.setText("");
                });
                builder.setNegativeButton("NÃO", (dialog, which) -> {

                    patrimonio.setText("");

                    numSerie.setText("");
                });

                AlertDialog dialog = builder.create();
                dialog.show();

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

    public void apagarRelacao() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Apagar");
        builder.setMessage("Apagar todos os campos?");

        builder.setPositiveButton("APAGAR", (dialog, which) -> {

            relacao.setText("");
            patrimonio.setText("");
            numSerie.setText("");

            contadorLinhas();

            num_sequencia = "";
        });

        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void forcarSalvar() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Salvar");
        builder.setMessage("Salvar todas as alterações na relação atual?");

        builder.setPositiveButton("OK", (dialog, which) -> {

            manterNaMemoria();

            Toast.makeText(getBaseContext(), "Salvo", Toast.LENGTH_SHORT).show();

        });

        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void procurar() {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Procurar");

        final AutoCompleteTextView input = new AutoCompleteTextView(this);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        historicoBens = tinyDB.getListString("historicoBens");

        ArrayAdapter<String> adapterBens = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historicoBens);
        input.setAdapter(adapterBens);

        builder.setPositiveButton("PROCURAR", (dialog, which) -> {
            String text = input.getText().toString();

            if (!text.isEmpty()) {

                utils.procurarTexto(relacao, text.toUpperCase(), relacaoTV);

                if (!historicoBens.contains(input.getText().toString())) {

                    tinyDB.remove("historicoBens");
                    historicoBens.add(input.getText().toString());
                    tinyDB.putListString("historicoBens", historicoBens);
                }
            }
        });

        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    public void switchMode(String i, String method) {

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

                if (method.equals("scan")) {

                    autoScan(newIntentResult);
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

                if (method.equals("scan")) {

                    autoScan(newIntentResult);
                }

                break;

            case "descricao":

                if (relacao.getText().toString().contains(i)) {

                    Toast.makeText(getBaseContext(), i + " Já foi escaneado", Toast.LENGTH_LONG).show();

                    if (relacao.getText().toString().contains(i)) {

                        utils.autoScroll(relacaoScrollView, relacao, i);
                    }

                } else {

                    AlertDialog.Builder builder = new AlertDialog.Builder(this);

                    builder.setCancelable(false);
                    builder.setTitle("Descrição");
                    builder.setMessage("Patrimônio: " + i + "\n\nInsira a descrição do bem abaixo:");

                    final AutoCompleteTextView input = new AutoCompleteTextView(this);
                    input.setInputType(InputType.TYPE_CLASS_TEXT);
                    builder.setView(input);

                    historicoBens = tinyDB.getListString("historicoBens");

                    ArrayAdapter<String> adapterBens = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, historicoBens);
                    input.setAdapter(adapterBens);

                    builder.setPositiveButton("OK", (dialog, which) -> {
                        String text = input.getText().toString();

                        ultimoRelacao();

                        relacao.append(text.toUpperCase() + " : " + i + "\n");

                        manterNaMemoria();

                        contadorLinhas();

                        atualRelacao();

                        ultimoItem = true;

                        voltarItem = true;

                        if (!historicoBens.contains(input.getText().toString())) {

                            tinyDB.remove("historicoBens");
                            historicoBens.add(input.getText().toString());
                            tinyDB.putListString("historicoBens", historicoBens);
                        }
                    });

                    builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

                    AlertDialog dialog = builder.create();
                    dialog.show();
                }

                break;
        }
    }

    public void carregarPastebin(String s) {

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(false);
        builder.setTitle("Carregar nova relação");
        builder.setMessage("Carregar uma nova relação do pastebin?");

        builder.setPositiveButton("SIM", (dialog, which) -> pastebin.pastebin(MainActivity.this, s, relacao, relacaoTV));
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

    }
}