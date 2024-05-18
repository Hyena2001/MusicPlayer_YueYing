package com.example.yueying2;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.drawable.AnimationDrawable;
import android.os.Bundle;
import android.widget.ImageView;

import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private AnimationDrawable animationDrawable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        imageView = findViewById(R.id.iv_anim);
        animationDrawable = (AnimationDrawable) imageView.getBackground();
        animationDrawable.start();


        Intent intent = new Intent(this, MainMusicActivity.class);
        Timer timer=new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                animationDrawable.stop();
                startActivity(intent);
                //销毁此活动
            }
        },3000);//表示延时3秒进行跳转

    }
}