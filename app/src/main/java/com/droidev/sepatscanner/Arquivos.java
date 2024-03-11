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
import java.io.FileOutputStream;

public class Arquivos {

    public void enviarArquivo(Context context, String file, String content) {

        try {

            content = content.replace(" : ", ",");

            FileOutputStream out = context.openFileOutput(file + ".csv", Context.MODE_PRIVATE);
            out.write((content.getBytes()));
            out.close();

            File fileLocation = new File(context.getFilesDir(), file + ".csv");
            Uri path = FileProvider.getUriForFile(context, "com.droidev.sepatscan.fileprovider", fileLocation);
            Intent fileIntent = new Intent(Intent.ACTION_SEND);
            fileIntent.setType("text/csv");
            fileIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            fileIntent.putExtra(Intent.EXTRA_STREAM, path);
            context.startActivity(Intent.createChooser(fileIntent, "Enviar"));

        } catch (Exception e) {

            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    public void salvarArquivo(Activity activity, int arquivo) {

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        builder.setTitle("Nome do Arquivo");
        builder.setCancelable(false);

        final EditText input = new EditText(activity);
        input.setInputType(InputType.TYPE_CLASS_TEXT);
        builder.setView(input);

        builder.setPositiveButton("OK", (dialog, which) -> {
            String text = input.getText().toString();

            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);

            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("text/plain|text/csv|text/comma-separated-values");
            intent.putExtra(Intent.EXTRA_TITLE, text + ".csv");

            activity.startActivityForResult(intent, arquivo);
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> dialog.cancel());

        AlertDialog dialog = builder.create();
        dialog.show();

    }
}
