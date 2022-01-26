package org.kin.framework.utils;

import java.util.Arrays;
import java.util.stream.Collectors;

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

        System.out.println("-------------------------------------------------------------");
        System.out.println(StringUtils.mkString(Symbols.SEMICOLON, "127.0.0.1:16888", "127.0.0.2:16888"));
        System.out.println(StringUtils.mkString(Arrays.asList(1, 2, 3, 4)));
        System.out.println(StringUtils.mkString(Arrays.asList(1, 2, 3, 4).stream().collect(Collectors.toMap(item -> item, item -> item + 1)),
                "->", k -> String.valueOf(k + 1), v -> String.valueOf(v + 1)));

    }
}
