package comzerone1stsimsim.github.hw4;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.ListView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity implements MusicAdapter.ListClickListener{

    Context mContext;
    ArrayList<Music> items;
    final int MY_CHECK = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getMusic();
        chkPermission();

        ListView mListView = (ListView)findViewById(R.id.listView);
        MusicAdapter adapter;
        mContext = this;

        Intent serviceIntent = new Intent("comzerone1stsimsim.github.hw4.MusicService");
        serviceIntent.setPackage("comzerone1stsimsim.github.hw4");
        startService(serviceIntent);

        adapter = new MusicAdapter(mContext, R.layout.listview_layout, items, this);
        mListView.setAdapter(adapter);

    }

    public void chkPermission(){
        if(checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED
                || checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED)
        {
            if(shouldShowRequestPermissionRationale(Manifest.permission.WRITE_EXTERNAL_STORAGE)){
                Toast.makeText(this, "Read / Write External Storage", Toast.LENGTH_SHORT).show();
            }
        }
        requestPermissions(new String[] { Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE }, MY_CHECK);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults){
        switch(requestCode){
            case MY_CHECK:
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED && grantResults[1] == PackageManager.PERMISSION_GRANTED){

                } else {
                    Log.d("MUSIC", "PERMISSION DENIED");
                }
                break;
        }
    }

    public void getMusic(){
        Log.d("getMusic", "in1");
        items = new ArrayList<>();
        //음악 정보 가져오기.
        String[] musicInfo = {
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.DISPLAY_NAME
        };
        Cursor cursor = getContentResolver().query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, musicInfo, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
            Music music = null;
            while (cursor.moveToNext()) {
                music = new Music();
                if(!(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE))).contains("Hangouts ")) {
                    music.setAlbumId(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ALBUM_ID)));
                    music.setTitle(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.TITLE)));
                    music.setArtist(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.ARTIST)));
                    music.setDisplayName(cursor.getString(cursor.getColumnIndex(MediaStore.Audio.Media.DISPLAY_NAME)));

                    items.add(music);
                }
            }
            //path += music.getDisplayName();             //임시. 노래재생용.
        }

        cursor.close();
    }

    public void onListBtnClick(int position){
    }

}