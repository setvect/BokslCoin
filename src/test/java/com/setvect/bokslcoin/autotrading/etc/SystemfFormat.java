package com.setvect.bokslcoin.autotrading.etc;

import java.io.IOException;

public class SystemfFormat {
    public static void main(String[] args) throws IOException {
        double v = 100000.000965;
        System.out.printf("%f%n",v);
        System.out.printf("%,f%n",v);
        System.out.printf("%,.00f%n",v);
//        System.out.printf("%f%n",v);
    }
}
