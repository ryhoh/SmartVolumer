package com.gmail.axis38akasira.autovolumer;

class RegressionModel {
    private static double[] beta = {0.0176811594, 2.31544266, -7.75456428};

    static Double infer(double x) {
        return beta[0] + beta[1] * x + beta[2] * x * x;
    }
}
