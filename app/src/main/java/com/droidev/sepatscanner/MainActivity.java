package com.droidev.sepatscanner;

import androidx.appcompat.app.AppCompatActivity;

import android.annotation.SuppressLint;
import android.app.Activity;
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
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class MainActivity extends AppCompatActivity {

    Button scan, ok;

    EditText relacao, patrimonio, numSerie;

    TextView relacaoTV;

    String modo = "padrao", ultimo, atual, newIntentResult;

    Boolean ultimoItem = false, voltarItem = false;

    Utils utils;
    CaixaDialogo caixaDialogo;
    Arquivos arquivos;
    Pastebin pastebin;

    TinyDB tinyDB;

    private static final int LER_ARQUIVO = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        utils = new Utils();
        caixaDialogo = new CaixaDialogo();
        arquivos = new Arquivos();
        pastebin = new Pastebin();

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

        patrimonio.setEnabled(false);
        numSerie.setEnabled(false);
        ok.setEnabled(false);

        scan.setOnClickListener(v -> utils.scanner(MainActivity.this));

        ok.setOnClickListener(v -> {

            if (patrimonio.getText().toString().equals("") || numSerie.getText().toString().equals("")) {

                Toast.makeText(MainActivity.this, "Os campos Patrim??nio e N??mero de s??rie n??o podem est?? vazios", Toast.LENGTH_SHORT).show();
            } else if (relacao.getText().toString().contains(patrimonio.getText())) {

                Toast.makeText(getBaseContext(), patrimonio.getText() + " J?? foi escaneado", Toast.LENGTH_LONG).show();
            } else {

                ultimoRelacao();

                relacao.append(patrimonio.getText().toString() + ", " + numSerie.getText().toString() + "\n");

                patrimonio.setText("");

                numSerie.setText("");

                manterNaMemoria();

                contadorLinhas();

                atualRelacao();

                ultimoItem = true;

                voltarItem = true;
            }
        });

        String content = utils.recuperarDaMemoria(MainActivity.this, "bens.txt");

        relacao.setText(content);
        relacao.setFocusableInTouchMode(false);
        relacao.clearFocus();
        relacao.setCursorVisible(false);

        if (!tinyDB.getString("Fonte").isEmpty()) {

            relacao.setTextSize(TypedValue.COMPLEX_UNIT_SP, Integer.parseInt(tinyDB.getString("Fonte")));
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        esperar();
    }

    @Override
    public void onBackPressed() {

        caixaDialogo.simples(MainActivity.this, "Sair", "Desejar sair da aplica????o?", "Sim", "N??o", i -> {

            if (i.equals("true")) {

                manterNaMemoria();

                MainActivity.this.finish();
            }
        });
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

                caixaDialogo.simplesComView(MainActivity.this, "Procurar", "Digite uma palavra abaixo para real??ar.", "Exemplo: estabilizador", "Procurar", "Cancelar", InputType.TYPE_CLASS_TEXT, true, false, i -> utils.realcarTexto(relacao, i.toUpperCase(), relacaoTV));

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

                caixaDialogo.simplesComView(MainActivity.this, "Enviar relat??rio", "Nome do arquivo:", "Exemplo: monitores", "Enviar", "Cancelar", InputType.TYPE_CLASS_TEXT, false, false, i -> arquivos.enviarArquivo(MainActivity.this, i, relacao.getText().toString()));

                return true;

            case R.id.abrir:

                caixaDialogo.simples(MainActivity.this, "Abrir nova rela????o", "Abrir uma nova rela????o ir?? apagar tudo da rela????o atual no App. Deseja continuar?", "Sim", "N??o", i -> {
                    if (i.equals("true")) {

                        try {
                            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                            intent.setType("text/csv|text/comma-separated-values|application/csv");
                            String[] mimetypes = {"text/csv", "text/comma-separated-values", "application/csv", "text/*"};
                            intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                            startActivityForResult(Intent.createChooser(intent, "Abrir rela????o"), LER_ARQUIVO);
                        } catch (Exception e) {

                            Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

                return true;

            case R.id.fonte:

                float scaledDensity = MainActivity.this.getResources().getDisplayMetrics().scaledDensity;
                float sp = relacao.getTextSize() / scaledDensity;

                caixaDialogo.simplesComView(MainActivity.this, "Alterar tamanho da fonte", "Insira o tamanho abaixo:", "O tamanho atual ??: " + sp, "Ok", "Cancelar", (InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL), false, false, i -> {

                    try {

                        relacao.setTextSize(TypedValue.COMPLEX_UNIT_SP, Float.parseFloat(i));

                        TinyDB tinyDB = new TinyDB(MainActivity.this);

                        tinyDB.remove("Fonte");
                        tinyDB.putString("Fonte", i);

                    } catch (Exception e) {

                        Toast.makeText(MainActivity.this, e.toString(), Toast.LENGTH_SHORT).show();
                    }
                });

                return true;

            case R.id.contarLinhas:

                contadorLinhas();

                return true;

            case R.id.forcarSalvar:

                caixaDialogo.simples(MainActivity.this, "Salvar", "Salvar todas as altera????es na rela????o atual?", "Sim", "N??o", i -> {
                    if (i.equals("true")) {

                        manterNaMemoria();

                        Toast.makeText(getBaseContext(), "Salvo", Toast.LENGTH_SHORT).show();
                    }
                });

                return true;

            case R.id.padrao:

                modo = "padrao";

                patrimonio.setEnabled(false);
                numSerie.setEnabled(false);
                ok.setEnabled(false);

                Toast.makeText(this, "Modo ''Padr??o'' ativado", Toast.LENGTH_SHORT).show();

                return true;

            case R.id.descricao:

                modo = "descricao";

                patrimonio.setEnabled(false);
                numSerie.setEnabled(false);
                ok.setEnabled(false);

                Toast.makeText(this, "Modo ''Patrim??nio com descri????o'' ativado", Toast.LENGTH_SHORT).show();

                return true;

            case R.id.checking:

                modo = "checking";

                patrimonio.setEnabled(false);
                numSerie.setEnabled(false);
                ok.setEnabled(false);

                Toast.makeText(this, "Modo ''Checking de rela????o'' ativado", Toast.LENGTH_SHORT).show();

                return true;

            case R.id.ns:

                modo = "ns";

                patrimonio.setEnabled(true);
                numSerie.setEnabled(true);
                ok.setEnabled(true);

                Toast.makeText(this, "Modo ''N??mero de s??rie'' ativado", Toast.LENGTH_SHORT).show();

                return true;

            case R.id.apagar:

                caixaDialogo.simples(MainActivity.this, "Apagar", "Apagar todos os campos?", "Sim", "Cancelar", i -> {

                    if (i.equals("true")) {

                        relacao.setText("");
                        patrimonio.setText("");
                        numSerie.setText("");

                        contadorLinhas();
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

                pastebin.checarQrCode(MainActivity.this, relacao.getText().toString());

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

                switch (modo) {

                    case "padrao":

                        newIntentResult = intentResult.getContents();

                        if (newIntentResult.length() == 10 && newIntentResult.startsWith("45")) {

                            newIntentResult = utils.remover45(newIntentResult);
                        }

                        if (relacao.getText().toString().contains(newIntentResult)) {

                            Toast.makeText(getBaseContext(), newIntentResult + " J?? foi escaneado", Toast.LENGTH_LONG).show();
                        } else if (newIntentResult.contains("pastebin")) {

                            caixaDialogo.simples(MainActivity.this, "Carregar nova rela????o", "Carregar uma nova rela????o do pastebin?", "Sim", "Cancelar", i -> {

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

                        break;

                    case "descricao":

                        if (relacao.getText().toString().contains(intentResult.getContents())) {

                            Toast.makeText(getBaseContext(), intentResult.getContents() + " J?? foi escaneado", Toast.LENGTH_LONG).show();

                        } else {

                            caixaDialogo.simplesComView(MainActivity.this, "Descri????o", "Patrim??nio: " + intentResult.getContents() + "\n\nInsira a descri????o do bem abaixo:", "Exemplo: mesa reta", "Ok", "Cancelar", InputType.TYPE_CLASS_TEXT, true, false, i -> {

                                ultimoRelacao();

                                relacao.append(i.toUpperCase() + " : " + intentResult.getContents() + "\n");

                                manterNaMemoria();

                                contadorLinhas();

                                atualRelacao();

                                ultimoItem = true;

                                voltarItem = true;
                            });
                        }

                        break;

                    case "ns":

                        if (relacao.getText().toString().contains(intentResult.getContents())) {

                            Toast.makeText(getBaseContext(), intentResult.getContents() + " J?? foi escaneado", Toast.LENGTH_LONG).show();
                        } else {

                            if (patrimonio.getText().toString().equals("")) {

                                patrimonio.setText(intentResult.getContents());
                            } else {

                                if (numSerie.getText().toString().equals("")) {

                                    numSerie.setText(intentResult.getContents());
                                } else {

                                    caixaDialogo.simples(MainActivity.this, "Aten????o", "Substituir o n??mero de s??rie atual por ''" + intentResult.getContents() + "'' ?", "Sim", "Cancelar", i -> {

                                        if (i.equals("true")) {

                                            numSerie.setText(intentResult.getContents());
                                        }
                                    });
                                }
                            }
                        }
                        break;

                    case "checking":

                        newIntentResult = intentResult.getContents();

                        if (newIntentResult.length() == 10 && newIntentResult.startsWith("45")) {

                            newIntentResult = utils.remover45(newIntentResult);
                        }

                        if (relacao.getText().toString().contains(newIntentResult + " : [OK]")) {

                            Toast.makeText(getBaseContext(), newIntentResult + " consta na lista, e j?? foi escaneado", Toast.LENGTH_LONG).show();

                        } else if (relacao.getText().toString().contains(newIntentResult)) {

                            ultimoRelacao();

                            String relacao_check = relacao.getText().toString().replace(newIntentResult, newIntentResult + " : [OK]");

                            relacao.setText(relacao_check);

                            Toast.makeText(getBaseContext(), newIntentResult + " consta na rela????o", Toast.LENGTH_LONG).show();

                            manterNaMemoria();

                            atualRelacao();

                            ultimoItem = true;

                            voltarItem = true;

                        } else {

                            Toast.makeText(getBaseContext(), newIntentResult + " n??o consta na rela????o", Toast.LENGTH_LONG).show();
                        }
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

                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    BufferedReader r = new BufferedReader(new InputStreamReader(inputStream));

                    relacao.setText("");

                    String mLine;
                    while ((mLine = r.readLine()) != null) {
                        relacao.append(mLine.toUpperCase().replace(",", " : ").replace("  ", " ") + "\n");
                    }
                } catch (IOException e) {
                    Toast.makeText(getBaseContext(), e.toString(), Toast.LENGTH_SHORT).show();
                }
            }

            manterNaMemoria();
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

        caixaDialogo.simplesComView(MainActivity.this, "Modo manual", "Insira o n??mero patrimonial abaixo:", "Exemplo: 012345", "Ok", "Cancelar", InputType.TYPE_CLASS_NUMBER, false, true,
                i -> {

                    switch (modo) {

                        case "checking":

                            if (relacao.getText().toString().contains(i + " : [OK]")) {

                                Toast.makeText(getBaseContext(), i + " consta na lista, e j?? foi escaneado", Toast.LENGTH_LONG).show();

                            } else if (relacao.getText().toString().contains(i)) {

                                ultimoRelacao();

                                String relacao_check = relacao.getText().toString().replace(i, i + " : [OK]");

                                relacao.setText(relacao_check);

                                Toast.makeText(getBaseContext(), i + " consta na rela????o", Toast.LENGTH_LONG).show();

                                manterNaMemoria();

                                atualRelacao();

                                ultimoItem = true;

                                voltarItem = true;

                            } else {

                                Toast.makeText(getBaseContext(), i + " n??o consta na rela????o", Toast.LENGTH_LONG).show();
                            }
                            break;

                        case "padrao":

                            if (relacao.getText().toString().contains(i)) {

                                Toast.makeText(getBaseContext(), i + " J?? foi escaneado", Toast.LENGTH_LONG).show();
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

                                Toast.makeText(getBaseContext(), i + " J?? foi escaneado", Toast.LENGTH_LONG).show();

                            } else {

                                caixaDialogo.simplesComView(MainActivity.this, "Descri????o", "Patrim??nio: " + i + "\n\nInsira a descri????o do bem abaixo:", "Exemplo: mesa reta", "Ok", "Cancelar", InputType.TYPE_CLASS_TEXT, true, false, j -> {

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

        utils.manterNaMemoria(MainActivity.this, relacao.getText().toString(), "bens.txt");
    }
}