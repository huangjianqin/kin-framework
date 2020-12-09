package org.kin.framework.utils;

import java.io.*;
import java.net.*;
import java.util.Enumeration;
import java.util.regex.Pattern;

/**
 * @author huangjianqin
 * @date 2018/1/28
 */
public class NetUtils {
    private static final String ANYHOST = "0.0.0.0";
    private static final String LOCALHOST = "127.0.0.1";
    private static final Pattern IP_PATTERN = Pattern.compile("\\d{1,3}(\\.\\d{1,3}){3,5}:\\d{1,5}$");

    private static volatile InetAddress LOCAL_ADDRESS = null;

    // -------------------------------------------------------------------- valid ------------------------------------------------------

    /**
     * valid Inet4Address
     */
    private static boolean isValidAddress(InetAddress address) {
        if (address == null || address.isLoopbackAddress()) {
            return false;
        }
        String name = address.getHostAddress();
        return (name != null
                && !ANYHOST.equals(name)
                && !LOCALHOST.equals(name)
                && IP_PATTERN.matcher(name).matches());
    }

    /**
     * valid Inet6Address, if an ipv6 address is reachable.
     */
    private static boolean isValidV6Address(Inet6Address address) {
        boolean preferIpv6 = Boolean.getBoolean("java.net.preferIPv6Addresses");
        if (!preferIpv6) {
            return false;
        }
        try {
            return address.isReachable(100);
        } catch (IOException e) {
            // ignore
        }
        return false;
    }

    /**
     * normalize the ipv6 Address, convert scope name to scope id.
     * <p>
     * e.g.
     * convert
     * fe80:0:0:0:894:aeec:f37d:23e1%en0
     * to
     * fe80:0:0:0:894:aeec:f37d:23e1%5
     * <p>
     * The %5 after ipv6 address is called scope id.
     * see java doc of {@link Inet6Address} for more details.
     *
     * @param address the input address
     * @return the normalized address, with scope id converted to int
     */
    private static InetAddress normalizeV6Address(Inet6Address address) {
        String addr = address.getHostAddress();
        int i = addr.lastIndexOf('%');
        if (i > 0) {
            try {
                return InetAddress.getByName(addr.substring(0, i) + '%' + address.getScopeId());
            } catch (UnknownHostException e) {
                // ignore
            }
        }
        return address;
    }

    // ---------------------- find ip ----------------------

