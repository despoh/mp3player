package com.example.mp3player;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class PlayerService extends Service {

    MP3Player player;

    private IBinder binder = new MyBinder();

    public PlayerService() {
        player = new MP3Player();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String path = intent.getStringExtra("path");
        Log.d("pondo","onstartCommand get Called");
        player.load(path);
        player.play();
        return START_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return binder;
    }

    public class MyBinder extends Binder{
        PlayerService getService(){
            return PlayerService.this;
        }
    }

    @Override
    public boolean stopService(Intent name) {
        return super.stopService(name);

    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);

    }

}
