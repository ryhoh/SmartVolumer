package com.gmail.axis38akasira.autovolumer;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.media.AudioManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;

public class MainActivity extends AppCompatActivity {

    final static int MY_PERMISSIONS_REQUEST_RECORD_AUDIO = 128;

    // 録音・音声ハードウェアの管理オブジェクト
    final AudioResources aRes = new AudioResources();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 権限のチェック
        check_permission();

        // 音量管理オブジェクトの初期化
        aRes.setAudioManager((AudioManager)getSystemService(Context.AUDIO_SERVICE));

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
            aRes.EnableMicAccessAllowed();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           @NonNull String permissions[],
                                           @NonNull int[] grantResults) {
        if (grantResults.length > 0) {
            switch (requestCode) {
                case MY_PERMISSIONS_REQUEST_RECORD_AUDIO:
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                        aRes.EnableMicAccessAllowed();
                    } else {
                        openSettings();
                    }
            }
        }
    }

    private void handler_audioRecord() {
        final TextView textView_playingVol = findViewById(R.id.tv_playingVol);
        final TextView textView_envVol = findViewById(R.id.tv_envVol);

        final Handler handler = new Handler();
        handler.post(new VolumeManager(aRes, handler, textView_envVol, textView_playingVol));
    }

    private void init_buttonToggleMode() {
        final Button textView_toggleAuto = findViewById(R.id.but_toggleMode);
        final TextView textView_mode = findViewById(R.id.tv_mode);
        textView_toggleAuto.setOnClickListener((v) -> {
            if (!aRes.getAutoEnabled()) {
                // 録音権限チェック
                check_permission();
                if (aRes.getMicAccessAllowed()) {
                    textView_mode.setText(R.string.automationOn);
                    aRes.setAutoEnabled(true);
                }
            } else {
                textView_mode.setText(R.string.automationOff);
                aRes.setAutoEnabled(false);
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
        aRes.deleteAudioRecorder();
    }

}
