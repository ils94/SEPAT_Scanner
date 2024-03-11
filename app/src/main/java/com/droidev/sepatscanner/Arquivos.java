package com.droidev.sepatscanner;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.text.InputType;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public class Arquivos {

    public void enviarArquivo(Activity activity, String content) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Enviar Relação");

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        String conteudo = content.replace(" : ", ",");

        builder.setPositiveButton("ENVIAR", (dialog, which) -> {
            String text = input.getText().toString();

            try {
                FileOutputStream out = activity.openFileOutput(text + ".csv", Context.MODE_PRIVATE);

                out.write((conteudo.getBytes()));
                out.close();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            File fileLocation = new File(activity.getFilesDir(), text + ".csv");
            Uri path = FileProvider.getUriForFile(activity, "com.droidev.sepatscan.fileprovider", fileLocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            activity.startActivity(Intent.createChooser(fileIntent, "Enviar"));
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void salvarArquivo(Activity activity, int arquivo) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Nome do Arquivo");
        builder.setCancelable(false);

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("SALVAR", (dialog, which) -> {
            String text = input.getText().toString();

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain|text/csv|text/comma-separated-values");
            intent.putExtra(Intent.EXTRA_TITLE, text + ".csv");

            activity.startActivityForResult(intent, arquivo);
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

    }

    public void abrirArquivo(Activity activity, int arquivo) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setCancelable(false);
        builder.setTitle("Abrir nova relação");
        builder.setMessage("Abrir uma nova relação irá apagar tudo da relação atual no App. Deseja continuar?");

        builder.setPositiveButton("ABRIR", (dialog, which) -> {

            try {
                Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                intent.setType("text/csv|text/comma-separated-values|application/csv");
                String[] mimetypes = {"text/csv", "text/comma-separated-values", "application/csv", "text/*"};
                intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
                activity.startActivityForResult(Intent.createChooser(intent, "Abrir relação"), arquivo);
            } catch (Exception e) {

                Toast.makeText(activity, e.toString(), Toast.LENGTH_SHORT).show();
            }
        });
        builder.setNegativeButton("CANCELAR", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

    }
}
