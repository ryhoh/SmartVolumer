package com.gmail.axis38akasira.autovolumer;

class RegressionModel {
    private final static double[] beta = {-0.013011305980174026, 0.56593706};

    static Double infer(final double x) {
        return beta[0] + beta[1] * Math.sqrt(x);
    }
}
