package com.gmail.axis38akasira.autovolumer;

import android.os.Handler;
import android.support.annotation.NonNull;
import android.widget.TextView;

import com.gmail.axis38akasira.autovolumer.notifications.NotificationWrapper;

class VolumeManager implements Runnable {

    private AudioResources aRes;
    private Handler handler;
    private TextView textView_envVol, textView_playingVol;
    private NotificationWrapper notificationWrapper;
    private int micSenseCnt = 0, micSenseSum = 0;

    VolumeManager(@NonNull AudioResources aRes, @NonNull Handler handler,
                  @NonNull TextView textView_envVol, @NonNull TextView textView_playingVol,
                  @NonNull NotificationWrapper notificationWrapper) {
        this.aRes = aRes;
        this.handler = handler;
        this.textView_envVol = textView_envVol;
        this.textView_playingVol = textView_playingVol;
        this.notificationWrapper = notificationWrapper;
    }

    @Override
    public void run() {
        if (aRes.getMicAccessAllowed()) {
            final short[] buffer = aRes.readByAudioRecorder();

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
                final int outLevel = aRes.applyPlayingVolume(inputLevel, textView_playingVol);
                if (outLevel != -1) {
                    notificationWrapper.post(
                            MainActivity.class, "音量の自動調整が有効",
                            notificationWrapper.getActivity().getString(R.string.vol_playing)
                                    + String.valueOf(outLevel)
                    );
                }
            }
        }
        handler.postDelayed(this, 100);
    }
}
