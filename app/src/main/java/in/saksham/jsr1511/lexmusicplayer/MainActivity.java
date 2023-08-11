package in.saksham.jsr1511.lexmusicplayer;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.ColorUtils;
import androidx.palette.graphics.Palette;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import android.Manifest;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.MediaStore;
import android.speech.RecognizerIntent;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.LinearInterpolator;
import android.view.animation.OvershootInterpolator;
import android.view.animation.RotateAnimation;
import android.widget.LinearLayout;
import android.widget.SearchView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.chibde.visualizer.BarVisualizer;
import com.google.android.exoplayer2.ExoPlayer;
import com.google.android.exoplayer2.MediaItem;
import com.google.android.exoplayer2.Player;
import com.google.android.material.slider.Slider;
import com.jgabrielfreitas.core.BlurImageView;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import de.hdodenhof.circleimageview.CircleImageView;
import jp.wasabeef.recyclerview.adapters.ScaleInAnimationAdapter;

public class MainActivity extends AppCompatActivity {
    private static final int RESULT_SPEECH = 1;
    RecyclerView recyclerView;
    SongAdapter songAdapter;
    List<Song> allSongs=new ArrayList<>();
    ActivityResultLauncher<String> storagePermissionLauncher;
    final String permission=Manifest.permission.READ_EXTERNAL_STORAGE;
    ExoPlayer player;
    ActivityResultLauncher <String> recordAudioPermissionLauncher;
    final String recordAudioPermission = Manifest.permission.RECORD_AUDIO;
    String keeper;
    ConstraintLayout playerView;
    TextView playerCloseBtn;
    TextView songNameView,skipPreviousBtn,skipNextBtn,playPauseBtn,repeatModeBtn,voiceBtn;
    TextView homeSongNameView,homeSkipPreviousBtn,homePlayPauseBtn,homeSkipNextBtn;
    ConstraintLayout homeControlWrapper,headWrapper,artworkWrapper,seekbarWrapper,controlWrapper,audioVisualizerWrapper;
    CircleImageView artworkView;
    SeekBar seekbar,volume;
    TextView progressView,durationView;
    BarVisualizer audioVisualizer;
    BlurImageView blurImageView;

