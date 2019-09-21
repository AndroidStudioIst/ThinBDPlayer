package com.walixiwa.thin.bdplayer;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ActivityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.baidu.cloud.media.player.BDCloudMediaPlayer;
import com.baidu.cloud.media.player.IMediaPlayer;
import com.walixiwa.thin.bdplayer.widget.BDCloudVideoView;

import java.util.Timer;
import java.util.TimerTask;

import static android.view.View.GONE;
import static android.view.View.VISIBLE;
import static com.walixiwa.thin.bdplayer.widget.BDCloudVideoView.convertFileSize;

public class PlayerActivity extends AppCompatActivity implements IMediaPlayer.OnPreparedListener,
        IMediaPlayer.OnCompletionListener, IMediaPlayer.OnErrorListener,
        IMediaPlayer.OnInfoListener, IMediaPlayer.OnBufferingUpdateListener,
        BDCloudVideoView.OnPlayerStateListener {
    private String AK = "";
    private BDCloudVideoView mVV = null;
    private RelativeLayout mViewHolder = null;

    private SeekBar seekBar;
    boolean mbIsDragging = false;
    //是否用户手动
    private ImageView iv_play;
    private long currentPositionInMilliSeconds = 0L;
    private TextView positionView;
    private TextView durationView;
    private Timer positionTimer;
    private ProgressBar cachingProgressBar = null;
    private TextView cachingProgressHint = null;

    private int duration;
    private int currentPosition;
    private float clickX;
    private float clickY;
    private int action;
    private int currentVolume;
    private int maxVolume;
    private int pressedBrightness;
    private int currentMBritness;
    private int clickVolume;
    private float moveXY;
    private int currentMoveXY;
    private AudioManager audioManager;
    private int width;
    private int height;

    private TextView jindutishitext;

    private LinearLayout ctrl_bar;
    private LinearLayout header_bar;
    private ImageView back;
    private ImageView lock;
    private ImageView rotate;
    public boolean islocked = false;
    private TextView tv_title;
    private boolean locker = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_player);
        Intent intent = getIntent();
        initUI(intent.getStringExtra("title"), intent.getStringExtra("url"));
        hideBottomUIMenu();
    }

    /**
     * 初始化界面
     */
    private void initUI(String title, String url) {
        this.audioManager = ((AudioManager) getSystemService(AUDIO_SERVICE));
        if (audioManager != null) {
            int j = this.audioManager.getStreamMaxVolume(3);
            this.maxVolume = j;
            this.maxVolume *= 6;
        }
        float f = this.currentVolume * 6;
        try {
            int k = Settings.System.getInt(getContentResolver(), "screen_brightness");
            f = 1.0F * k / 255.0F;
        } catch (Settings.SettingNotFoundException localSettingNotFoundException) {
            localSettingNotFoundException.printStackTrace();
        }
        this.currentMBritness = ((int) (f * 100.0F));
        this.pressedBrightness = ((int) (f * 100.0F));

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        this.width = displayMetrics.widthPixels;
        this.height = displayMetrics.heightPixels;

        mViewHolder = findViewById(R.id.view_holder);
        BDCloudVideoView.setAK(AK);

        mVV = new BDCloudVideoView(this);
        mVV.setVideoPath(url);

        RelativeLayout.LayoutParams rllp = new RelativeLayout.LayoutParams(-1, -1);
        rllp.addRule(RelativeLayout.CENTER_IN_PARENT);
        mViewHolder.addView(mVV, rllp);

        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePostion(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                mbIsDragging = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mVV.getDuration() > 0) {
                    currentPositionInMilliSeconds = seekBar.getProgress();
                    if (mVV != null) {
                        mVV.seekTo(seekBar.getProgress());
                    }
                }
                mbIsDragging = false;
            }
        });
        jindutishitext = findViewById(R.id.tv_info);
        ctrl_bar = findViewById(R.id.ctrl_bar);
        header_bar = findViewById(R.id.header_bar);
        back = findViewById(R.id.v_back);
        lock = findViewById(R.id.v_player_lock);
        tv_title = findViewById(R.id.v_title);
        tv_title.setText(title);
        rotate = findViewById(R.id.v_rotate);
        iv_play = findViewById(R.id.iv_play);
        positionView = findViewById(R.id.tv_position);
        durationView = findViewById(R.id.tv_duration);
        seekBar = findViewById(R.id.seekbar);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updatePostion(progress);
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
                mbIsDragging = true;
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
                if (mVV.getDuration() > 0) {
                    // 仅非直播的视频支持拖动
                    currentPositionInMilliSeconds = seekBar.getProgress();
                    if (mVV != null) {
                        mVV.seekTo(seekBar.getProgress());
                    }
                }
                mbIsDragging = false;
            }
        });

        cachingProgressBar = findViewById(R.id.loading);
        cachingProgressHint = findViewById(R.id.loading_text);

        mVV.setOnCachingStateChangeListener(new BDCloudVideoView.OnCachingStateChangeListener() {
            @Override
            public void onCachingStateChange(boolean b) {
                findViewById(R.id.caching).setVisibility(b ? VISIBLE : GONE);
            }
        });
        iv_play.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mVV != null) {
                    if (mVV.isPlaying()) {
                        iv_play.setImageResource(R.drawable.v_play_arrow);
                        mVV.pause();
                    } else {
                        iv_play.setImageResource(R.drawable.v_play_pause);
                        mVV.start();
                    }
                }
            }
        });
        rotate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Configuration mConfiguration = getResources().getConfiguration();               /* 获取设置的配置信息 */
                int ori = mConfiguration.orientation;                           /* 获取屏幕方向 */
                if (ori == Configuration.ORIENTATION_LANDSCAPE) {
                    /* 横屏 */
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);            /* 强制为竖屏 */
                } else if (ori == Configuration.ORIENTATION_PORTRAIT) {
                    /* 竖屏 */
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE);    /* 强制为横屏 */
                }
            }
        });
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        lock.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!locker) {
                    locker = true;
                    hideCtrlBar();
                    lock.setImageResource(R.drawable.v_player_locked);
                } else {
                    lock.setImageResource(R.drawable.v_player_unlocked);
                    locker = false;
                    showBars();
                }
            }
        });
        /**
         * 注册listener
         */
        mVV.setOnPreparedListener(this);
        mVV.setOnCompletionListener(this);
        mVV.setOnErrorListener(this);
        mVV.setOnInfoListener(this);
        mVV.setOnBufferingUpdateListener(this);
        mVV.setOnPlayerStateListener(this);
        mVV.start();
    }

    @Override
    public void onBufferingUpdate(IMediaPlayer iMediaPlayer, int i) {
        seekBar.setSecondaryProgress(i * mVV.getDuration() / 100);
    }

    @Override
    public void onCompletion(IMediaPlayer iMediaPlayer) {

    }

    @Override
    public boolean onError(IMediaPlayer mp, int what, int extra) {
        String Msg;
        switch (what) {
            case BDCloudMediaPlayer.MEDIA_ERROR_UNKNOWN:
                Msg = "未知错误,可能该资源失效了哦";
                break;
            case BDCloudMediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Msg = "媒体服务器挂掉了";
                break;
            case BDCloudMediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Msg = "视频播放比较慢或视频本身有问题";
                break;
            case BDCloudMediaPlayer.MEDIA_ERROR_IO:
                Msg = "IO错误";
                break;
            case BDCloudMediaPlayer.MEDIA_ERROR_MALFORMED:
                Msg = "比特流不符合相关的编码标准和文件规范";
                break;
            case BDCloudMediaPlayer.MEDIA_ERROR_UNSUPPORTED:
                Msg = "暂不支持当前格式";
                break;
            case BDCloudMediaPlayer.MEDIA_ERROR_TIMED_OUT:
                Msg = "播放连接超时";
                break;
            case -10000:
                Msg = "资源连接失败,可能资源已失效";
                break;
            default:
                Msg = "未知错误:" + what;
                break;
        }
        Toast.makeText(this, Msg, Toast.LENGTH_LONG).show();
        return false;
    }

    @Override
    public boolean onInfo(IMediaPlayer iMediaPlayer, int i, int i1) {
        return false;
    }

    @Override
    public void onPrepared(IMediaPlayer iMediaPlayer) {

    }

    @Override
    public void onPlayerStateChanged(BDCloudVideoView.PlayerState nowState) {
        if (nowState == BDCloudVideoView.PlayerState.STATE_IDLE) {
            iv_play.setEnabled(true);
            iv_play.setImageResource(R.drawable.v_play_arrow);
            stopPositionTimer();
            //Toast.makeText(context, "播放器已闲置", Toast.LENGTH_LONG).show();
            seekBar.setEnabled(false);
            updatePostion(mVV == null ? 0 : mVV.getCurrentPosition());
            updateDuration(mVV == null ? 0 : mVV.getDuration());
        } else if (nowState == BDCloudVideoView.PlayerState.STATE_ERROR) {
            iv_play.setImageResource(R.drawable.v_play_arrow);
            iv_play.setEnabled(true);
            stopPositionTimer();
            //Toast.makeText(context, "播放异常终止", Toast.LENGTH_LONG).show();
            seekBar.setEnabled(false);
            updatePostion(mVV == null ? 0 : mVV.getCurrentPosition());
            updateDuration(mVV == null ? 0 : mVV.getDuration());
        } else if (nowState == BDCloudVideoView.PlayerState.STATE_PREPARED) {
            iv_play.setImageResource(R.drawable.v_play_pause);
            iv_play.setEnabled(true);
            startPositionTimer();
            updateDuration(mVV.getDuration());
            seekBar.setMax(mVV.getDuration());
            seekBar.setEnabled(true);
        } else if (nowState == BDCloudVideoView.PlayerState.STATE_PLAYING) {
            iv_play.setImageResource(R.drawable.v_play_pause);
            iv_play.setEnabled(true);
            startPositionTimer();
            seekBar.setMax(mVV.getDuration());
            seekBar.setEnabled(true);
        } else if (nowState == BDCloudVideoView.PlayerState.STATE_PLAYBACK_COMPLETED) {
            iv_play.setImageResource(R.drawable.v_play_arrow);
            iv_play.setEnabled(true);
            stopPositionTimer();
        } else if (nowState == BDCloudVideoView.PlayerState.STATE_PREPARING) {
            iv_play.setImageResource(R.drawable.v_play_pause);
            iv_play.setEnabled(false);
            startPositionTimer();
            seekBar.setEnabled(false);
        } else if (nowState == BDCloudVideoView.PlayerState.STATE_PAUSED) {
            iv_play.setImageResource(R.drawable.v_play_arrow);
            iv_play.setEnabled(true);
            stopPositionTimer();
        }
    }


    private void updateDuration(int milliSecond) {
        if (durationView != null) {
            durationView.setText(formatMilliSecond(milliSecond));
        }
    }

    private void updatePostion(int milliSecond) {
        if (positionView != null) {
            positionView.setText(formatMilliSecond(milliSecond));
        }
    }

    private String formatMilliSecond(int milliSecond) {
        int seconds = milliSecond / 1000;
        int hh = seconds / 3600;
        int mm = seconds % 3600 / 60;
        int ss = seconds % 60;
        String strTemp = null;
        if (0 != hh) {
            strTemp = String.format("%02d:%02d:%02d", hh, mm, ss);
        } else {
            strTemp = String.format("%02d:%02d", mm, ss);
        }
        return strTemp;
    }


    private Handler mainThreadHandler = new Handler(Looper.getMainLooper());

    private void startPositionTimer() {
        if (positionTimer != null) {
            positionTimer.cancel();
            positionTimer = null;
        }
        positionTimer = new Timer();
        positionTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mainThreadHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        if (mVV == null) {
                            return;
                        }
                        cachingProgressHint.setText(String.format("加载视频：%s/s", convertFileSize(mVV.getDownloadSpeed())));
                        onPositionUpdate();

                    }
                });
            }
        }, 0, 1000);
    }

    private void stopPositionTimer() {
        if (positionTimer != null) {
            positionTimer.cancel();
            positionTimer = null;
        }
    }

    public void onPositionUpdate() {
        if (mVV == null) {
            return;
        }
        long newPositionInMilliSeconds = mVV.getCurrentPosition();
        long previousPosition = currentPositionInMilliSeconds;
        if (newPositionInMilliSeconds > 0 && !mbIsDragging) {
            currentPositionInMilliSeconds = newPositionInMilliSeconds;
        }
        if (seekBar.getVisibility() != VISIBLE) {
            // 如果控制条不可见，则不设置进度
            return;
        }
        if (!mbIsDragging) {
            int durationInMilliSeconds = mVV.getDuration();
            if (durationInMilliSeconds > 0) {
                seekBar.setMax(durationInMilliSeconds);
                // 直播视频的duration为0，此时不设置进度
                if (previousPosition != newPositionInMilliSeconds) {
                    //DebugUtil.e("update", "set..."+newPositionInMilliSeconds);
                    seekBar.setProgress((int) newPositionInMilliSeconds);
                }
            }
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent motionEvent) {
        float x = motionEvent.getX();
        float y = motionEvent.getY();
        Log.e("motionEvent", x + "|" + y);
        float f;
        switch (motionEvent.getAction()) {
            case MotionEvent.ACTION_DOWN:
                this.duration = this.mVV.getDuration() / 1000;
                this.currentPosition = this.mVV.getCurrentPosition() / 1000;
                this.clickX = x;
                this.clickY = y;
                this.action = 1;
                this.currentVolume = this.audioManager.getStreamVolume(3);
                this.clickVolume = (this.currentVolume * 6);
                f = 1.0F;
                try {
                    int i = Settings.System.getInt(getContentResolver(), "screen_brightness");
                    f = 1.0F * i / 255.0F;
                } catch (Settings.SettingNotFoundException localSettingNotFoundException) {
                    localSettingNotFoundException.printStackTrace();
                }
                this.moveXY = y;
                break;
            case MotionEvent.ACTION_UP:
                new Handler().postDelayed(new Runnable() {
                    public void run() {
                        jindutishitext.setVisibility(GONE);
                        switch (action) {
                            case 2:
                                if (islocked) {
                                    return;
                                }
                                if (mVV.getDuration() > 5) {
                                    mVV.seekTo(currentMoveXY * 1000);
                                }
                                break;
                            case 3:
                                pressedBrightness = currentMBritness;
                                break;
                            case 4:
                                break;
                            default:
                                onClickEmptyArea();
                                break;
                        }
                    }
                }, 100L);
                break;
            case MotionEvent.ACTION_MOVE:
                if (islocked) {
                    return true;
                }
                f = Math.abs(x - this.clickX);
                float abs = Math.abs(y - this.clickY);
                if (this.action == 1) {
                    if (f > 50.0f && abs < 50.0f) {
                        this.action = 2;
                    }
                    if (f < 50.0f && abs > 50.0f && ((double) this.clickX) < ((double) this.width) * 0.25d) {
                        this.action = 3;
                    }
                    if (f < 50.0f && abs > 50.0f && ((double) this.clickX) > ((double) this.width) * 0.75d) {
                        this.action = 4;
                    }
                }
                switch (this.action) {
                    case 2:
                        this.currentMoveXY = (int) ((float) ((((double) (((x - this.clickX) / ((float) this.width)) * ((float) this.duration))) * 0.3d) + ((double) this.currentPosition)));
                        if (this.currentMoveXY < 0) {
                            this.currentMoveXY = 0;
                        }
                        if (this.currentMoveXY > this.duration) {
                            this.currentMoveXY = this.duration;
                        }
                        this.jindutishitext.setVisibility(VISIBLE);
                        this.jindutishitext.setText(updateTextViewWithTimeFormat(this.currentMoveXY) + "/" + updateTextViewWithTimeFormat(this.duration));
                        break;
                    case 3:
                        float f6 = (y - this.moveXY) * 100.0F / this.height;
                        this.currentMBritness = (this.pressedBrightness - (int) f6);
                        if (this.currentMBritness > 100) {
                            this.currentMBritness = 100;
                        }
                        if (this.currentMBritness < 7) {
                            this.currentMBritness = 7;
                        }
                        this.jindutishitext.setVisibility(VISIBLE);
                        int j = (this.currentMBritness - 7) * 100 / 93;
                        this.jindutishitext.setText("亮度：" + j + "%");

                        setBrightness(this.currentMBritness);
                        break;
                    case 4:
                        float f7 = (y - this.moveXY) * 100.0F / this.height;

                        this.currentVolume = (this.clickVolume - (int) f7);
                        if (this.currentVolume > this.maxVolume) {
                            this.currentVolume = this.maxVolume;
                        }
                        if (this.currentVolume < 0) {
                            this.currentVolume = 0;
                        }
                        this.jindutishitext.setVisibility(VISIBLE);
                        int k = this.currentVolume * 100 / this.maxVolume;
                        this.jindutishitext.setText("音量：" + k + "%");
                        int m = this.currentVolume / 6;
                        this.audioManager.setStreamVolume(3, m, 0);
                        break;
                    default:
                        break;
                }
                break;
        }
        return true;
    }

    private void setBrightness(int paramInt) {
        if (paramInt < 0) {
            paramInt = 0;
        }
        if (paramInt > 100) {
            paramInt = 100;
        }
        WindowManager.LayoutParams localLayoutParams = getWindow().getAttributes();
        localLayoutParams.screenBrightness = (1.0F * paramInt / 100.0F);
        getWindow().setAttributes(localLayoutParams);
        this.currentMBritness = paramInt;
    }

    private String updateTextViewWithTimeFormat(int i) {
        int i2 = (i % 3600) / 60;
        int i3 = i % 60;
        if (i / 3600 != 0) {
            return String.format("%02d:%02d:%02d", new Object[]{Integer.valueOf(i / 3600), Integer.valueOf(i2), Integer.valueOf(i3)});
        }
        return String.format("%02d:%02d", new Object[]{Integer.valueOf(i2), Integer.valueOf(i3)});
    }

    private void onClickEmptyArea() {
        if (locker) {
            if (lock.getVisibility() != VISIBLE) {
                lock.setVisibility(VISIBLE);
                Animation animation3 = AnimationUtils.loadAnimation(this, R.anim.anim_left_in);
                lock.startAnimation(animation3);
            } else {
                Animation animation3 = AnimationUtils.loadAnimation(this, R.anim.anim_left_out);
                lock.startAnimation(animation3);
                lock.setVisibility(GONE);
            }
            return;
        }
        if (header_bar.getVisibility() == GONE) {
            showBars();
        } else {
            hideBottomUIMenu();
            hideCtrlBar();
        }
    }

    private void showBars() {
        header_bar.setVisibility(VISIBLE);
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.anim_top_in);
        header_bar.startAnimation(animation);

        ctrl_bar.setVisibility(VISIBLE);
        Animation animation2 = AnimationUtils.loadAnimation(this, R.anim.anim_bottom_in);
        ctrl_bar.startAnimation(animation2);

        lock.setVisibility(VISIBLE);
        Animation animation3 = AnimationUtils.loadAnimation(this, R.anim.anim_left_in);
        lock.startAnimation(animation3);

        rotate.setVisibility(VISIBLE);
        rotate.startAnimation(animation2);
    }


    private void hideCtrlBar() {
        Animation animation = AnimationUtils.loadAnimation(this, R.anim.anim_top_out);
        header_bar.startAnimation(animation);
        header_bar.setVisibility(GONE);
        Animation animation2 = AnimationUtils.loadAnimation(this, R.anim.anim_bottom_out);
        ctrl_bar.startAnimation(animation2);
        ctrl_bar.setVisibility(GONE);
        Animation animation3 = AnimationUtils.loadAnimation(this, R.anim.anim_left_out);
        lock.startAnimation(animation3);
        lock.setVisibility(GONE);

        rotate.startAnimation(animation2);
        rotate.setVisibility(GONE);
    }


    /**
     * 隐藏虚拟按键，并且全屏
     */
    protected void hideBottomUIMenu() {
        Window window = getWindow();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                //布局位于状态栏下方
                View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION |
                //全屏
                View.SYSTEM_UI_FLAG_FULLSCREEN |
                //隐藏导航栏
                View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        if (Build.VERSION.SDK_INT >= 19) {
            uiOptions |= 0x00001000;
        } else {
            uiOptions |= View.SYSTEM_UI_FLAG_LOW_PROFILE;
        }
        window.getDecorView().setSystemUiVisibility(uiOptions);
    }

    protected void showBottomUIMenu() {
        //显示虚拟按键
        if (Build.VERSION.SDK_INT >= 19) {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_VISIBLE;
            decorView.setSystemUiVisibility(uiOptions);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
        this.width = displayMetrics.widthPixels;
        this.height = displayMetrics.heightPixels;
    }

    @Override
    protected void onPause() {
        super.onPause();

    }

    @Override
    protected void onResume() {
        super.onResume();

    }

    @Override
    protected void onRestart() {
        super.onRestart();
        if (mVV != null) {
            mVV.enterForeground();
        }
    }

    @Override
    protected void onStop() {
        if (mVV != null) {
            mVV.enterBackground();
        }
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mVV != null) {
            mVV.stopPlayback();
        }
    }


}
