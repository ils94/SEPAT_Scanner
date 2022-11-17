package com.droidev.sepatscanner;

import android.content.Context;
import android.widget.Toast;

import org.json.JSONObject;


public class JSON {

    public JSONObject criarJson(Context context, String relacao) {

        JSONObject jsonObject = new JSONObject();

        try {

            jsonObject.put("relacao", relacao);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(context, e.toString(), Toast.LENGTH_SHORT).show();
        }

        return jsonObject;
    }
}
