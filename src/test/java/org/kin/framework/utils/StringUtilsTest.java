package org.kin.framework.utils;

/**
 * @author huangjianqin
 * @date 2020/9/21
 */
public class StringUtilsTest {
    public static void main(String[] args) {
        String s = "abc";
        String url = "https://weibo.com/u/2264861970/home?topnav=1&wvr=6&mod=logo#1600656831449";

        String hexS = StringUtils.str2HexStr(s);
        String hexUrl = StringUtils.str2HexStr(url);

        String s2 = StringUtils.hexStr2Str(hexS);
        String url2 = StringUtils.hexStr2Str(hexUrl);

        System.out.println(hexS);
        System.out.println(s2);
        System.out.println(s.equals(s2));
        System.out.println("--------------");
        System.out.println(hexUrl);
        System.out.println(url2);
        System.out.println(url.equals(url2));

    }
}
