package com.dip1968.androidapp;

import android.app.Activity;
import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.widget.TextView;

public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        TextView text = new TextView(this);
        text.setText("Androidapp\\n\\nBuild successful!");
        text.setTextSize(24);
        text.setTextColor(Color.BLACK);
        text.setGravity(Gravity.CENTER);
        text.setPadding(32, 32, 32, 32);
        setContentView(text);
    }
}
