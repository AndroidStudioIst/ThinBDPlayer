package com.walixiwa.bdplayer;


import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.EditText;

import com.walixiwa.thin.bdplayer.PlayerActivity;

public class MainActivity extends AppCompatActivity {
    private EditText et_title;
    private EditText et_url;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        et_title = findViewById(com.walixiwa.thin.bdplayer.R.id.title);
        et_url = findViewById(R.id.url);
        findViewById(R.id.play).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                String title = et_title.getText().toString().trim();
                String url = et_url.getText().toString().trim();
                startPlay(MainActivity.this, title, url);
            }
        });
    }
    public static void startPlay(Context context, String title, String url) {
        Intent intent = new Intent(context, PlayerActivity.class);
        intent.putExtra("title", title);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }
}
