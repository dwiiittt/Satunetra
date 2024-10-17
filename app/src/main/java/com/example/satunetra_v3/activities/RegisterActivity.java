package com.example.satunetra_v3.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;


import com.example.satunetra_v3.R;
import com.example.satunetra_v3.helper.BotHelper;
import com.example.satunetra_v3.helper.RoomHelper;
import com.example.satunetra_v3.helper.SpeechHelper;
import com.example.satunetra_v3.helper.VoiceHelper;

import java.util.ArrayList;
import java.util.Random;

public class RegisterActivity extends AppCompatActivity implements View.OnTouchListener
{
    //deklarasi
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;
    private TextToSpeech tts;
    private TextView tvTitle, tvName, tvBtn;
    private GestureDetector mGestureDetector;
    private LinearLayout llSpeech, llForm;
    private BotHelper botHelper;

    //var
    private int deep;
    private String respond;
    private boolean allowSpeech;
    private String name;
    private boolean firstRepeat;

    //const
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        //mengatur layout
        llSpeech = findViewById(R.id.ll_speech);
        llForm = findViewById(R.id.ll_form);
        tvTitle = findViewById(R.id.tv_title);
        tvName = findViewById(R.id.t_name);
        tvBtn = findViewById(R.id.btn_daftar);

        mGestureDetector = new GestureDetector(this, new GestureListener());

        deep = 0;
        allowSpeech = false;

        name = "";
        firstRepeat = true;


        respond = "";


        //init bot
        botHelper = new BotHelper(this);

        llSpeech.setEnabled(false);
        llSpeech.setOnTouchListener(this);




        //memanggil konfigurasi SR dan TTS
        configureSpeechRecognition();
        configureTextToSpeech();

