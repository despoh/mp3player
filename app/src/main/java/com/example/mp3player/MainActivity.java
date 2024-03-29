package com.example.mp3player;

import androidx.appcompat.app.AppCompatActivity;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.os.Environment;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    PlayerService playerService;
    Thread progressBarThread ;

    private Boolean isServiceBounded;

    private ProgressBar progressBar;
    private Button playPauseButton;
    private Button stopButton;
    private TextView songTitleTextView;
    private ListView songsListView;
    private TextView songDurationTextView;
    private TextView songProgressTextView;

    private String lastSelectedSongPath;

    private Boolean isComeBackFromPendingIntent = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        lastSelectedSongPath = pref.getString("lastSelectedSongPath","");

        linkViewToVariables();
        populateSongsListView();
        isServiceBounded = false;

    }





    @Override
    protected void onStart() {
        super.onStart();

        isComeBackFromPendingIntent = getIntent().getBooleanExtra("return",false);
        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0);
        if(isComeBackFromPendingIntent){

            Intent intent = new Intent(this,  PlayerService.class);
            intent.putExtra("path",lastSelectedSongPath);
            songProgressTextView.setText( minuteSecondString(pref.getInt("lastProgress",0)));
            this.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        }else{
            stopButton.setEnabled(false);
            playPauseButton.setEnabled(false);
            songTitleTextView.setText("");
        }
    }

    @Override
    protected void onStop() {
        super.onStop();

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();

        if(isServiceBounded){
            editor.putBoolean("return", true);
            editor.putString("lastSelectedSongPath",lastSelectedSongPath);
            editor.putInt("lastProgress",playerService.player.getProgress());
            unbindService(connection);
        }



        editor.putString("lastSelectedSongPath",lastSelectedSongPath);
        editor.commit();


    }


    private void populateSongsListView(){
        File musicDir = new File(
                Environment.getExternalStorageDirectory().getPath()+ "/Music/");
        File list[] = musicDir.listFiles();
        List nameArray = new ArrayList<String>();

        for(File file : list){
            nameArray.add(file.getName());
        }
        songsListView.setAdapter(new ArrayAdapter<String>(this,android.R.layout.simple_list_item_1, nameArray));
        songsListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> myAdapter, View myView, int myItemInt, long mylng) {
                String selectedFromList =(String) (songsListView.getItemAtPosition(myItemInt));
                lastSelectedSongPath = Environment.getExternalStorageDirectory().getPath()+ "/Music/" + selectedFromList;
                playMusic(Environment.getExternalStorageDirectory().getPath()+ "/Music/" + selectedFromList);
            } });
    }

    private void playMusic(String path){

        SharedPreferences pref = getApplicationContext().getSharedPreferences("MyPref", 0); // 0 - for private mode
        SharedPreferences.Editor editor = pref.edit();

        if(isServiceBounded){
            if(playerService.player.getState() == MP3Player.MP3PlayerState.PLAYING || playerService.player.getState() == MP3Player.MP3PlayerState.PAUSED){
                playerService.player.stop();
            }

            playerService.player.load(path);
            playerService.player.play();
            updateUIToShowProgress();

        }else{
            Intent intent = new Intent(this,  PlayerService.class);
            intent.putExtra("path",path);
            isServiceBounded = true;
            startService(intent);
            this.bindService(intent, connection, Context.BIND_AUTO_CREATE);

        }

        playPauseButton.setText("Pause");
        stopButton.setEnabled(true);
        playPauseButton.setEnabled(true);



    }

    private void linkViewToVariables(){

        progressBar = (ProgressBar) findViewById(R.id.progressBar);
        playPauseButton = (Button) findViewById(R.id.playPauseButton);
        stopButton = (Button) findViewById(R.id.stopButton);
        songTitleTextView = (TextView) findViewById(R.id.songTitleTextView);
        songsListView = (ListView) findViewById(R.id.songsListView);
        songDurationTextView =  (TextView) findViewById(R.id.songDurationTextView);
        songProgressTextView = (TextView) findViewById(R.id.songProgressTextView);

    }


    public void playPauseButtonClicked(View view) {
        if(playerService.player.state == MP3Player.MP3PlayerState.PLAYING){
            playerService.player.pause();
            playPauseButton.setText("Play");
        }else if(playerService.player.state == MP3Player.MP3PlayerState.PAUSED){
            playerService.player.play();
            playPauseButton.setText("Pause");
            updateUIToShowProgress();
        }else if(playerService.player.state == MP3Player.MP3PlayerState.STOPPED){
            playMusic(lastSelectedSongPath);
        }
    }

    public void stopButtonClicked(View view) {
        if(isServiceBounded && (playerService.player.state == MP3Player.MP3PlayerState.PLAYING || playerService.player.state == MP3Player.MP3PlayerState.PAUSED)){
            Intent myService = new Intent(this, PlayerService.class);
            this.stopService(myService);
            this.unbindService(connection);
            isServiceBounded = false;

            progressBar.setProgress(1);
            songProgressTextView.setText("0 : 00");
            songDurationTextView.setText("0 : 00");
            playPauseButton.setText("Play");
            stopButton.setEnabled(false);

        }
    }

    public void updateUIToShowProgress(){

        if(playerService.player.state == MP3Player.MP3PlayerState.PLAYING){
            playPauseButton.setText("Pause");
            stopButton.setEnabled(true);
        }else{
             playPauseButton.setText("Play");
        }

        progressBar.setProgress(0);
        songDurationTextView.setText(minuteSecondString(playerService.player.getDuration()));


        songTitleTextView.setText(playerService.player.getFilePath().substring(26));


        progressBarThread = new Thread(new Runnable() {
            @Override
            public void run() {


                while(isServiceBounded && playerService.player.getProgress() < playerService.player.getDuration() && playerService.player.state == MP3Player.MP3PlayerState.PLAYING){

                    final double percent = (double)playerService.player.getProgress()/playerService.player.getDuration()*100;

                    if(!(progressBar.getProgress() == (int)percent)){
                        if(isServiceBounded){
                            progressBar.setProgress((int)percent,true);

                        }else{
                            progressBar.setProgress(0,true);

                        }
                    }



                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {

                            if(isServiceBounded){
                                if(playerService.player.getProgress()<playerService.player.getDuration()){
                                    songProgressTextView.setText(minuteSecondString(playerService.player.getProgress()));
                                }else{
                                    playPauseButton.setText("Play");
                                    playerService.player.stop();
                                }

                            }

                        }
                    });

                }




            }
            
        });



        progressBarThread.start();
    }



    public String minuteSecondString(int millsec){
        int minutes = (millsec/1000) /60;
        int second = (millsec/1000) % 60;

        if(second<10){
            return minutes + " : 0" + second;
        }else{
            return minutes + " : " + second;

        }
    }

    private ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
            PlayerService.MyBinder binder = (PlayerService.MyBinder) iBinder;
            playerService = binder.getService();
            isServiceBounded = true;
            updateUIToShowProgress();
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            isServiceBounded=false;
        }


    };


}

