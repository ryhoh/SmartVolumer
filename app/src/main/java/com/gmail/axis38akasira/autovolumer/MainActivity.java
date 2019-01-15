package com.gmail.axis38akasira.autovolumer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    final int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 128;

    AudioManager am;
    AudioRecord ar;
    boolean micAccessAllowed = false;
    boolean autoEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 権限のチェック
        check_permission();

        // 音量管理オブジェクトの初期化
        am = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        // AudioRecordのハンドラ
        handler_audioRecord();

        // モード切替ボタン
        init_buttonToggleMode();
    }

    private void check_permission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {
            // 権限の説明
            new AlertDialog.Builder(this)
                .setTitle("マイクへのアクセス")
                .setMessage("周囲の音量を知るために、マイクへのアクセスを許可してください。")
                .setPositiveButton(android.R.string.ok,
                    (DialogInterface dialog, int which) ->
                        ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.RECORD_AUDIO},
                            MY_PERMISSIONS_REQUEST_RECORD_AUDIO)
                ).create().show();
        } else {
            micAccessAllowed = true;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String permissions[], int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        micAccessAllowed = true;
                    } else {
                        openSettings();
                    }
            }
        }
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
                } catch (Exception e) {}
            }
        }

        if (buffSize == AudioRecord.ERROR_BAD_VALUE || buffSize == AudioRecord.ERROR) {
            throw new IllegalStateException();
        }

        return buffSize;
    }

    private void handler_audioRecord() {
        final TextView textView_playingVol = findViewById(R.id.tv_playingVol);
        final TextView textView_envVol = findViewById(R.id.tv_envVol);

        final Handler handler = new Handler();
        handler.post(new Runnable() {
            int micSenseCnt = 0, micSenseSum = 0;
            int buffSize;
            short[] buffer;
            @Override
            public void run() {
                if (micAccessAllowed) {
                    // マイク入力用 AudioRecord の設定
                    if (ar == null) {
                        buffSize = init_audioRecord();
                        buffer = new short[buffSize];
                        ar.startRecording();
                    }

                    if (ar.read(buffer, 0, buffSize) < 0) {
                        // 起動に失敗する場合，デバイスの設定->アプリ->権限->マイクを許可
                        throw new IllegalStateException();
                    }
                    // 最大
                    int max_val = Integer.MIN_VALUE;
                    for (short x : buffer) {
                        max_val = Math.max(max_val, x);
                    }

                    // 何度も計測して，平均値をその時間間隔の間の計測結果とする
                    micSenseSum += max_val;
                    if (micSenseCnt != 10) micSenseCnt++;
                    else {
                        final double inputLevel = micSenseSum / 10;
                        micSenseSum = 0;
                        micSenseCnt = 0;

                        textView_envVol.setText(String.valueOf(inputLevel));

                        if (autoEnabled) {
                            // 耳を護るために上限を設ける
                            double outLevel = RegressionModel.infer(inputLevel / 100000);
                            outLevel = Math.min(outLevel, 0.25);
                            // 下限も設定して音が消えないようにする
                            final int i_outLevel = (int) Math.max(Math.round(outLevel * am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 1);

                            // 再生音量を変更し，TextViewにも反映
                            am.setStreamVolume(AudioManager.STREAM_MUSIC, i_outLevel, 0);
                            textView_playingVol.setText(String.valueOf(i_outLevel));
                        }
                    }
                }
                handler.postDelayed(this, 100);
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
                    // 録音権限チェック
                    check_permission();
                    if (micAccessAllowed) {
                        textView_mode.setText(R.string.automationOn);
                        autoEnabled = true;
                    }
                } else {
                    textView_mode.setText(R.string.automationOff);
                    autoEnabled = false;
                }
            }
        });
    }

    private void openSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(Uri.fromParts("package", getPackageName(), null));
        startActivity(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (ar != null) {
            ar.stop();
            ar.release();
        }
    }

}