        //mengecek izin record audio
        checkPermissionSpeech();

    }
    private void checkPermissionSpeech() {
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[]{Manifest.permission.RECORD_AUDIO}, 1);
        }else{
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    // speak out the bot reply
                    try{
                        sendMessage("REGISTER");
                        startSpeak(respond);
                    }catch (Exception e){
                        checkPermissionSpeech();
                    }

                }
            }, 10);
        }
    }

    private void sendMessage(String message) {
        respond = botHelper.sendChatMessage(message);
    }

    private void startSpeak(String string) {
        //mengubah teks jadi suara
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(string, TextToSpeech.QUEUE_ADD, null, "text");
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        configureSpeechRecognition();
    }

    @Override
    public void onDestroy() {
        // Mematikan TTS dan SP
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
        if (speechRecognizer != null){
            speechRecognizer.stopListening();
            speechRecognizer.destroy();
        }
        super.onDestroy();
    }

    private void configureTextToSpeech() {
        //konfigurasi tts
        VoiceHelper helper = new VoiceHelper(this);
        tts = helper.getTts();
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                allowSpeech = false;
            }

            @Override
            public void onDone(String utteranceId) {
                new Thread()
                {
                    public void run()
                    {
                        //ketika tts selesai maka SR akan otomtasi dimulai
                        RegisterActivity.this.runOnUiThread(new Runnable()
                        {

                            public void run()
                            {
                                llSpeech.setEnabled(true);
                                if(deep!=1){
                                    allowSpeech = true;
                                }
                            }
                        });
                    }
                }.start();

            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }

    private void configureSpeechRecognition() {
        //instanca SR
        SpeechHelper helper = new SpeechHelper(this);
        speechRecognizer = helper.getSpeechRecognizer();
        speechIntent = helper.getSpeechIntent();
        //mengatur Listener SR
        speechRecognizer.setRecognitionListener(new RecognitionListener() {
            @Override
            public void onReadyForSpeech(Bundle params) {

            }

            @Override
            public void onBeginningOfSpeech() {

            }

            @Override
            public void onRmsChanged(float rmsdB) {

            }

            @Override
            public void onBufferReceived(byte[] buffer) {

            }

            @Override
            public void onEndOfSpeech() {
                speechRecognizer.stopListening();
            }

            @Override
            public void onError(int error) {

            }

            @Override
            public void onResults(Bundle results) {
                //mengirim result
                ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
                String string = "";
                if(matches!=null){
                    string = matches.get(0);
                    endOfResult(string);
                }
            }

            @Override
            public void onPartialResults(Bundle partialResults) {

            }

            @Override
            public void onEvent(int eventType, Bundle params) {

            }
        });
    }

    private void endOfResult(String string) {
        if(deep==0){
            String[] sub = string.toLowerCase().split(" ");
            for(String subString : sub){
                System.out.println(subString);
                //Jika string yang diucapkan adalah lanjutkan
                if(subString.equals("lanjutkan")){
                    llForm.setVisibility(View.VISIBLE);
                    tvTitle.setText(getResources().getString(R.string.daftar));
                    tvBtn.setText(getResources().getString(R.string.daftar));
                    sendMessage("LANJUTKAN");
                    startSpeak(respond);
                    deep = 1;
                    break;
                }
                //jika bukan otomatis mulai ulang SR
                else{
                    speechRecognizer.startListening(speechIntent);
                }
            }
        }
        else if(deep==2){
            sendMessage(string);
            name = respond.toLowerCase();
            tvName.setText(name);
            tvName.setTextColor(getResources().getColor(R.color.mainBlue));
            String repeat = "Saya ulang, nama anda adalah " + name;
            if(firstRepeat){
                repeat += ". Jika sudah benar usap layar dari kiri kekanan, dan jika masih salah anda dapat mengulanginya kembali";
                firstRepeat=false;
            }
            startSpeak(repeat);
        }
        else if(deep==3){
            String[] sub = string.toLowerCase().split(" ");
            for(String subString : sub){
                System.out.println(subString);
                //Jika string yang diucapkan adalah iya
                if(subString.equals("iya")){
                    sendMessage("YA");
                    startSpeak(respond);
                    saveToDB();
                    deep = 4;
                    break;
                }
                //jika bukan otomatis mulai ulang SR
                else{
                    speechRecognizer.startListening(speechIntent);
                }
            }
        }
    }

    private void saveToDB() {
        RoomHelper helper = new RoomHelper(RegisterActivity.this);
        Random random = new Random();
        int id = random.nextInt(10 - 1 + 1) + 1;
        helper.createUser(id,name);
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        //set gesture detector for a widget
        if(v.getId() == R.id.ll_speech){
            mGestureDetector.onTouchEvent(event);
            return true;
        }

        return true;
    }



    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if(allowSpeech){
                System.out.println("DITEKAAN");
                speechRecognizer.startListening(speechIntent);
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            //on swipe
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        //swipe from left to right
                        if (diffX > 0) {
                            if(deep==1){
                                sendMessage("NEXT");
                                startSpeak(respond);
                                deep++;
                            }
                            else if(deep==2 && !name.isEmpty()){
                                sendMessage("NEXT");
                                startSpeak(respond);
                                deep++;
                            }
                            else  if(deep ==4){
                                Intent registerIntent = new Intent(RegisterActivity.this, ChatActivity.class);
                                startActivity(registerIntent);
                                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                                finish();
                            }

                        }
                        result = true;
                    }
                }
            } catch (Exception e){
                e.printStackTrace();
            }
            return result;
        }
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull @org.jetbrains.annotations.NotNull String[] permissions, @NonNull @org.jetbrains.annotations.NotNull int[] grantResults) {
        if(requestCode==1){
            if(grantResults[0]==PackageManager.PERMISSION_GRANTED){
                Toast.makeText(this, "Permission Granted", Toast.LENGTH_SHORT).show();
                checkPermissionSpeech();
            }else{
                Toast.makeText(this, "Permission Denied", Toast.LENGTH_SHORT).show();
                finish();
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

}



