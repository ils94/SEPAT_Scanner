package com.droidev.sepatscanner;

import android.content.Context;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;

public class CaixaDialogo {

    ArrayList<String> historicoBens = new ArrayList<>();

    interface onButtonPressed {

        void buttonPressed(String i);
    }

    public void simples(Context context, String title, String message, String positive, String negative, onButtonPressed onButtonPressed) {

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, null)
                .setNegativeButton(negative, null)
                .show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        positiveButton.setOnClickListener(v -> {

            onButtonPressed.buttonPressed("true");

            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> {

            onButtonPressed.buttonPressed("false");

            dialog.dismiss();
        });
    }

    public void simplesTresBotoes(Context context, String title, String message, String positive, String negative, String neutral, onButtonPressed onButtonPressed) {

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, null)
                .setNegativeButton(negative, null)
                .setNeutralButton(neutral, null)
                .show();

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        positiveButton.setOnClickListener(v -> {

            onButtonPressed.buttonPressed("true");

            dialog.dismiss();
        });

        negativeButton.setOnClickListener(v -> {

            onButtonPressed.buttonPressed("false");

            dialog.dismiss();
        });

        neutralButton.setOnClickListener(v -> {

            onButtonPressed.buttonPressed("neutral");

            dialog.dismiss();
        });
    }

    public void simplesComView(Context context, String title, String message, String hint, String positive, String negative, int inputType, Boolean adapter, Boolean length, onButtonPressed onButtonPressed) {

        AutoCompleteTextView autoCompleteTextView = new AutoCompleteTextView(context);
        autoCompleteTextView.setHint(hint);
        autoCompleteTextView.setInputType(inputType);

        LinearLayout lay = new LinearLayout(context);
        lay.setOrientation(LinearLayout.VERTICAL);
        lay.addView(autoCompleteTextView);

        AlertDialog dialog = new AlertDialog.Builder(context)
                .setCancelable(false)
                .setTitle(title)
                .setMessage(message)
                .setPositiveButton(positive, null)
                .setNegativeButton(negative, null)
                .setView(lay)
                .show();

        TinyDB tinyDB = new TinyDB(context);

        historicoBens = tinyDB.getListString("historicoBens");

        ArrayAdapter<String> adapterBens = new ArrayAdapter<>(context, android.R.layout.simple_list_item_1, historicoBens);
        autoCompleteTextView.setAdapter(adapterBens);

        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);

        positiveButton.setOnClickListener(v -> {

            String string = autoCompleteTextView.getText().toString();

            if (!adapter) {

                string = StringUtils.leftPad(string, 6, '0');
            }

            if (!string.equals("")) {

                onButtonPressed.buttonPressed(string);

                if (adapter) {

                    if (!historicoBens.contains(autoCompleteTextView.getText().toString())) {

                        tinyDB.remove("historicoBens");
                        historicoBens.add(autoCompleteTextView.getText().toString());
                        tinyDB.putListString("historicoBens", historicoBens);
                    }
                }

                dialog.dismiss();

            } else {

                Toast.makeText(context, "Erro, campo vazio", Toast.LENGTH_SHORT).show();
            }
        });
    }

}
