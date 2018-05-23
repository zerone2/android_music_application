package comzerone1stsimsim.github.hw4;

import java.io.Serializable;

/**
 * Created by yicho on 2017-12-21.
 */

public class Music implements Serializable{
    private String albumId;
    private String title;
    private String artist;
    private String displayName;

    public Music(){}

    public Music(String albumId, String title, String artist){
        this.albumId = albumId;
        this.title = title;
        this.artist = artist;
    }

    public String getAlbumId() { return albumId;}
    public void setAlbumId(String albumId) {this.albumId = albumId;}

    public String getTitle() {return title;}
    public void setTitle(String title) {this.title = title;}

    public String getArtist() {return artist;}
    public void setArtist(String artist) {this.artist = artist;}

    public String getDisplayName() {return displayName;}
    public void setDisplayName(String displayName) {this.displayName = displayName;}
}
