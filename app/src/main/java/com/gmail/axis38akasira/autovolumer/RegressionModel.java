package com.gmail.axis38akasira.autovolumer;

class RegressionModel {
    private final static double[] beta1 = {0.0326086957, 1.46245059};
    private final static double[] beta2 = {0.0176811594, 2.31544266, -7.75456428};

    static Double infer(double x) {
        if (x <= 0.06) {
            return f_2(x);
        } else {
            return Math.max(f_1(x), f_2(x));
        }
    }

    private static Double f_1(double x) {
        return beta1[0] + beta1[1] * x;
    }

    private static Double f_2(double x) {
        return beta2[0] + beta2[1] * x + beta2[2] * x * x;
    }
}