    int defaultStatusColor;
    int repeatMode=1;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        //
        this.setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        //
        defaultStatusColor=getWindow().getStatusBarColor();
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor,199));
        Toolbar toolbar =findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        Objects.requireNonNull(getSupportActionBar()).setTitle(getResources().getString(R.string.app_name));
        recyclerView=findViewById(R.id.recyclerview);
        storagePermissionLauncher=registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{
            if(granted){
                fetchSongs();
            }
            else{
                userResponses();
            }
        });

        storagePermissionLauncher.launch(permission);
        recordAudioPermissionLauncher= registerForActivityResult(new ActivityResultContracts.RequestPermission(),granted->{
            if(granted && player.isPlaying()){
                activateAudioVisualizer();
            }else{
                userResponsesOnRecordAudioPerm();
            }
        });
        player= new ExoPlayer.Builder(this).build();
        playerView=findViewById(R.id.playerView);
        playerCloseBtn=findViewById(R.id.playerCloseBtn);
        songNameView=findViewById(R.id.songNameView);
        skipPreviousBtn=findViewById(R.id.skipPreviousBtn);
        skipNextBtn=findViewById(R.id.skipNextBtn);
        playPauseBtn=findViewById(R.id.playPauseBtn);
        repeatModeBtn=findViewById(R.id.repeatModeBtn);
        voiceBtn=findViewById(R.id.speechToText);
        homeSongNameView=findViewById(R.id.homeSongNameView);
        homeSkipPreviousBtn=findViewById(R.id.homeSkipPreviousBtn);
        homeSkipNextBtn=findViewById(R.id.homeSkipNextBtn);
        homePlayPauseBtn=findViewById(R.id.homePlayPauseBtn);
        homeControlWrapper=findViewById(R.id.homeControlWrapper);
        headWrapper=findViewById(R.id.headWrapper);
        artworkWrapper=findViewById(R.id.artworkWrapper);
        seekbarWrapper=findViewById(R.id.seekbarWrapper);
        controlWrapper=findViewById(R.id.controlWrapper);
        audioVisualizerWrapper = findViewById(R.id.audioVisualizerWrapper);
        artworkView=findViewById(R.id.artworkView);
        seekbar=findViewById(R.id.seekbar);
        volume=findViewById(R.id.volume);
        progressView=findViewById(R.id.progressView);
        durationView=findViewById(R.id.durationView);
        audioVisualizer=findViewById(R.id.visualizer);
        blurImageView=findViewById(R.id.blurImageView);





        playerControls();

    }

    @Override
    public void onBackPressed() {
        if (playerView.getVisibility() == View.VISIBLE) {
            exitPlayerView();
        }
        else {
            super.onBackPressed();
        }
    }


    private void playerControls() {
//        float volume = player.getVolume();
//        int progress = (int) (volume * 100);

        songNameView.setSelected(true);
        homeSongNameView.setSelected(true);
        playerCloseBtn.setOnClickListener(view -> exitPlayerView());
        homeControlWrapper.setOnClickListener(view -> showPlayerView());
        player.addListener(new Player.Listener() {
            @Override
            public void onMediaItemTransition(@Nullable MediaItem mediaItem, int reason) {
                Player.Listener.super.onMediaItemTransition(mediaItem, reason);
                assert mediaItem != null;
                songNameView.setText(mediaItem.mediaMetadata.title);
                homeSongNameView.setText(mediaItem.mediaMetadata.title);
                progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                seekbar.setProgress((int) player.getCurrentPosition());
                seekbar.setMax((int) player.getCurrentPosition());
                durationView.setText(getReadableTime((int) player.getDuration()));
                playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
                homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
                showCurrentArtwork();
                updatePlayerPositionProgress();
                artworkView.setAnimation(loadRotation());
                activateAudioVisualizer();
                updatePlayerColors();
                if(!player.isPlaying()){
                    player.play();
                }
            }
            @Override
            public void onPlaybackStateChanged(int playbackState){
                Player.Listener.super.onPlaybackStateChanged(playbackState);
                if(playbackState==ExoPlayer.STATE_READY){
                    songNameView.setText(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.title);
                    homeSongNameView.setText(player.getCurrentMediaItem().mediaMetadata.title);
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    durationView.setText(getReadableTime((int) player.getDuration()));
                    seekbar.setMax((int) player.getDuration());
                    seekbar.setProgress((int) player.getCurrentPosition());
                    volume.setMax(100);
                    volume.setProgress((int)incDecVol());
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
                    showCurrentArtwork();
                    updatePlayerPositionProgress();
                    artworkView.setAnimation(loadRotation());
                    activateAudioVisualizer();
                    updatePlayerColors();
                }
                else{
                    playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
                    homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
                }
            }
        });
        //skip to next track
        skipNextBtn.setOnClickListener(view -> skipToNextSong());
        homeSkipNextBtn.setOnClickListener(view -> skipToNextSong());

        //skip to previous track
        skipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());
        homeSkipPreviousBtn.setOnClickListener(view -> skipToPreviousSong());

        //play or pause the player
        playPauseBtn.setOnClickListener(view -> playOrPausePlayer());
        homePlayPauseBtn.setOnClickListener(view ->playOrPausePlayer());



        //seekbar listener
         seekbar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progressValue=0;
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                progressValue=seekBar.getProgress();
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if(player.getPlaybackState()==ExoPlayer.STATE_READY){
                    seekBar.setProgress(progressValue);
                    progressView.setText(getReadableTime(progressValue));
                    player.seekTo(progressValue);
                }
            }
        });
