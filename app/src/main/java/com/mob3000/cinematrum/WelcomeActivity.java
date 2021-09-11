package com.mob3000.cinematrum;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.os.Bundle;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

public class WelcomeActivity extends AppCompatActivity {

    //custom animations for button click because it isn't from MaterialDesign (it's from android.widget.button)
    Animation scale_up_animation,scale_down_animation;
    Button btnSignIn,btnSignUp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);


        //removes the UI actionBar at the top of the screen
        getSupportActionBar().hide();

        //loading animations
        LoadAnimations();
        //loading all activity views
        LoadViews();
        //setting up the touch listeners
        SetOnTouchListeners();
    }



    //loading all the views from activity
    private void LoadViews() {
        btnSignIn = findViewById(R.id.btnSignIn);
        btnSignUp = findViewById(R.id.btnSignUp);
    }


    //setting touch listeners on both buttons to activate the touch animations and navigating to corresponding activity
    private void SetOnTouchListeners() {
        btnSignIn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction()==MotionEvent.ACTION_DOWN)
                    btnSignIn.startAnimation(scale_up_animation);
                else if(motionEvent.getAction() == MotionEvent.ACTION_UP)
                    btnSignIn.startAnimation(scale_down_animation);


                Intent intent = new Intent(WelcomeActivity.this,SignInActivity.class);
                startActivity(intent);
                return true;
            }
        });

        btnSignUp.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                if(motionEvent.getAction() == MotionEvent.ACTION_DOWN)
                    btnSignUp.setAnimation(scale_up_animation);
                else if(motionEvent.getAction()== MotionEvent.ACTION_UP)
                    btnSignUp.setAnimation(scale_down_animation);

                Intent intent = new Intent(WelcomeActivity.this,SignUpActivity.class);
                startActivity(intent);
                return false;
            }
        });
    }


    //storing the animations in two Animation variables
    private void LoadAnimations() {
        scale_up_animation = AnimationUtils.loadAnimation(this,R.anim.scale_up);
        scale_down_animation = AnimationUtils.loadAnimation(this,R.anim.scale_down);
    }


}