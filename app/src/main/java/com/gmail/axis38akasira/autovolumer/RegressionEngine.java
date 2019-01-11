package com.gmail.axis38akasira.autovolumer;

import java.util.HashMap;

// y = a + bx の推論モデル
class RegressionEngine {

    HashMap<Integer, Integer> data = new HashMap<>();
    private double beta[] = new double[2];  // 切片, 一次の係数

    RegressionEngine() {
        data.put(200, 1);
        data.put(250, 1);
        refresh();
    }

    void learn(int x, int y) {
        // 1の位を切り捨て
        x = (x / 10 * 10);

        // データ更新
        data.put(x, y);

        // 重み更新
        refresh();
    }

    double infer(int x) {
        return beta[0] + beta[1] * x;
    }

    // 重み更新
    private void refresh() {
        // x, y の平均
        double x_mean = 0, y_mean = 0;

        for (Integer xi: data.keySet()) {
            x_mean += xi;
        }
        x_mean = x_mean / data.size();

        for (Integer yi: data.values()) {
            y_mean += yi;
        }
        y_mean = y_mean / data.size();


        double top = 0, bottom = 0;
        for (Integer xi: data.keySet()) {
            top += (data.get(xi) - y_mean) * xi;
            bottom += (xi - x_mean) * xi;
        }
        beta[1] = top / bottom;

        // 分散・共分散
        double cov = 0, var = 0;
        for (Integer xi: data.keySet()) {
            cov += (data.get(xi) - y_mean) * (xi - x_mean);
            var += Math.pow((xi - x_mean), 2);
        }
        beta[0] = cov / var;
    }

}
