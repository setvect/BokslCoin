package com.setvect.bokslcoin.autotrading.etc;

import com.setvect.bokslcoin.autotrading.util.ApplicationUtil;

public class ApplicationUtilTest {
    public static void main(String[] args) {
        double cagr = ApplicationUtil.getCagr(1000, 2000, 365 * 3);
        System.out.println(cagr);

        cagr = ApplicationUtil.getCagr(1000, 3000, 365 * 3);
        System.out.println(cagr);

        cagr = ApplicationUtil.getCagr(1000, 500, 365 * 3);
        System.out.println(cagr);

        cagr = ApplicationUtil.getCagr(1000, 500, 182);
        System.out.println(cagr);

        cagr = ApplicationUtil.getCagr(1000, 1500, 365);
        System.out.println(cagr);

        cagr = ApplicationUtil.getCagr(1000, 1500, 182);
        System.out.println(cagr);
    }
}
