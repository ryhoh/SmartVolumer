package com.gmail.axis38akasira.autovolumer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.os.Handler;
import android.widget.TextView;

class VolumeManager implements Runnable {

    private Handler handler;
    private TextView textView_envVol, textView_playingVol;
    private int micSenseCnt = 0, micSenseSum = 0;
    private int buffSize;
    private short[] buffer;

    VolumeManager(Handler handler, TextView textView_envVol, TextView textView_playingVol) {
        this.handler = handler;
        this.textView_envVol = textView_envVol;
        this.textView_playingVol = textView_playingVol;
    }

    @Override
    public void run() {
        if (MainActivity.micAccessAllowed) {
            // マイク入力用 AudioRecord の設定
            if (MainActivity.ar == null) {
                buffSize = init_audioRecord();
                buffer = new short[buffSize];
                MainActivity.ar.startRecording();
            }

            if (MainActivity.ar.read(buffer, 0, buffSize) < 0) {
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
            if (micSenseCnt != 9) micSenseCnt++;
            else {
                final double inputLevel = micSenseSum / 10.0;
                micSenseSum = 0;
                micSenseCnt = 0;

                textView_envVol.setText(String.valueOf(inputLevel));

                if (MainActivity.autoEnabled) {
                    // 耳を護るために上限を設ける
                    double outLevel = RegressionModel.infer(inputLevel / 100000);
                    outLevel = Math.min(outLevel, 0.25);
                    // 下限も設定して音が消えないようにする
                    final int i_outLevel = (int) Math.max(Math.round(outLevel * MainActivity.am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 1);

                    // 再生音量を変更し，TextViewにも反映
                    MainActivity.am.setStreamVolume(AudioManager.STREAM_MUSIC, i_outLevel, 0);
                    textView_playingVol.setText(String.valueOf(i_outLevel));
                }
            }
        }
        handler.postDelayed(this, 100);
    }

    private int init_audioRecord() {
        int buffSize = 0;
        for (short audioFormat: new short[] {AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT}) {
            for (short channelConfig: new short[] {AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                try {
                    buffSize = AudioRecord.getMinBufferSize(8000,
                            channelConfig, audioFormat);
                    MainActivity.ar = new AudioRecord(MediaRecorder.AudioSource.MIC, 8000,
                            channelConfig, audioFormat, buffSize);
                    if (MainActivity.ar.getState() == AudioRecord.STATE_INITIALIZED) {
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
}