//        incDecVol();
        repeatModeBtn.setOnClickListener(view ->{
            if(repeatMode ==1){
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ONE);
                repeatMode=2;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat_one,0,0,0);
            }
            else if(repeatMode==2){

                player.setShuffleModeEnabled(true);
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                repeatMode=3;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_shuffle,0,0,0);
            }
            else if(repeatMode==3){
                player.setRepeatMode(ExoPlayer.REPEAT_MODE_ALL);
                player.setShuffleModeEnabled(false);
                repeatMode=1;
                repeatModeBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_repeat,0,0,0);
            }
            updatePlayerColors();
        });
        voiceBtn.setOnClickListener(view->{
            Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
            intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, "en-US");
            try {
                startActivityForResult(intent, RESULT_SPEECH);

            } catch (ActivityNotFoundException e) {
                Toast.makeText(getApplicationContext(),"Your device dosen't support Speech to Text",Toast.LENGTH_LONG).show();
                e.printStackTrace();
            }
        });

    }
    private float incDecVol(){
       float volume1=50;
        volume.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float volume1 = (float) progress / 100.0f;
                player.setVolume(volume1);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {

            }
        });
      return volume1;
    }


    private void playOrPausePlayer() {
        if(player.isPlaying()){
            artworkView.clearAnimation();
            player.pause();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);

        }
        else{
            artworkView.clearAnimation();
            artworkView.setAnimation(loadRotation());
            player.play();
            playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
            homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);

        }
        updatePlayerColors();
    }
    private void playPlayer(){
        player.play();
        playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause_outline,0,0,0);
        homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_pause,0,0,0);
        artworkView.setAnimation(loadRotation());
        updatePlayerColors();
    }
    private void pausePlayer(){
        player.pause();
        playPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play_outline,0,0,0);
        homePlayPauseBtn.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_play,0,0,0);
        updatePlayerColors();
    }

    private void skipToPreviousSong() {
        if(player.hasPreviousMediaItem()){
            player.seekToPrevious();
        }
    }
    private void skipToNextSong() {
        if(player.hasNextMediaItem()){
            player.seekToNext();
        }
    }

    private Animation loadRotation() {
        RotateAnimation rotateAnimation=new RotateAnimation(0,360,Animation.RELATIVE_TO_SELF,0.5f,Animation.RELATIVE_TO_SELF,0.5f);
        rotateAnimation.setInterpolator(new LinearInterpolator());
        rotateAnimation.setDuration(10000);
        rotateAnimation.setRepeatCount(Animation.INFINITE);
        return rotateAnimation;

    }

    private void updatePlayerPositionProgress() {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                if (player.isPlaying()) {
                    progressView.setText(getReadableTime((int) player.getCurrentPosition()));
                    seekbar.setProgress((int) player.getCurrentPosition());
                }
                updatePlayerPositionProgress();
            }
        }, 1000);
    }

    private void showCurrentArtwork() {
        artworkView.setImageURI(Objects.requireNonNull(player.getCurrentMediaItem()).mediaMetadata.artworkUri);
        if(artworkView.getDrawable()==null){
            artworkView.setImageResource(R.drawable.default_artwork);
        }
    }

    String getReadableTime(int duration) {
        String time;
        int hrs = duration/(1000*60*60);
        int min = (duration%(1000*60*60))/(1000*60);
        int secs = (((duration%(1000*60*60))%(1000*60*60))%(1000*60))/1000;
        if(hrs<1){
            time=min+":"+secs;
        }
        else{
            time=hrs+":"+min+":"+secs;
        }
        return time;
    }

    private void updatePlayerColors() {
        if(playerView.getVisibility()==View.GONE)
            return ;
        BitmapDrawable bitmapDrawable = (BitmapDrawable) artworkView.getDrawable();
        if(bitmapDrawable ==null){
            bitmapDrawable=(BitmapDrawable) ContextCompat.getDrawable(this,R.drawable.default_artwork);
        }
        assert bitmapDrawable != null;
        Bitmap bmp= bitmapDrawable.getBitmap();
        blurImageView.setImageBitmap(bmp);
        blurImageView.setBlur(4);
        Palette.from(bmp).generate(palette -> {
            if(palette!=null){
                Palette.Swatch swatch=palette.getDarkVibrantSwatch();
                if(swatch ==null){
                    swatch =palette.getMutedSwatch();
                    if(swatch ==null){
                        swatch = palette.getDominantSwatch();
                    }
                }
                assert swatch != null;
                int titleTextColor= swatch.getTitleTextColor();
                int bodyTextColor=swatch.getBodyTextColor();
                int rgbColor= swatch.getRgb();
                getWindow().setStatusBarColor(rgbColor);
                getWindow().setNavigationBarColor(rgbColor);
                songNameView.setTextColor(titleTextColor);
                playerCloseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                progressView.setTextColor(bodyTextColor);
                durationView.setTextColor(bodyTextColor);
                repeatModeBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipPreviousBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                skipNextBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
                playPauseBtn.getCompoundDrawables()[0].setTint(titleTextColor);
                voiceBtn.getCompoundDrawables()[0].setTint(bodyTextColor);
            }
        });
    }
    private void showPlayerView() {
        playerView.setVisibility(View.VISIBLE);
        updatePlayerColors();
    }
    private void exitPlayerView() {
        playerView.setVisibility(View.GONE);
        getWindow().setStatusBarColor(defaultStatusColor);
        getWindow().setNavigationBarColor(ColorUtils.setAlphaComponent(defaultStatusColor,199));
    }

    private void userResponsesOnRecordAudioPerm() {
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(recordAudioPermission)){
                new AlertDialog.Builder(this)
                        .setTitle("Requesting to show Audio Visualizer")
                        .setMessage("Allow this app to display audio visualizer when music is playing ")
                        .setPositiveButton("allow", (dialogInterface, i) -> recordAudioPermissionLauncher.launch(recordAudioPermission))
                        .setNegativeButton("No", (dialogInterface, i) -> {
                            Toast.makeText(getApplicationContext(),"you denied to show the audio visualizer",Toast.LENGTH_SHORT).show();
                            dialogInterface.dismiss();
                        })
                        .show();
            }
        }
    }

    private void activateAudioVisualizer() {
        if(ContextCompat.checkSelfPermission(this,recordAudioPermission)!= PackageManager.PERMISSION_GRANTED){
            return;
        }
        audioVisualizer.setColor(ContextCompat.getColor(this,R.color.secondary_color));
        audioVisualizer.setDensity(120);
        audioVisualizer.setPlayer(player.getAudioSessionId());
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();
        if(player.isPlaying()){
            player.stop();
        }
        player.release();
    }

    private void userResponses() {
        if(ContextCompat.checkSelfPermission(this,permission)== PackageManager.PERMISSION_GRANTED){
            fetchSongs();
        }
        else if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.M){
            if(shouldShowRequestPermissionRationale(permission)){
                new AlertDialog.Builder(this).setTitle("Requesting Permission").setMessage("Allow us to fetch songs on your device")
                        .setPositiveButton("Allow", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                storagePermissionLauncher.launch(permission);
                            }
                        })
                        .setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int which) {
                                Toast.makeText(getApplicationContext(), "You denied us to show songs", Toast.LENGTH_SHORT).show();
                                dialogInterface.dismiss();
                            }
                        })
                        .show();


            }
            else{
                Toast.makeText(getApplicationContext(), "You denied us to show songs", Toast.LENGTH_SHORT).show();
            }

        }
        else{
            Toast.makeText(this, "You canceled to show songs", Toast.LENGTH_SHORT).show();
        }
    }

    private void fetchSongs() {
        List<Song> songs=new ArrayList<>();
        Uri mediaStoreUri;
        if(Build.VERSION.SDK_INT>=Build.VERSION_CODES.Q){
            mediaStoreUri = MediaStore.Audio.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);

        }
        else{
            mediaStoreUri= MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        }

        String []projection=new String[]{
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.DISPLAY_NAME,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.SIZE,
                MediaStore.Audio.Media.ALBUM_ID
        };
        String sortOrder=  MediaStore.Audio.Media.DATE_ADDED+ " DESC";
        try(Cursor cursor= getContentResolver().query(mediaStoreUri,projection,null,null,sortOrder)){
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DISPLAY_NAME);
            int durationColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.SIZE);
            int albumColumn = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID);

            while(cursor.moveToNext()){
                long id= cursor.getLong((idColumn));
                String name= cursor.getString(nameColumn);
                int duration= cursor.getInt(durationColumn);
                int size =cursor.getInt(sizeColumn);
                long albumId=cursor.getLong(albumColumn);

                Uri uri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,id);
                Uri albumArtworkUri =ContentUris.withAppendedId(Uri.parse("content://media/external/audio/albumart"),albumId); //instead of albumart use album
                name= name.substring(0,name.lastIndexOf("."));
                Song song = new Song(name,uri,albumArtworkUri,size,duration);
                songs.add(song);
            }
            showSongs(songs);
        }
    }

    private void showSongs(List<Song> songs) {
        if(songs.size()==0){
            Toast.makeText(this,"No Songs",Toast.LENGTH_SHORT).show();
        }
        allSongs.clear();
        allSongs.addAll(songs);
//        String title=getResources().getString(R.string.app_name)+ " - " +songs.size();
//        Objects.requireNonNull(getSupportActionBar()).setTitle(title);
        LinearLayoutManager layoutManager=new LinearLayoutManager(this);
        recyclerView.setLayoutManager(layoutManager);
        songAdapter = new SongAdapter(this,songs,player,playerView);
        ScaleInAnimationAdapter scaleInAnimationAdapter=new ScaleInAnimationAdapter(songAdapter);
        scaleInAnimationAdapter.setDuration(1500);
        scaleInAnimationAdapter.setInterpolator(new OvershootInterpolator());
        scaleInAnimationAdapter.setFirstOnly(false);
        recyclerView.setAdapter(scaleInAnimationAdapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.search_btn,menu);
        MenuItem menuItem=menu.findItem(R.id.searchBtn);
        SearchView searchView= (SearchView) menuItem.getActionView();
        searchView.setQueryHint("Type the song name");
        SearchSong(searchView);
        return super.onCreateOptionsMenu(menu);

    }
    private void SearchSong(SearchView searchView) {

        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                filterSongs(newText.toLowerCase());
                return true;
            }
        });
    }
    private void filterSongs(String query) {
        List<Song> filteredList=new ArrayList<>();
        if(allSongs.size()>0){
            for(Song song:allSongs){
                if(song.getTitle().toLowerCase().contains(query)){
                    filteredList.add(song);
                }
            }
            if(songAdapter!=null){
                songAdapter.filterSongs(filteredList);
            }
        }

    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        switch(requestCode){
            case RESULT_SPEECH:
                if(resultCode==RESULT_OK && data !=null){
                    ArrayList <String> text =data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                    keeper=text.get(0);
                    if(keeper.equalsIgnoreCase("pause the song")||keeper.equalsIgnoreCase("pause")||keeper.equalsIgnoreCase("gaana roko")||keeper.equalsIgnoreCase("gana roko")||keeper.equalsIgnoreCase("gana ruko")){
                        pausePlayer();
                    }
                    else if(keeper.equalsIgnoreCase("play the song")||keeper.equalsIgnoreCase("play")||keeper.equalsIgnoreCase("gana chalao")||keeper.equalsIgnoreCase("gana chala")
                            || keeper.equalsIgnoreCase("gaana chalao")){
                        playPlayer();
                    }
                    else if(keeper.equalsIgnoreCase("play the next song")|| keeper.equalsIgnoreCase("next song")||keeper.equalsIgnoreCase("agla gana")){
                        skipToNextSong();
                    }
                    else if(keeper.equalsIgnoreCase("play the previous song")|| keeper.equalsIgnoreCase("previous song")||keeper.equalsIgnoreCase("pichhla gana")){
                        skipToNextSong();
                    }

                }
                break;
        }
    }

}