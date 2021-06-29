package com.setvect.bokslcoin.autotrading.etc;

import java.io.IOException;

public class SystemFormat {
    public static void main(String[] args) throws IOException {
        double v = 100000.0965;
        System.out.printf("%f%n",v);
        System.out.printf("%,f%n",v);
        System.out.printf("%,.2f%n",v);
//        System.out.printf("%f%n",v);
    }
}
