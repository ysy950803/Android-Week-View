package com.alamkanak.weekview.sample;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;


/**
 * The launcher activity of the sample app. It contains the links to visit all the example screens.
 * Created by Raquib-ul-Alam Kanak on 7/21/2014.
 * Website: http://alamkanak.github.io
 */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        findViewById(R.id.buttonBasic).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, BasicActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.buttonAsynchronous).setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, AsynchronousActivity.class);
            startActivity(intent);
        });

        findViewById(R.id.btn_calendar).setOnClickListener(v -> {
            startActivity(new Intent(MainActivity.this, CalendarActivity.class));
        });
    }
}
