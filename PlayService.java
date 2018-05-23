package comzerone1stsimsim.github.hw4;

import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static comzerone1stsimsim.github.hw4.MusicPlayActivity.ACTIVITY_STATE;
import static comzerone1stsimsim.github.hw4.MusicPlayActivity.ON_PLAY;

/**
 * Created by yicho on 2017-12-24.
 */

public class PlayService extends Service {

    ArrayList<Music> musics;
    int position = 0;
    int playTime = 0;
    String path;
    MediaPlayer mPlayer;
    BroadcastReceiver musicReceiver;
    NotificationCompat.Builder mBuilder;

    static int PLAYED = 0;
    final static int NOTIFY_ID = 1529;

    final static String PLAY_ACTION = "comzerone1stsimsim.github.hw4.action.PLAY_MUSIC";
    final static String PAUSE_ACTION = "comzerone1stsimsim.github.hw4.action.PAUSE_MUSIC";
    final static String FORWARD_ACTION = "comzerone1stsimsim.github.hw4.action.FORWARD_MUSIC";
    final static String BACKWARD_ACTION = "comzerone1stsimsim.github.hw4.action.BACKWARD_MUSIC";
    final static String CHANGE_ACTION = "comzerone1stsimsim.github.hw4.action.PLAY_MUSIC_CHANGED";
    final static String STOP_ACTION = "comzerone1stsimsim.github.hw4.action.STOP";
    final static String DESTROY_ACTION = "comzerone1stsimsim.github.hw4.action.DESTROY";
    final static String RESTART_ACTION = "comzerone1stsimsim.github.hw4.action.RESTART";

