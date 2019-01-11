package com.gmail.axis38akasira.autovolumer;

import android.content.Context;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    SeekBar vol;
    AudioManager am;
    AudioRecord ar;
    boolean autoEnabled = false;
    int micSenseCnt = 0, micSenseSum = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        vol = findViewById(R.id.seekbar_vol);
        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
        final TextView textView_envVol = findViewById(R.id.tv_envVol);

        // 音量調整用シークバー
        init_seekBar();

        // AudioRecordのハンドラ
        handler_audioRecord(textView_envVol);

        // 学習ボタン
        init_buttonLearn();

        // モード切替ボタン
        init_buttonToggleMode();
    }

    private void init_seekBar() {
        final TextView textView_playingVol = findViewById(R.id.tv_playingVol);
        vol.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {
                    @Override
                    public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                        int pr = seekBar.getProgress();
                        int am_max = am.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, am_max * pr / seekBar.getMax(), 0);
                        textView_playingVol.setText("再生音量: " + am_max * pr / seekBar.getMax());
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                    }
                }
        );
    }

    private int init_audioRecord() {
        int buffSize = 0;
        for (short audioFormat: new short[] {AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT}) {
            for (short channelConfig: new short[] {AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                try {
                    buffSize = AudioRecord.getMinBufferSize(8000,
                            channelConfig, audioFormat);
                    ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                            channelConfig, audioFormat, buffSize);
                    if (ar.getState() == AudioRecord.STATE_INITIALIZED) {
                        break;
                    }
                } catch (Exception e) { }
            }
        }

        if (buffSize == 0) {
            throw new IllegalStateException();
        }

        return buffSize;
    }

    private void handler_audioRecord(final TextView textView_envVol) {
        // マイク入力用 AudioRecord の設定
        final int buffSize = init_audioRecord();
        final TextView textView_playingVol = findViewById(R.id.tv_playingVol);

        final Handler handler = new Handler();
        ar.startRecording();
        handler.post(new Runnable() {
            short[] buffer = new short[buffSize];
            @Override
            public void run() {
                int res;
                if ((res = ar.read(buffer, 0, buffSize)) < 0) {
                    Log.e("AudioRead", String.valueOf(res));  // -3が出たらデバイスの設定からアプリのマイクを許可
                    throw new IllegalStateException();
                }
                // 最大
                int max_val = Integer.MIN_VALUE;
                for (short x: buffer) {
                    max_val = Math.max(max_val, x);
                }
//                textView_envVol.setText(String.valueOf(max_val));

                // 何度も計測して，平均値をその時間間隔の間の計測結果とする
                micSenseSum += max_val;
                if (micSenseCnt != 10) micSenseCnt++;
                else {
                    double inputLevel = micSenseSum / 10;
                    micSenseSum = 0; micSenseCnt = 0;
                    textView_envVol.setText(String.valueOf(inputLevel));

                    if (autoEnabled) {
                        // 耳を護るために上限を設ける
                        double outLevel = RegressionModel.infer(inputLevel / 100000);
                        outLevel = Math.min(outLevel, 0.25);
                        // 下限も設定して音が消えないようにする
                        int i_outLevel = (int) Math.max(Math.round(outLevel * am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 1);

                        // 再生音量を変更し，TextViewにも反映
                        am.setStreamVolume(AudioManager.STREAM_MUSIC, i_outLevel, 0);
                        textView_playingVol.setText("再生音量: " + i_outLevel);
                    }
                }

                handler.postDelayed(this, 100);
            }
        });
    }

    private void init_buttonLearn() {
        final Button button_learn = findViewById(R.id.but_learn);
        button_learn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                reg.learn(inputLevel, am.getStreamVolume(AudioManager.STREAM_MUSIC));
            }
        });
    }

    private void init_buttonToggleMode() {
        final Button textView_toggleAuto = findViewById(R.id.but_toggleMode);
        final TextView textView_mode = findViewById(R.id.tv_mode);
        textView_toggleAuto.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!autoEnabled) {
                    textView_mode.setText("自動調整ON");
                    autoEnabled = true;
                } else {
                    textView_mode.setText("自動調整OFF");
                    autoEnabled = false;
                }
            }
        });
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        ar.stop();
        ar.release();
    }

}
