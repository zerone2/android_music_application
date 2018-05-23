package comzerone1stsimsim.github.hw4;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;

import java.util.ArrayList;

import static comzerone1stsimsim.github.hw4.PlayService.PAUSE_ACTION;
import static comzerone1stsimsim.github.hw4.PlayService.PLAYED;
import static comzerone1stsimsim.github.hw4.PlayService.PLAY_ACTION;

/**
 * Created by yicho on 2017-12-23.
 */

public class MusicPlayActivity extends Activity {


    ArrayList<Music> musics;
    int position = 0;
    static int ON_PLAY = 1;
    static String ACTIVITY_STATE = null;

    int totalDuration, totalMinute, totalSecond;
    int mDuration;
    int playTime = 0;
    int currentPos = 0;

    ImageView albumIV;
    TextView titleTV;
    ImageView playIV;
    ImageView forwardIV;
    ImageView backwardIV;
    SeekBar seekBar;
    TextView durationTV;

    BroadcastReceiver serviceReceiver;
    Thread progressThread;

    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.playscreen_layout);     //레이아웃변경.
        ACTIVITY_STATE = "onCreate";

        albumIV = (ImageView) findViewById(R.id.album_iv);
        titleTV = (TextView) findViewById(R.id.title_tv);
        playIV = (ImageView) findViewById(R.id.play_iv);
        forwardIV = (ImageView) findViewById(R.id.forward_iv);
        backwardIV = (ImageView) findViewById(R.id.backward_iv);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        durationTV = (TextView) findViewById(R.id.duration_tv);

        Intent rcvIntent = getIntent();        //메인 액티비티에서 음악정보 넘겨받기

        musics = (ArrayList<Music>) rcvIntent.getSerializableExtra("musics");  //음악 배열리스트
        position = rcvIntent.getIntExtra("position", position);                //누른 음악의 위치


        playIV.setImageResource(R.drawable.pause);
        ON_PLAY = 1;
        Uri uri = Uri.parse("content://media/external/audio/albumart/" + musics.get(position).getAlbumId());    //앨범 이미지 uri
        albumIV.setImageURI(uri);
        titleTV.setText(musics.get(position).getTitle() + " - " + musics.get(position).getArtist());            //노래 타이틀, 가수 표시

        rcvBroadcast();             //서비스로부터 방송받아 seekBar 등 설정, 쓰레드 생성.

        if(PLAYED == 1){
            currentPos = rcvIntent.getIntExtra("CUR_POS", currentPos);
            totalDuration = rcvIntent.getIntExtra("DURATION", totalDuration) + 1;
            totalMinute = totalDuration / 60000;
            totalSecond = (totalDuration % 60000)/1000 + 1;
            mDuration = (totalDuration/1000) + 1;

            seekBar.setMax(mDuration);

            makeThread();
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            playTime = currentPos/1000;

            ON_PLAY = rcvIntent.getIntExtra("ONPLAY", ON_PLAY);
            if(ON_PLAY == 1)
                playIV.setImageResource(R.drawable.pause);
            else
                playIV.setImageResource(R.drawable.play);

            Log.e("CUR_POS in ACTIVITY", currentPos+"");

        } else {
            playMusic("PLAY_MUSIC");    //service로 방송을 보내 음악 재생.
        }

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                ON_PLAY = 0;
                playMusic("PAUSE_MUSIC");
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                ON_PLAY = 1;
                playTime = seekBar.getProgress();
                playMusic("PLAY_MUSIC_CHANGED");
                playIV.setImageResource(R.drawable.pause);
            }
        });
    }

    /**각 버튼의 onClick함수*/
    public void onClick(View v){
        int id = v.getId();
        if(id == R.id.play_iv && ON_PLAY == 1){         //플레이 상태

            playMusic("PAUSE_MUSIC");
            playIV.setImageResource(R.drawable.play);

        } else if (id == R.id.play_iv && ON_PLAY == 0){ //일시정지 상태

            playMusic("PLAY_MUSIC");
            playIV.setImageResource(R.drawable.pause);

        } else if (id == R.id.forward_iv){

            if(position < musics.size()-1) {
                position += 1;      //포지션 증가.
                playMusic("FORWARD_MUSIC");     //서비스한테 방송.
            } else {
                position = 0;
                playMusic("FORWARD_MUSIC");
                //Toast.makeText(this, "재생목록의 끝 입니다.",Toast.LENGTH_LONG).show();
            }

        } else if (id == R.id.backward_iv){
            if(position > 0){

                position -= 1;
                playMusic("BACKWARD_MUSIC");

            } else {
                position = musics.size()-1;
                playMusic("BACKWARD_MUSIC");
                //Toast.makeText(this, "재생목록의 시작 입니다.", Toast.LENGTH_LONG).show();
            }

        }

    }

    /**서비스한테 방송을 보내 음악 재생 함수*/
    public void playMusic(String action){
        Intent broadcastIntent = new Intent();      //서비스로 방송보내기.
        broadcastIntent.setAction("comzerone1stsimsim.github.hw4.action." + action);
        broadcastIntent.putExtra("MUSIC", musics);
        broadcastIntent.putExtra("POSITION", position);
        broadcastIntent.putExtra("SEEKBAR_POS", playTime*1000);
        broadcastIntent.addFlags(Intent.FLAG_RECEIVER_REGISTERED_ONLY);
        sendBroadcast(broadcastIntent);
    }

    /**서비스로부터 방송받는 함수(재생시간 받아오기)*/
    public void rcvBroadcast(){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("comzerone1stsimsim.github.hw4.action.DURATION");
        intentFilter.addAction("comzerone1stsimsim.github.hw4.action.COMPLETE");
        intentFilter.addAction(PLAY_ACTION);
        intentFilter.addAction(PAUSE_ACTION);

        serviceReceiver = new BroadcastReceiver(){
            @Override
            public void onReceive(Context context, Intent intent) {

                if(intent.getAction().equals("comzerone1stsimsim.github.hw4.action.DURATION")) {        //최초 재생시 duration을 받아오기 위한 리시버.

                    playIV.setImageResource(R.drawable.pause);

                    totalDuration = intent.getIntExtra("DURATION", totalDuration);
                    totalMinute = totalDuration / 60000;
                    totalSecond = (totalDuration % 60000)/1000 + 1;
                    mDuration = (totalDuration/1000) + 1;

                    seekBar.setMax(mDuration);

                    makeThread();       //play, forward, backward 방송 받았을 때 쓰레드 생성후 시작.

                } else if (intent.getAction().equals("comzerone1stsimsim.github.hw4.action.COMPLETE")){ //back, forward등 다른 노래를 처음부터 시작할 때 리시버

                    progressThread.interrupt();
                    Log.e("rcvBroadcast", "COMPLETE");

                    totalDuration = intent.getIntExtra("DURATION", totalDuration) + 1;
                    position = intent.getIntExtra("POSITION", position);
                    totalMinute = totalDuration / 60000;
                    totalSecond = (totalDuration % 60000)/1000 + 1;
                    mDuration = (totalDuration/1000) + 1;

                    seekBar.setMax(mDuration);          //총 재생 시간 설정.
                    Uri uri = Uri.parse("content://media/external/audio/albumart/" + musics.get(position).getAlbumId());    //앨범 이미지 uri
                    albumIV.setImageURI(uri);
                    titleTV.setText(musics.get(position).getTitle() + " - " + musics.get(position).getArtist());            //노래 타이틀, 가수 표시
                    playIV.setImageResource(R.drawable.pause);
                    ON_PLAY = 1;

                    makeThread();       //play, forward, backward 방송 받았을 때 쓰레드 생성후 시작.

                } else if(intent.getAction().equals(PLAY_ACTION)){
                    playIV.setImageResource(R.drawable.pause);
                } else if(intent.getAction().equals(PAUSE_ACTION)){
                    playIV.setImageResource(R.drawable.play);
                }
            }
        };
        registerReceiver(serviceReceiver,intentFilter);
    }

    /**seekbar와 밑에 진행상황 텍스트뷰 업데이트 용 쓰레드*/
    public void makeThread(){

        progressThread = null;
        progressThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {       //interrupt 들어왔을 때 쓰레드 중지.
                        int minute, second;

                        for (playTime = 0; playTime <= mDuration; playTime++) {                //duration / 1000
                            while (ON_PLAY == 0) { Thread.sleep(500); }   //ON_PLAY = 0 (일시정지 상태) 이면 무한루프.
                            minute = playTime / 60;
                            second = playTime % 60;

                            final int finalPlayTime = playTime;
                            final int finalMinute = minute;
                            final int finalSecond = second;

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    seekBar.setProgress(finalPlayTime);
                                    if(totalSecond < 10) {
                                        if (finalSecond < 10) {
                                            durationTV.setText(finalMinute + ":0" + finalSecond + " / " + totalMinute + ":0" + totalSecond);
                                        } else {
                                            durationTV.setText(finalMinute + ":" + finalSecond + " / " + totalMinute + ":0" + totalSecond);
                                        }
                                    } else {
                                        if (finalSecond < 10) {
                                            durationTV.setText(finalMinute + ":0" + finalSecond + " / " + totalMinute + ":" + totalSecond);
                                        } else {
                                            durationTV.setText(finalMinute + ":" + finalSecond + " / " + totalMinute + ":" + totalSecond);
                                        }
                                    }
                                }
                            });
                            Thread.sleep(1000);     //1초마다 증가.

                        }
                        Thread.currentThread().interrupt();     //for문이 끝났으면 interrupt 발생시켜서 쓰레드 중지
                    }
                } catch (InterruptedException e){}

            }
        });
        progressThread.start();
    }

    protected void onResume(){
        super.onResume();
        ACTIVITY_STATE = "onResume";
        Log.v("onResume()", "onResume");
    }

    protected void onPause(){
        super.onPause();
        ACTIVITY_STATE = "onPause";
        Log.v("onPause()", "onPause");
    }

    protected void onStop(){
        super.onStop();
        ACTIVITY_STATE = "onStop";
        playMusic("STOP");
        Log.v("onStop()", "onStop");
    }

    protected void onDestroy(){
        Log.v("onDestroy()","onDestroy");
        ACTIVITY_STATE = "onDestroy";
        playMusic("DESTROY");           //죽을 때 서비스한테 die 방송을 보내 다시 곡을 재생할 수 있도록 함.
        unregisterReceiver(serviceReceiver);

        super.onDestroy();
    }
}