    @Override
    public void onCreate(){
        super.onCreate();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId){

        mPlayer = new MediaPlayer();
        mPlayer.setOnCompletionListener(playerOnComplete);

        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(PLAY_ACTION);
        intentFilter.addAction(PAUSE_ACTION);
        intentFilter.addAction(FORWARD_ACTION);
        intentFilter.addAction(BACKWARD_ACTION);
        intentFilter.addAction(CHANGE_ACTION);
        intentFilter.addAction(STOP_ACTION);
        intentFilter.addAction(DESTROY_ACTION);
        intentFilter.addAction(RESTART_ACTION);

        musicReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                musics = (ArrayList<Music>)intent.getSerializableExtra("MUSIC");
                position = intent.getIntExtra("POSITION", position);
                playTime = intent.getIntExtra("SEEKBAR_POS", playTime);
                path = Environment.getExternalStorageDirectory().getPath() + File.separator;

                makeNotification(intent.getAction());       //들어온 액션을 가지고 노티피케이션 생성.
                Log.e("receiver getAction", intent.getAction());
                /**여기서 부터 노래 재생 부분*/
                if(intent.getAction().equals(PLAY_ACTION)) {                  //play, 자동재생
                    ON_PLAY = 1;
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                if(PLAYED == 1) {           //일시정지에서 풀려난 경우. 그대로 다시 재생.
                                    mPlayer.start();
                                } else {
                                    mPlayer.reset();        //액티비티가 죽었을 경우 다시 틀었을 떄 리셋되도록.
                                    mPlayer.setDataSource(path + musics.get(position).getDisplayName());
                                    mPlayer.prepare();
                                    mPlayer.start();
                                    PLAYED = 1;
                                    sendDuration();
                                }
                            } catch (IOException e) { Log.d("tag", e.getMessage());}
                        }
                    }).start();

                } else if(intent.getAction().equals(PAUSE_ACTION)){           //pause
                    ON_PLAY = 0;
                    mPlayer.pause();

                } else if(intent.getAction().equals(FORWARD_ACTION) || intent.getAction().equals(BACKWARD_ACTION)){      //forward, backward

                    mPlayer.reset();
                    mPlayer.setOnCompletionListener(playerOnComplete);

                    //새로 포지션 설정해서 재생.
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                mPlayer.setDataSource(path + musics.get(position).getDisplayName());
                                mPlayer.prepare();
                                mPlayer.start();
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                            musicComplete();
                        }
                    }).start();

                } else if(intent.getAction().equals(CHANGE_ACTION)){

                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            mPlayer.seekTo(playTime);
                            mPlayer.start();
                        }
                    }).start();

                } else if(intent.getAction().equals(DESTROY_ACTION)) {          //die를 받으면 재생플래그를 1으로 설정
                    PLAYED = 1;
                } else if(intent.getAction().equals(RESTART_ACTION)){
                    Log.e("onReceive()", "Restart_action");
                    Intent restartIntent = new Intent(context, MusicPlayActivity.class);
                    restartIntent.putExtra("ONPLAY", ON_PLAY);
                    restartIntent.putExtra("musics", musics);
                    restartIntent.putExtra("position", position);
                    restartIntent.putExtra("DURATION", mPlayer.getDuration());
                    restartIntent.putExtra("CUR_POS", mPlayer.getCurrentPosition());
                    context.startActivity(restartIntent);
                }

            }

        };
        registerReceiver(musicReceiver, intentFilter);

        return START_REDELIVER_INTENT;
    }

    /**액티비티로 duration 보내는 방송*/
    public void sendDuration(){
        /**방송 보내기*/
        //Log.e("After registerReceiver", "send broadcast");
        Intent ProgressIntent = new Intent();
        ProgressIntent.setAction("comzerone1stsimsim.github.hw4.action.DURATION");
        ProgressIntent.putExtra("DURATION", mPlayer.getDuration());
        ProgressIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        sendBroadcast(ProgressIntent);
    }
    /**액티비티로 현재 진행 중인 노래 끝날 시 방송*/
    public void musicComplete(){
        Intent completeIntent = new Intent();
        completeIntent.setAction("comzerone1stsimsim.github.hw4.action.COMPLETE");
        completeIntent.putExtra("DURATION", mPlayer.getDuration());
        completeIntent.putExtra("POSITION", position);
        completeIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        sendBroadcast(completeIntent);
    }

    public void makeNotification(String action){

        RemoteViews remoteView = new RemoteViews(getPackageName(), R.layout.notification_layout);
        Intent notiAlbumIntent = new Intent(this, MusicPlayActivity.class);
        Intent notiAlbumIntent2 = new Intent();
        Intent notiPlayIntent = new Intent();           //noti에서 플레이버튼 클릭시.
        Intent notiForwardIntent = new Intent();
        Intent notiBackwardIntent = new Intent();

        Uri uri = Uri.parse("content://media/external/audio/albumart/" + musics.get(position).getAlbumId());    //앨범 이미지 uri

        remoteView.setImageViewUri(R.id.noti_album_iv, uri);                                //현재 재생중인 앨범 이미지
        remoteView.setTextViewText(R.id.noti_title_tv, musics.get(position).getTitle());    //음악 이름

        notiAlbumIntent.putExtra("musics", musics);         //stop 상태에서 앨범아트 클릭시
        notiAlbumIntent.putExtra("position", position);
        notiAlbumIntent.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);  //차례대로 새로운 태스크 생성, 하나의 액티비티 인스턴스만 존재, 호출한 액티비티 위에 존재하는 태스크 제거

        notiAlbumIntent2.putExtra("MUSIC", musics);        //destroy 상태에서 앨범아트 클릭시
        notiAlbumIntent2.putExtra("POSITION", position);
        notiAlbumIntent2.addFlags(FLAG_ACTIVITY_NEW_TASK | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
        notiAlbumIntent2.setAction(RESTART_ACTION);

        notiPlayIntent.putExtra("MUSIC", musics);
        notiPlayIntent.putExtra("POSITION", position);
        notiPlayIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);


        if(position < musics.size() - 1)                        //position이 음악 수 초과시 0으로
            notiForwardIntent.putExtra("POSITION", position+1);
        else
            notiForwardIntent.putExtra("POSITION", 0);

        notiForwardIntent.putExtra("MUSIC", musics);
        notiForwardIntent.setAction(FORWARD_ACTION);
        notiForwardIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);


        if(position > 0)
            notiBackwardIntent.putExtra("POSITION", position-1);
        else
            notiBackwardIntent.putExtra("POSITION", musics.size()-1);

        notiBackwardIntent.putExtra("MUSIC", musics);
        notiBackwardIntent.setAction(BACKWARD_ACTION);
        notiBackwardIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY | FLAG_ACTIVITY_SINGLE_TOP | FLAG_ACTIVITY_CLEAR_TOP);
        //차례대로 새로운 태스크 생성, 하나의 액티비티 인스턴스만 존재, 호출한 액티비티 위에 존재하는 태스크 제거

        switch (action){
            case PLAY_ACTION : {            //재생 액션이 들어왔을 때
                notiPlayIntent.setAction(PAUSE_ACTION);
                remoteView.setImageViewResource(R.id.noti_play_iv, R.drawable.pause);               //버튼 pause로 변경.
                break;
            }
            case PAUSE_ACTION : {
                notiPlayIntent.setAction(PLAY_ACTION);
                remoteView.setImageViewResource(R.id.noti_play_iv, R.drawable.play);                //버튼 play로 변경.
                break;
            }
            case FORWARD_ACTION : {
                notiPlayIntent.setAction(PAUSE_ACTION);
                remoteView.setImageViewResource(R.id.noti_play_iv, R.drawable.pause);               //버튼 pause로 변경.
                break;
            }
            case BACKWARD_ACTION : {
                notiPlayIntent.setAction(PAUSE_ACTION);
                remoteView.setImageViewResource(R.id.noti_play_iv, R.drawable.pause);               //버튼 pause로 변경.
                break;
            }
            case STOP_ACTION : {
                notiPlayIntent.setAction(PAUSE_ACTION);
            }
            case DESTROY_ACTION : {
                notiPlayIntent.setAction(PAUSE_ACTION);
            }
            case RESTART_ACTION : {
                notiPlayIntent.setAction(PAUSE_ACTION);
            }
        }

        PendingIntent pPlayIntent = PendingIntent.getBroadcast(this, 0, notiPlayIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pForwardIntent = PendingIntent.getBroadcast(this, 0, notiForwardIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        PendingIntent pBackwardIntent = PendingIntent.getBroadcast(this, 0, notiBackwardIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        remoteView.setOnClickPendingIntent(R.id.noti_play_iv, pPlayIntent);
        remoteView.setOnClickPendingIntent(R.id.noti_forward_iv, pForwardIntent);
        remoteView.setOnClickPendingIntent(R.id.noti_backward_iv, pBackwardIntent);


        if(ACTIVITY_STATE == "onStop" || ACTIVITY_STATE == "onCreate"){

            PendingIntent pAlbumIntent = PendingIntent.getActivity(this, 0, notiAlbumIntent, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteView.setOnClickPendingIntent(R.id.noti_album_iv, pAlbumIntent);

        } else if(ACTIVITY_STATE == "onDestroy"){
            //Log.e("makeNotification", "onDestroy, pendingIntent");
            PendingIntent pAlbumIntent2 = PendingIntent.getBroadcast(this, 0, notiAlbumIntent2, PendingIntent.FLAG_UPDATE_CURRENT);
            remoteView.setOnClickPendingIntent(R.id.noti_album_iv, pAlbumIntent2);

        }

        mBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this)
                .setSmallIcon(R.drawable.play_noti)
                .setContent(remoteView);

        //notiManager.notify(NOTIFY_ID, mBuilder.build());
        startForeground(NOTIFY_ID, mBuilder.build());
    }

    /**재생 완료시 호출 되는 리스너*/
    MediaPlayer.OnCompletionListener playerOnComplete = new MediaPlayer.OnCompletionListener() {
        @Override
        public void onCompletion(MediaPlayer mp) {

            if (position < musics.size() - 1) { position += 1; } //노래가 마지막이 아니면 다음 노래 재생
            else { position = 0; }                               //마지막이면 맨 처음 노래 재생

            makeNotification(PLAY_ACTION);

            mPlayer.reset();
            mPlayer.setOnCompletionListener(playerOnComplete);

            //새로 포지션 설정해서 재생.
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        mPlayer.setDataSource(path + musics.get(position).getDisplayName());
                        mPlayer.prepare();
                        mPlayer.start();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    musicComplete();         //forward, backward클릭시 액티비티로 방송
                }
            }).start();

        }
    };

    @Override
    public void onDestroy() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        unregisterReceiver(musicReceiver);
        super.onDestroy();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

}
