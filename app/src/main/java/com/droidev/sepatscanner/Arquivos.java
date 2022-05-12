package com.droidev.sepatscanner;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
}
