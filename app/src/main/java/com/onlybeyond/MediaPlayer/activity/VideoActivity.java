package com.onlybeyond.MediaPlayer.activity;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Configuration;
import android.graphics.PixelFormat;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.SeekBar;
import android.widget.TextView;
import com.onlybeyond.MediaPlayer.R;
import com.onlybeyond.MediaPlayer.utils.UIUtils;
import com.onlybeyond.MediaPlayer.widget.LoadingView;

import java.io.IOException;

/**
 * Created by only on 15/12/7.
 */
public class VideoActivity extends Activity implements View.OnClickListener {

    private MediaPlayer mediaPlayer;
    private SurfaceView svVideo;
    private int position;
    private CheckBox cbBefore;
    private CheckBox cbPlay;
    private CheckBox cbAfter;
    private TextView tvPlayTime;
    private TextView tvLongTime;
    private SeekBar sbPlayTime;
    private boolean isStop;
    private boolean isBefore;                      //是否后退
    private boolean isAfter;              //是否前进
    private boolean isNetBreak; //似乎断过网

    //用于更新时间
    private Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            int currentPosition = mediaPlayer.getCurrentPosition();
            switch (msg.what) {

                case 1:
                    //正常更新时间
                    if (isBefore || isAfter) {
                        //恢复前进或后退暂停的播放，
                        isBefore = false;
                        isAfter = false;
                        mediaPlayer.start();
                    }
                    if (mediaPlayer.isPlaying()) {
                        int i = mediaPlayer.getCurrentPosition();
                        acumenttime();
                        sbPlayTime.setProgress(i);
                        i /= 1000;
                        int minute = i / 60;
                        int hour = minute / 60;
                        int second = i % 60;
                        minute %= 60;
                        tvPlayTime.setText(String.format("%02d:%02d:%02d", hour,
                                minute, second));
                        Log.d("time", "--time" + i + "hour" + hour + "minute" + minute + "second" + second);
//
                    } else {
                        //buffer缓冲的接口再start 状态才会调用
                        if (!isStop) {
                            mediaPlayer.start();
                        }
                        lvLoading.setVisibility(View.VISIBLE);
                    }
                    sendEmptyMessage(1);
                    break;
                case 2:
                    //快进
                    isStop = false;
                    if (currentPosition + 5000 < mediaPlayer.getDuration()) {
                        mediaPlayer.seekTo(currentPosition + 5000);
                        Log.d("after", "---buffer percent" + bufferPercent);
                        isAfter = true;
                        mediaPlayer.pause();

                    }
                    break;
                case 3:
                    //快退
                    isStop = false;
                    if (currentPosition - 13000 > 0) {
                        mediaPlayer.seekTo(currentPosition - 13000);
                    } else {
                        mediaPlayer.seekTo(0);
                    }
                    //暂停播放，避免跳帧
                    isBefore = true;
                    mediaPlayer.pause();

                    Log.d("before", "---before" + (currentPosition - 5000));
            }
        }
    };
    private int bufferPercent;
    private LoadingView lvLoading;
    private NetworkChangeReceiver mReceiver;

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        initScreen();
    }

    /***
     * 根据视频的横款比设置显示的视频
     */
    private void initScreen() {
        if (svVideo != null && mediaPlayer != null) {
            ViewGroup.LayoutParams layoutParams = svVideo.getLayoutParams();
            int screenWidth = UIUtils.getScreenWidth(VideoActivity.this);
            int screenHeight = UIUtils.getScreenHeight(VideoActivity.this);
            double proportionOne = screenWidth / (double) screenHeight;
            double proportionTwo = mediaPlayer.getVideoWidth() / (double) mediaPlayer.getVideoHeight();
            Log.d("proportion", "--- proportion one" + proportionOne + "---proportion two" + proportionTwo);
            if (Math.abs(proportionTwo - proportionOne) > 0.3) {
                if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    //land
                    layoutParams.width = (int) (screenHeight * ((double) mediaPlayer.getVideoWidth() / mediaPlayer.getVideoHeight()));
                    layoutParams.height = screenHeight;
                    Log.d("screen", "---land" + "width" + layoutParams.width + "height" + layoutParams.height);

                } else if (this.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                    //port
                    layoutParams.height = (int) (screenWidth * ((double) mediaPlayer.getVideoHeight() / mediaPlayer.getVideoWidth()));
                    layoutParams.width = screenWidth;
                    Log.d("screen", "---port" + "width" + layoutParams.width + "height" + layoutParams.height);
                }
            } else {
                //但视频横宽比和屏幕横宽比相差小于0.3时设置成全屏
                layoutParams.width = screenWidth;
                layoutParams.height = screenHeight;
            }
            svVideo.setLayoutParams(layoutParams);


        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        mReceiver = new NetworkChangeReceiver();
        registerReceiver(mReceiver, filter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null) {
            //再次
            mediaPlayer.release();
            initMedia();
            Log.d("position", "---position" + position);
            mediaPlayer.seekTo(position);
        } else {
            initMedia();
            mediaPlayer.seekTo(position);
        }
    }

    @Override
    protected void onPause() {
        mediaPlayer.pause();
        //记录当前位置
        position = mediaPlayer.getCurrentPosition();
        super.onPause();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d("onCreate", "---onCreate");
        setContentView(R.layout.activity_video);
        svVideo = (SurfaceView) findViewById(R.id.sv_video);
        cbBefore = (CheckBox) findViewById(R.id.cb_before);
        cbPlay = (CheckBox) findViewById(R.id.cb_play);
        cbAfter = (CheckBox) findViewById(R.id.cb_after);
        tvPlayTime = (TextView) findViewById(R.id.tv_play_time);
        tvLongTime = (TextView) findViewById(R.id.tv_long);
        sbPlayTime = (SeekBar) findViewById(R.id.sb_play_time);
        lvLoading = (LoadingView) findViewById(R.id.loading);
        sbPlayTime.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    //滑动改变
                    mediaPlayer.seekTo(progress);
                    lvLoading.setVisibility(View.VISIBLE);
                }

            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


        cbBefore.setOnClickListener(this);
        cbPlay.setOnClickListener(this);
        cbAfter.setOnClickListener(this);


        // 设置播放时打开屏幕。
        svVideo.getHolder().setKeepScreenOn(true);
        // 设置surfaceview自己不管理的缓冲区。
        svVideo.getHolder()
                .setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);

        svVideo.getHolder().setFormat(PixelFormat.TRANSLUCENT);
        svVideo.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {

            }

            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                Log.d("surface", "---create");
                //需要在创建时赋值，每回界面创建时都会调用
                mediaPlayer.setDisplay(svVideo.getHolder());

            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format,
                                       int width, int height) {
                // TODO Auto-generated method stub

            }
        });
    }

    public void initMedia() {
        mediaPlayer = new MediaPlayer();
        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);


        try {
            String url = "http://medical.7xlizb.com2.z0.glb.qiniucdn.com/cfa15343-cb1c-4d1e-aa2a-43caf86cfa50";
            mediaPlayer.setDataSource(url);
            //使用同步的网速慢时会导致ui无响应
            mediaPlayer.prepareAsync();
            mediaPlayer.start();
            svVideo.setVisibility(View.VISIBLE);
            gotimepoint();
            Log.d("play 123", "---play");

        } catch (IllegalArgumentException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (SecurityException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IllegalStateException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        mediaPlayer.setOnSeekCompleteListener(new MediaPlayer.OnSeekCompleteListener() {
            @Override
            public void onSeekComplete(MediaPlayer mp) {
                lvLoading.setVisibility(View.GONE);
                Log.d("seek", "---seek");
            }
        });
        mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                initScreen();
                if (position != 0) {
                    mediaPlayer.seekTo(position);
                }
                Log.d("prepare", "----prepare position" + position);

            }
        });
        mediaPlayer.setOnErrorListener(new MediaPlayer.OnErrorListener() {
            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                        //服务器错误
                        Log.d("media", "----server error");
                        break;
                    case MediaPlayer.MEDIA_ERROR_IO:
                        //本地文件或者网络错误
                        Log.d("media", "----network error");
                        break;
                    case MediaPlayer.MEDIA_ERROR_TIMED_OUT:
                        //超时错误
                        Log.d("media", "----time out");
                        break;
                    case MediaPlayer.MEDIA_ERROR_MALFORMED:
                        //比特流读写错误
                        Log.d("media", "----write error ");
                        break;

                }
                return true;
            }
        });
        mediaPlayer.setOnInfoListener(new MediaPlayer.OnInfoListener() {
            @Override
            public boolean onInfo(MediaPlayer mp, int what, int extra) {
                switch (what) {
                    case MediaPlayer.MEDIA_INFO_BUFFERING_END:
                        //缓冲结束
                        Log.d("media", "----buffer end");
                        lvLoading.setVisibility(View.GONE);
                        break;
                    case MediaPlayer.MEDIA_INFO_BUFFERING_START:
                        Log.d("media", "----buffer start");
                        lvLoading.setVisibility(View.VISIBLE);
                        //缓冲开始
                        break;
                    case MediaPlayer.MEDIA_INFO_SUBTITLE_TIMED_OUT:
                        //读取字幕超时
                        break;
                    case MediaPlayer.MEDIA_INFO_BAD_INTERLEAVING:
                        //错误交叉
                        Log.d("media", "----internet bad");
                        break;
                    case MediaPlayer.MEDIA_INFO_VIDEO_RENDERING_START:
                        //开始渲染第一帧
                        Log.d("media", "----start");
                        break;


                }
                return true;
            }
        });
        mediaPlayer.setOnBufferingUpdateListener(new MediaPlayer.OnBufferingUpdateListener() {
            @Override
            public void onBufferingUpdate(MediaPlayer mp, int percent) {
                //视频暂停播放时不会回调断网也不会调用
                int currentPosition = mp.getCurrentPosition();
                int duration = mp.getDuration();
                bufferPercent = percent;
                if (canPlay()) {
                    if (!isStop) {
                        //没有按下暂停
                        if (!mediaPlayer.isPlaying()) {
                            mediaPlayer.start();
                        }
                        lvLoading.setVisibility(View.GONE);
                        if(svVideo.getVisibility()==View.GONE) {
                            svVideo.setVisibility(View.VISIBLE);
                        }
                    }
                } else {
                    //记录位置
                    position=mediaPlayer.getCurrentPosition();
                    svVideo.setVisibility(View.GONE);
                    lvLoading.setLoadingText("网络不给力～");
                    lvLoading.setVisibility(View.VISIBLE);
                }
                Log.d("buffer progress", "---buffer" + percent + "duration" + duration + "current" + currentPosition);
            }
        });


    }

    public void acumenttime() {
        int i = mediaPlayer.getDuration();
        sbPlayTime.setMax(i);
        i /= 1000;
        int minute = i / 60;
        int hour = minute / 60;
        int second = i % 60;
        minute %= 60;
        tvLongTime.setText(String.format("%02d:%02d:%02d", hour, minute,
                second));

    }

    public void gotimepoint() {
        //播放
        Message message = handler.obtainMessage();
        message.what = 1;
        handler.sendMessage(message);

    }

    public void gotimepoint2() {
        //快进
        Message message = handler.obtainMessage();
        message.what = 2;
        handler.sendMessage(message);

    }

    public void gotimepoint3() {
        //快退
        Message message = handler.obtainMessage();
        message.what = 3;
        handler.sendMessage(message);

    }

    @Override
    public void onClick(View v) {
        int id = v.getId();
        if (id == R.id.cb_after) {
            gotimepoint2();
        } else if (id == R.id.cb_play) {
            if (mediaPlayer.isPlaying()) {
                isStop = true;
                position = mediaPlayer.getCurrentPosition();
                mediaPlayer.pause();
            } else {
                isStop = false;
                mediaPlayer.seekTo(position);
                lvLoading.setVisibility(View.VISIBLE);
                mediaPlayer.start();
            }

        } else if (id == R.id.cb_before) {
            gotimepoint3();
        }
    }

    public boolean canPlay() {
        boolean ret = false;
        if (mediaPlayer != null) {
            if (mediaPlayer.getVideoWidth() > 0 && mediaPlayer.getVideoHeight() > 0) {
                int bufferValue = mediaPlayer.getDuration() * bufferPercent / 100 - mediaPlayer.getCurrentPosition();
                if (bufferValue > 500) {
                    ret = true;
                }else {
                    //当快不能播放时记录位置
                    position=mediaPlayer.getCurrentPosition();
                }
                Log.d("media", "---buffer value" + bufferValue + "---buffer percent" + bufferPercent);

            }
        }
        Log.d("canPlay", "---is can play" + "ret:" + ret);

        return ret;
    }

    class NetworkChangeReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction() != null &&
                    intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
                ConnectivityManager connectManager =
                        (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
                NetworkInfo networkInfo = connectManager.getActiveNetworkInfo();
                if (networkInfo != null) {
                    //恢复网络时重新初始化视频
                    if (isNetBreak==true) {
                        if(mediaPlayer!=null){
                            mediaPlayer.release();
                        }
                        initMedia();
                        //需要重新绘制，否则会导致声音画面不同步
                        if(svVideo.getVisibility()==View.VISIBLE){
                            svVideo.setVisibility(View.GONE);
                        }
                        svVideo.setVisibility(View.VISIBLE);
                        isNetBreak=false;
                    }
                } else {
                    //无网络连接
                    if (mediaPlayer != null) {
                          lvLoading.setLoadingText("网络不给力～");
                        isNetBreak=true;
                    }
                }
            }

        }
    }

}
