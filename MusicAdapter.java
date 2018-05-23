package comzerone1stsimsim.github.hw4;

import android.content.Context;
import android.content.Intent;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import java.io.File;
import java.util.ArrayList;

/**
 * Created by yicho on 2017-12-21.
 */

public class MusicAdapter extends ArrayAdapter implements View.OnClickListener{

    ArrayList<Music> items;
    LayoutInflater inflater;
    int resourceId;
    private ListClickListener listClickListener;
    MediaPlayer mPlayer;
    String path;

    public interface ListClickListener{
        void onListBtnClick(int position);
    }

    MusicAdapter(Context context, int resource, ArrayList<Music> items, ListClickListener clickListener){
        super(context, resource, items);

        this.resourceId = resource;
        this.listClickListener = clickListener;
        this.items = items;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent){
        final int pos = position;
        final Context context = parent.getContext();
        mPlayer = new MediaPlayer();        //point**
        path = Environment.getExternalStorageDirectory().getPath() + File.separator;

        if(convertView == null){
            inflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(this.resourceId, parent, false);
        }

        final ImageView imageView = (ImageView) convertView.findViewById(R.id.imageView1);
        final TextView textTextView = (TextView) convertView.findViewById(R.id.textView1);

        final Music music = (Music) getItem(position);

        path += items.get(pos).getDisplayName();
        Uri uri = Uri.parse("content://media/external/audio/albumart/" + items.get(position).getAlbumId());
        imageView.setImageURI(uri);

        textTextView.setText(music.getTitle());

        convertView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, MusicPlayActivity.class);
                intent.putExtra("musics", items);
                intent.putExtra("position", pos);
                context.startActivity(intent);
            }
        });

        return convertView;
    }

    @Override
    public void onClick(View v) {
        if(this.listClickListener != null){
            this.listClickListener.onListBtnClick((int)v.getTag());
        }
    }
}