    private static InetAddress getLocalAddress0() {
        InetAddress localAddress = null;
        try {
            localAddress = InetAddress.getLocalHost();
            if (localAddress instanceof Inet6Address) {
                Inet6Address address = (Inet6Address) localAddress;
                if (isValidV6Address(address)) {
                    return normalizeV6Address(address);
                }
            } else if (isValidAddress(localAddress)) {
                return localAddress;
            }
        } catch (Throwable e) {
            ExceptionUtils.throwExt(e);
        }
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            if (null == interfaces) {
                return localAddress;
            }
            while (interfaces.hasMoreElements()) {
                try {
                    NetworkInterface network = interfaces.nextElement();
                    Enumeration<InetAddress> addresses = network.getInetAddresses();
                    while (addresses.hasMoreElements()) {
                        try {
                            InetAddress address = addresses.nextElement();
                            if (address instanceof Inet6Address) {
                                Inet6Address v6Address = (Inet6Address) address;
                                if (isValidV6Address(v6Address)) {
                                    return normalizeV6Address(v6Address);
                                }
                            } else if (isValidAddress(address)) {
                                return address;
                            }
                        } catch (Throwable e) {
                            ExceptionUtils.throwExt(e);
                        }
                    }
                } catch (Throwable e) {
                    ExceptionUtils.throwExt(e);
                }
            }
        } catch (Throwable e) {
            ExceptionUtils.throwExt(e);
        }
        return localAddress;
    }


    // ---------------------- tool ----------------------

    public static boolean checkHostPort(String address) {
        return address.matches(IP_PATTERN.pattern());
    }

    /**
     * Find first valid IP from local network card
     *
     * @return first valid local IP
     */
    public static InetAddress getLocalAddress() {
        if (LOCAL_ADDRESS != null) {
            return LOCAL_ADDRESS;
        }
        InetAddress localAddress = getLocalAddress0();
        LOCAL_ADDRESS = localAddress;
        return localAddress;
    }

    /**
     * get ip address
     *
     * @return String
     */
    public static String getIp() {
        return getLocalAddress().getHostAddress();
    }

    /**
     * @param port 端口号
     * @return String ip:port
     */
    public static String getIpPort(int port) {
        String ip = getIp();
        return getIpPort(ip, port);
    }

    public static String getIpPort(String ip, int port) {
        if (ip == null) {
            return null;
        }
        return ip.concat(":").concat(String.valueOf(port));
    }

    public static String getIpPort2(int port) {
        InetSocketAddress address = new InetSocketAddress(port);
        return address.toString();
    }

    public static String getIpPort2(String ip, int port) {
        InetSocketAddress address = new InetSocketAddress(ip, port);
        return address.toString();
    }

    public static Object[] parseIpPort(String address) {
        String[] array = address.split(":");

        String host = array[0];
        int port = Integer.parseInt(array[1]);

        return new Object[]{host, port};
    }

    /**
     * 检查端口是否在指定范围内
     *
     * @param port 端口号
     * @return 是否合法
     */
    public static boolean isPortInRange(int port) {
        return 0 <= port && port <= 0xFFFF;
    }

    /**
     * 检查端口是否被占用
     */
    public static boolean isValidPort(int port) {
        return isValidPort(getIp(), port);
    }

    /**
     * 检查端口是否被占用
     */
    public static boolean isValidPort(String host, int port) {
        if (StringUtils.isBlank(host) || !isPortInRange(port)) {
            return false;
        }

        Socket socket = null;
        try {
            socket = new Socket();
            socket.bind(new InetSocketAddress(host, port));
            return true;
        } catch (IOException ignored) {

        } finally {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException e) {

                }
            }
        }

        return false;
    }

    /**
     * 读取远程服务器文件, 需保留两服务器连通
     */
    public static boolean copyRemoteFile(String url, String sinkFileName) {
        URL u;
        try {
            u = new URL(url);
            URLConnection uc = u.openConnection();
            //setting
            uc.setUseCaches(false);
            uc.setReadTimeout(5 * 1000);
            uc.setConnectTimeout(3 * 1000);

            String contentType = uc.getContentType();
            int contentLength = uc.getContentLength();
            if (contentLength != -1) {
                InputStream raw = uc.getInputStream();
                InputStream in = new BufferedInputStream(raw);
                byte[] data = new byte[contentLength];
                int bytesRead;
                int offset = 0;
                while (offset < contentLength) {
                    bytesRead = in.read(data, offset, data.length - offset);
                    if (bytesRead == -1) {
                        break;
                    }
                    offset += bytesRead;
                }
                in.close();

                if (offset != contentLength) {
                    throw new IOException("Only read " + offset + " bytes; Expected " + contentLength + " bytes");
                }

                File sinkFile = new File(sinkFileName);
                if (!sinkFile.exists()) {
                    sinkFile.getParentFile().mkdirs();
                }

                FileOutputStream out = new FileOutputStream(sinkFileName);
                out.write(data);
                out.flush();
                out.close();
            }

            return true;
        } catch (IOException e) {
            ExceptionUtils.throwExt(e);
        }

        return false;
    }

    /**
     * hash ip
     */
    public static long ipHashCode(String ip) {
        String[] splits = ip.split("/");
        if (splits.length > 1) {
            ip = splits[splits.length - 1];
        }
        splits = ip.split("\\.");
        long hashcode = 0L;
        int offset = 24;
        for (String item : splits) {
            hashcode += Long.parseLong(item) << offset;
            offset -= 8;
        }
        return hashcode;
    }

    /**
     * hash ip+port
     */
    public static long ipHashCode(String ip, int port) {
        return ipHashCode(ip) + port;
    }
}
