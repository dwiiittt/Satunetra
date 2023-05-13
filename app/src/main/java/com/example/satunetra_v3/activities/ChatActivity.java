package com.example.satunetra_v3.activities;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.speech.RecognitionListener;
import android.speech.SpeechRecognizer;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.satunetra_v3.R;
import com.example.satunetra_v3.adapter.ChatAdapter;
import com.example.satunetra_v3.data.TagMaps;
import com.example.satunetra_v3.helper.BotHelper;
import com.example.satunetra_v3.helper.RoomHelper;
import com.example.satunetra_v3.helper.SpeechHelper;
import com.example.satunetra_v3.helper.VoiceHelper;
import com.example.satunetra_v3.local.table.UserEntity;
import com.example.satunetra_v3.model.Message;
import com.example.satunetra_v3.model.Feel;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.database.annotations.NotNull;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class ChatActivity extends AppCompatActivity implements View.OnTouchListener {
    private View btnStartChat;
    private ImageView ivNotSpeech, ivMic;
    private TextView tvTimer;
    private GestureDetector gestureDetector;

    //for local db
    private RoomHelper helper;

    //for chat
    private RecyclerView recyclerView;
    private ChatAdapter chatAdapter;


    //for tts and SR
    private TextToSpeech tts;
    private SpeechRecognizer speechRecognizer;
    private Intent speechIntent;

    //for timer
    private CountDownTimer cTimer;

    //list and ,ap
    private ArrayList<Message> messageArrayList;
    private Map<String, Feel> tagMap;
    private List<String> test;

    //bot helper
    private BotHelper botHelper;

    //attribute
    //message from user and bot
    private String userMessage;
    //name of user
    private String name;
    private boolean allowShow;
    private boolean isTimer;
    private boolean allowSpeak;

    private String respond;
    private boolean initialRequest;
    //deep of chat
    private int deep;


    public static final String BroadcastStringForAction = "checkinternet";
    private IntentFilter mIntentFilter;
    private View vStatus;
    private TextView tvStatus;

    private boolean isConnected;
    //is user first init or not
    private boolean firstInit;
    private String instructionKey;
    private String feelKey;
    private boolean allowClose;
    private boolean allowPlay;
    private boolean afterPlay;

    //const
    private static final int SWIPE_THRESHOLD = 100;
    private static final int SWIPE_VELOCITY_THRESHOLD = 100;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        vStatus = findViewById(R.id.v_online);
        tvStatus = findViewById(R.id.tv_online);
        btnStartChat = findViewById(R.id.btn_gestur_chat);
        ivMic = findViewById(R.id.iv_mic_chat);
        recyclerView = findViewById(R.id.recycler_view);
        tvTimer = findViewById(R.id.tv_timer);

        deep = 0;
        gestureDetector = new GestureDetector(this, new GestureListener());
        firstInit = false;
        afterPlay = false;

        botHelper = new BotHelper(this);
        mIntentFilter = new IntentFilter();
        mIntentFilter.addAction(BroadcastStringForAction);
        isTimer = false;
        isConnected = false;
        allowClose = false;
        allowPlay = false;
        allowSpeak = false;
        instructionKey = "";
        feelKey = "";


        btnStartChat.setOnTouchListener(this);
        //instance
        helper = new RoomHelper(this);
        UserEntity userEntity = helper.readUser();


        //setting message
        messageArrayList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageArrayList);
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true);
        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(chatAdapter);
        name = userEntity.getName();
        initialRequest = true;

        configureSpeechRecognition();
        configureTTS();

        if (!userEntity.getFirst()) {
            helper.firstTake(userEntity.getId());
            firstInit = true;
        }
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                readDatabase();
            }
        });

        try{
            new AsyncAction().execute();
        }catch (Exception e) {
            e.printStackTrace();
        }

    }

    private void readDatabase() {
        TagMaps tags = new TagMaps();
        tagMap = tags.readTags();
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference reference = database.getReference("links");

        reference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull @NotNull DataSnapshot snapshot) {
                String[] allFeel = {"a01", "a02", "a03"};
                for (int i = 0; i < snapshot.getChildrenCount(); i++) {
                    DataSnapshot s = snapshot.child(allFeel[i]);
                    for (int j = 0; j < s.getChildrenCount(); j++) {
                        List<String> temp = new ArrayList<>();
                        String instruction = tagMap.get(allFeel[i]).getInstructionList()[j];
                        DataSnapshot snap = s.child(instruction);
                        for (int k = 0; k < snap.getChildrenCount(); k++) {
                            String tempLink = snap.child(String.valueOf(k)).getValue(String.class);
                            temp.add(tempLink);
                            System.out.println(tempLink);
                        }
                        tagMap.get(allFeel[i]).getChild().get(instruction).addURL(temp);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull @NotNull DatabaseError error) {

            }
        });
    }


    //listener for SR
    private class MyRecognitionListener implements RecognitionListener {
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
            refreshSpeechUI(false);
            speechRecognizer.stopListening();
        }

        @Override
        public void onError(int error) {
            refreshSpeechUI(false);
        }

        @Override
        public void onResults(Bundle results) {
            refreshSpeechUI(false);
            ArrayList<String> matches = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);
            String string = "...";
            if (matches != null) {
                string = matches.get(0);
                userMessage = string;
                sendMessage();
            }
        }

        @Override
        public void onPartialResults(Bundle partialResults) {

        }

        @Override
        public void onEvent(int eventType, Bundle params) {

        }
    }


    //configure SR
    private void configureSpeechRecognition() {
        SpeechHelper helper = new SpeechHelper(this);
        speechRecognizer = helper.getSpeechRecognizer();
        speechIntent = helper.getSpeechIntent();
        speechRecognizer.setRecognitionListener(new MyRecognitionListener());
    }

    //refresh speech UI
    private void refreshSpeechUI(boolean allowSpeech) {
        if (allowSpeech) {
            ivMic.setImageResource(R.drawable.ic_baseline_mic_24);
        } else {
            ivMic.setImageResource(R.drawable.ic_baseline_mic_off_24);
        }
    }

    //configure TTS
    private void configureTTS() {
        VoiceHelper voiceHelper = new VoiceHelper(this);
        tts = voiceHelper.getTts();
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            @Override
            public void onStart(String utteranceId) {
                allowSpeak = false;
            }

            @Override
            public void onStop(String utteranceId, boolean interrupted) {
                allowSpeak = true;

                ChatActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (allowPlay) {
                            letsPlaying();
                        }
                        if(allowClose&&deep==8){
                            new Handler().postDelayed(new Runnable(){
                                @Override
                                public void run() {
                                    System.exit(1);
                                    finish();
                                }},3000);
                        }
                        refreshSpeechUI(false);
                    }
                });
            }


            @Override
            public void onDone(String utteranceId) {
                allowSpeak = true;

                ChatActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        if (allowPlay) {
                            letsPlaying();
                        }
                        if(allowClose&&deep==8){
                            new Handler().postDelayed(new Runnable(){
                                @Override
                                public void run() {
                                    System.exit(1);
                                    finish();
                                }},3000);
                        }
                        refreshSpeechUI(false);
                    }
                });
            }

            @Override
            public void onError(String utteranceId) {

            }
        });
    }


    //save consultation history
    private void saveConsultationHistory() {
        Calendar calendar = Calendar.getInstance();
        String date = calendar.get(Calendar.DATE) + " " ;
        date += calendar.getDisplayName(Calendar.MONTH, Calendar.LONG, new Locale("id", "ID")) + " ";
        date += String.valueOf(calendar.get(Calendar.YEAR));
        String instructionValue = tagMap.get(feelKey).getValue();
        String feelValue = tagMap.get(feelKey).getChild().get(instructionKey).getValue();
        helper.insertConsul(feelValue, instructionValue, date);
    }

    //start speak
    private void startSpeak(String string) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts.speak(string, TextToSpeech.QUEUE_ADD, null, "text");
        }
    }


    //Check response
    public void setOnline() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            vStatus.setBackground(getDrawable(R.drawable.circle_shape_green));
        }
        tvStatus.setTextColor(getResources().getColor(R.color.onlineGreen));
        tvStatus.setText(getResources().getString(R.string.online));
    }

    //
