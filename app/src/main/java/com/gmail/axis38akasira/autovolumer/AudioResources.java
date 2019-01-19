package com.gmail.axis38akasira.autovolumer;

import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioRecord;
import android.media.MediaRecorder;
import android.support.annotation.NonNull;
import android.widget.TextView;

class AudioResources {

    private static final int SAMPLE_RATE_IN_HZ = 8000;

    private AudioManager am;
    private AudioRecord ar;
    private Boolean micAccessAllowed = false;
    private Boolean autoEnabled = false;
    private int buffSize;
    private short[] buffer;

    private int initAudioRecorder() {
        int buffSize = 0;
        for (short audioFormat: new short[] {AudioFormat.ENCODING_PCM_8BIT, AudioFormat.ENCODING_PCM_16BIT}) {
            for (short channelConfig: new short[] {AudioFormat.CHANNEL_IN_MONO, AudioFormat.CHANNEL_IN_STEREO}) {
                try {
                    buffSize = AudioRecord.getMinBufferSize(SAMPLE_RATE_IN_HZ,
                            channelConfig, audioFormat);
                    ar = new AudioRecord(MediaRecorder.AudioSource.MIC, SAMPLE_RATE_IN_HZ,
                            channelConfig, audioFormat, buffSize);
                    if (ar.getState() == AudioRecord.STATE_INITIALIZED) {
                        break;
                    }
                } catch (Exception ignored) {}
            }
        }

        if (buffSize == AudioRecord.ERROR_BAD_VALUE || buffSize == AudioRecord.ERROR) {
            throw new IllegalStateException();
        }

        return buffSize;
    }

    short[] readByAudioRecorder() {
        // マイク入力用 AudioRecord の設定
        if (ar == null) {
            buffSize = initAudioRecorder();
            buffer = new short[buffSize];
            ar.startRecording();
        }

        if (ar.read(buffer, 0, buffSize) < 0) {
            // 起動に失敗する場合，デバイスの設定->アプリ->権限->マイクを許可
            throw new IllegalStateException();
        }

        return buffer;
    }

    void deleteAudioRecorder() {
        if (ar != null) {
            ar.stop();
            ar.release();
        }
    }

    int applyPlayingVolume(double inputLevel, @NonNull TextView textView_playingVol) {
        // return 出力レベル
        if (autoEnabled) {
            // 耳を護るために上限を設ける
            double outLevel = RegressionModel.infer(inputLevel / 100000);
            outLevel = Math.min(outLevel, 0.25);
            // 下限も設定して音が消えないようにする
            final int i_outLevel = (int) Math.max(Math.round(outLevel * am.getStreamMaxVolume(AudioManager.STREAM_MUSIC)), 1);

            // 再生音量を変更し，TextViewにも反映
            am.setStreamVolume(AudioManager.STREAM_MUSIC, i_outLevel, 0);
            textView_playingVol.setText(String.valueOf(i_outLevel));

            return i_outLevel;
        } else {
            return -1;
        }
    }

    void setAudioManager(@NonNull AudioManager am) {
        this.am = am;
    }

    void EnableMicAccessAllowed() {
        this.micAccessAllowed = true;
    }

    void setAutoEnabled(boolean autoEnabled) {
        this.autoEnabled = autoEnabled;
    }

    Boolean getMicAccessAllowed() {
        return micAccessAllowed;
    }

    Boolean getAutoEnabled() {
        return autoEnabled;
    }


}
