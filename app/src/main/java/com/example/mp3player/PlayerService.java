package com.example.mp3player;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Icon;
import android.os.Binder;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

public class PlayerService extends Service {

    final String CHANNEL_ID = "MY_CHANNEL";
    final int NOTIFY_ID = 2222;

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
        createChannel();
        sendNotification();
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
    public void onDestroy() {
        Toast.makeText(this, "Playing service Stopped", Toast.LENGTH_LONG).show();
        player.stop();
        stopForeground(true);
        super.onDestroy();
    }

    public void sendNotification(){
        Intent resultIntent = new Intent(this, MainActivity.class);
        resultIntent.putExtra("return",true);

        PendingIntent pendingIntent =
                PendingIntent.getActivity(
                        this,
                        0,
                        resultIntent,
                        PendingIntent.FLAG_UPDATE_CURRENT
                );


        final Icon icon = Icon.createWithResource(this,
                android.R.drawable.ic_dialog_info);

        Notification.Action action =
                new Notification.Action.Builder(icon, "Open", pendingIntent)
                        .build();

        Notification notification =
                new Notification.Builder(this,
                        CHANNEL_ID)
                        .setContentTitle("Example Notification")
                        .setContentText("This is an example notification.")
                        .setSmallIcon(android.R.drawable.ic_dialog_info)
                        .setChannelId(CHANNEL_ID)
                        .setContentIntent(pendingIntent)
                        .setActions(action)
                        .build();

        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFY_ID, notification);
        startForeground(NOTIFY_ID,notification);
    }

    public void createChannel(){
        NotificationManager notificationManager = (NotificationManager)
                getSystemService(Context.NOTIFICATION_SERVICE);

        CharSequence name = "NotifyDemo News";
        String description = "Example News Channel";
        int importance = NotificationManager.IMPORTANCE_LOW;
        NotificationChannel channel =
                new NotificationChannel(CHANNEL_ID, name, importance);
        channel.setDescription(description);
        channel.enableLights(true);
        channel.setLightColor(Color.RED);
        channel.enableVibration(true);
        channel.setVibrationPattern(
                new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});

        notificationManager.createNotificationChannel(channel);
    }


}