//
    public void setOffline() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            vStatus.setBackground(getDrawable(R.drawable.circle_shape_orange));
        }
        tvStatus.setTextColor(getResources().getColor(R.color.offlineOrange));
        tvStatus.setText(getResources().getString(R.string.offline));
    }


    private void sendMessage() {
        if (allowShow) {
            Message inputMessage = new Message();
            inputMessage.setMessage(userMessage);
            inputMessage.setId("1");
            messageArrayList.add(inputMessage);
            updateChatRoom();
        } else {
            allowShow = true;
        }
        if (deep == 0) {
            System.out.println("FIRST");
            getFirstRespond();
        } else {
            System.out.println("HALO");
            getRespond();
        }
    }

    private void getFirstRespond() {
        Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                // speak out the bot reply
                try {
                    respond = botHelper.sendChatMessage(userMessage);
                    configureResponse();
                    Message outMessage = new Message();
                    outMessage.setMessage(respond);
                    outMessage.setId("2");
                    messageArrayList.add(outMessage);
                    updateChatRoom();
                    startSpeak(respond);
                    System.out.println("DISINI");
                } catch (Exception e) {
                    System.out.println(e.getMessage());
                    getFirstRespond();
                }

            }
        }, 100);
    }

    private void getRespond() {
        respond = botHelper.sendChatMessage(userMessage);
        configureResponse();
        Message outMessage = new Message();
        outMessage.setMessage(respond);
        outMessage.setId("2");
        messageArrayList.add(outMessage);
        updateChatRoom();
        startSpeak(respond);
    }

    private void updateChatRoom() {
        chatAdapter.notifyDataSetChanged();
        if (chatAdapter.getItemCount() > 1) {
            recyclerView.getLayoutManager().smoothScrollToPosition(recyclerView, null, chatAdapter.getItemCount() - 1);
        }
    }

    //configure bot message edit
    private void configureResponse() {
        if (!isConnected) {
            if (deep == 0) {
                deep=1;
            }
            if (deep == 1) {
                if (respond.trim().charAt(0) == '#') {
                    String[] yorn = respond.substring(1).split("#");
                    respond = yorn[1];
                    if (yorn[0].equalsIgnoreCase("YA")) {
                        allowPlay = true;
                    } else {
                        finish();
                    }
                }
            }
        }
        //online
        else {
            if (deep == 0) {
                if (respond.charAt(0) == '$') {
                    String[] greetings = respond.substring(1).split("#");
                    int hour = 0;
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                        hour = LocalDateTime.now().getHour();
                    }
                    if (hour > 5 && hour < 12) {
                        respond = greetings[0];
                    } else if (hour >= 12 && hour < 15) {
                        respond = greetings[1];
                    } else if (hour >= 15 && hour <= 18) {
                        respond = greetings[2];
                    } else {
                        respond = greetings[3];
                    }
                    respond += greetings[4];
                }
                respond = String.format(respond, name);
                if (firstInit) {
                    deep = 3;
                } else {
                    deep = 1;
                }
            }
            if (deep == 1) {
                if (respond.trim().charAt(0) == '#') {
                    String[] yorn = respond.substring(1).split("#");
                    respond = yorn[1];
                    if (yorn[0].equalsIgnoreCase("GOOD")) {
                        System.out.println("GO0D NEW");
                        deep = 2;
                    } else if (yorn[0].equalsIgnoreCase("BAD")) {
                        System.out.println("BAD NEW");
                        deep = 3;
                    }
                }
            }
            if (deep == 2) {
                if (respond.trim().charAt(0) == '#') {
                    String[] yorn = respond.substring(1).split("#");
                    respond = yorn[1];
                    if (yorn[0].equalsIgnoreCase("YES")) {
                        System.out.println("BUTUH BANTUAN");
                        deep = 3;
                    } else {
                        System.out.println("CLOSE AJA");
                        allowClose = true;
                    }
                }
            }
            if (deep == 3) {
                System.out.println("GET FEEL");
                if (respond.trim().charAt(0) == '#') {
                    String[] yorn = respond.substring(1).split("#");
                    respond = yorn[1];
                    if (yorn[0].equalsIgnoreCase("a01")) {
                        feelKey = "a01";
                    } else if (yorn[0].equalsIgnoreCase("a02")) {
                        feelKey = "a02";
                    } else if (yorn[0].equalsIgnoreCase("a03")) {
                        feelKey = "a03";
                    }
                    deep = 4;
                    System.out.println(feelKey);
                }
            }
            if (deep == 4) {
                System.out.println("NEXT AUDIO");
                if (respond.trim().charAt(0) == '#') {
                    String[] yorn = respond.substring(1).split("#");
                    respond = yorn[1];
                    if (yorn[0].equalsIgnoreCase("YES")) {
                        deep = 5;
                    } else {
                        allowClose = true;
                    }
                }
            }
            if (deep == 5) {
                System.out.println("AUDIO APA");
                if (respond.trim().charAt(0) == '#') {
                    String[] yorn = respond.substring(1).split("#");
                    respond = yorn[1];
                    if (feelKey.equals("a01")) {
                        if (yorn[0].equalsIgnoreCase("b01")) {
                            instructionKey = "b01";
                            System.out.println("b01");

                        } else if (yorn[0].equalsIgnoreCase("b02")) {
                            instructionKey = "b02";
                            System.out.println("b02");

                        } else if (yorn[0].equalsIgnoreCase("b03")) {
                            instructionKey = "b03";
                            System.out.println("b03");
                        }
                    } else if (feelKey.equals("a02")) {
                        if (yorn[0].equalsIgnoreCase("b04")) {
                            instructionKey = "b04";
                            System.out.println("b04");

                        } else if (yorn[0].equalsIgnoreCase("b05")) {
                            instructionKey = "b05";
                            System.out.println("b05");

                        } else if (yorn[0].equalsIgnoreCase("b06")) {
                            instructionKey = "b06";
                            System.out.println("b06");

                        }
                    } else if (feelKey.equals("a03")) {
                        if (yorn[0].equalsIgnoreCase("b07")) {
                            instructionKey = "b07";
                            System.out.println("b07");

                        } else if (yorn[0].equalsIgnoreCase("b07")) {
                            instructionKey = "b07";
                            System.out.println("b07");
                        }
                    }
                    deep = 6;
                    allowPlay = true;
                }
            }else if(deep==6){
                if(afterPlay){
                    allowClose = true;
                    if (respond.trim().charAt(0) == '#') {
                        String[] yorn = respond.substring(1).split("#");
                        respond = yorn[1];
                        if (yorn[0].equalsIgnoreCase("YES")) {
                            deep = 3;
                        } else if(yorn[0].equalsIgnoreCase("NO")) {
                            allowClose = true;
                            deep = 7;
                        } else if(yorn[0].equalsIgnoreCase("HISTORY")){
                            afterPlay = false;
                            respond = helper.readHistory();
                            allowClose = true;
                        }
                    }
                }
            }else if (deep==7){
                FirebaseDatabase database = FirebaseDatabase.getInstance();
                DatabaseReference reference = database.getReference("ulasan");
                reference.push().setValue(userMessage);
                respond = "Terima kasih, Aplikasi satunetra akan menutup dalam 3 detik, sampai jumpa lagi";
                deep = 8;
            }
        }
    }

    private void letsPlaying() {
        System.out.println("TERPANGGIL");
        isTimer = true;
        tvTimer.setVisibility(View.VISIBLE);
        ivMic.setVisibility(View.GONE);

        cTimer = new CountDownTimer(5000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                String time = String.valueOf(millisUntilFinished / 1000);
                tvTimer.setText(time + " detik");
            }

            @Override
            public void onFinish() {
                Intent playIntent = new Intent(ChatActivity.this, RoomActivity.class);
                if (isConnected) {
                    test = tagMap.get(feelKey).getChild().get(instructionKey).getListUrl();
                    playIntent.putStringArrayListExtra("link", (ArrayList<String>) test);
                    playIntent.putExtra("connected", "online");
                    playIntent.putExtra("type", tagMap.get(feelKey).getChild().get(instructionKey).getValue());
                } else {
                    playIntent.putExtra("connected", "offline");
                    playIntent.putExtra("type", "Musik Offline");
                }
                startActivity(playIntent);
                afterPlay = true;
                cTimer.cancel();
                tvTimer.setVisibility(View.GONE);
                ivMic.setVisibility(View.VISIBLE);
            }
        };
        cTimer.start();
    }

    @Override
    protected void onStart() {
        allowPlay = false;
        if(cTimer!=null)
            cTimer.cancel();
        isTimer = false;
        if(afterPlay){
            if(isConnected){
                allowShow = false;
                saveConsultationHistory();
                deep = 6;
                userMessage = "END";
                sendMessage();
            }else{
                allowClose = false;
                deep = 3;
                try{
                    new AsyncAction().execute();
                }catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        super.onStart();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        if (v.getId() == R.id.btn_gestur_chat) {
            gestureDetector.onTouchEvent(event);
            return true;
        }
        return false;
    }

    class GestureListener extends GestureDetector.SimpleOnGestureListener {
        @Override
        public boolean onSingleTapUp(MotionEvent e) {
            if (allowSpeak && (!allowPlay && !allowClose || deep>=6)) {
                refreshSpeechUI(true);
                speechRecognizer.startListening(speechIntent);
            } else if (!allowSpeak) {
                tts.stop();
            }
            return false;
        }

        @Override
        public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY) {
            boolean result = false;
            try {
                float diffY = e2.getY() - e1.getY();
                float diffX = e2.getX() - e1.getX();
                if (Math.abs(diffX) > Math.abs(diffY)) {
                    if (Math.abs(diffX) > SWIPE_THRESHOLD && Math.abs(velocityX) > SWIPE_VELOCITY_THRESHOLD) {
                        //swipe from left to right
                        if (diffX > 0) {
                            if (allowClose) {
                                if(deep==8){
                                    userMessage = "CLOSE0";
                                    allowShow = false;
                                    sendMessage();
                                }else{
                                    finish();
                                }

                            }
                        }
                        result = true;
                    }
                } else {
                    if (Math.abs(diffY) > SWIPE_THRESHOLD && Math.abs(velocityY) > SWIPE_VELOCITY_THRESHOLD) {
                        if (diffY > 0) {
                            if(allowClose){
                                userMessage = "HELP";
                                allowShow = false;
                                allowClose=false;
                                deep = 3;
                                sendMessage();
                            }
                        }
                        result = true;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }

            return result;
        }
    }

    private class AsyncAction extends AsyncTask<String, Void, String> {

        protected String doInBackground(String... args) {
            ConnectivityManager connectivityManager = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
            if (connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_MOBILE).getState() == NetworkInfo.State.CONNECTED ||
                    connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState() == NetworkInfo.State.CONNECTED) {
                //we are connected to a network
                String url_ping = "https://www.redhat.com/";
                HttpGet httpGet = new HttpGet(url_ping);
                HttpParams httpParameters = new BasicHttpParams();
                int timeoutConnection = 2000;
                HttpConnectionParams.setConnectionTimeout(httpParameters, timeoutConnection);

                int timeoutSocket = 4000;
                HttpConnectionParams.setSoTimeout(httpParameters, timeoutSocket);
                DefaultHttpClient httpClient = new DefaultHttpClient(httpParameters);
                try {
                    httpClient.execute(httpGet);
                    isConnected = true;
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else
                isConnected = false;
            return null;
        }

        protected void onPostExecute(String result) {
            if (isConnected) {
                setOnline();
                if(deep==0){
                    if (firstInit) {
                        userMessage = "W1";
                    } else {
                        userMessage = "W2";
                    }
                }else{
                    deep = 3;
                    userMessage = "HELP";
                }
            } else {
                setOffline();
                userMessage = "WOFFLINE";
            }
            allowShow = false;
            sendMessage();
        }
    }
}