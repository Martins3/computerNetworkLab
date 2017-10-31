package yuema.message;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Collections;
import java.util.Enumeration;

import static java.lang.System.out;

/**
 * Created by martin on 17-10-30.
 *
 */
public class NetInterfaceIP {
    public static void main(String[] args) throws SocketException {
        String a = localHostname();
        out.printf("%s", a);
    }

    public static String localHostname() throws SocketException {
        Enumeration<NetworkInterface> nets = NetworkInterface.getNetworkInterfaces();
        for (NetworkInterface netInt : Collections.list(nets)) {
            Enumeration<InetAddress> internetAddresses = netInt.getInetAddresses();
            for (InetAddress inetAddress : Collections.list(internetAddresses)) {
                if (inetAddress instanceof Inet4Address && !inetAddress.isLoopbackAddress()) {
                    String a = inetAddress.toString();
                    return a.substring(1, a.length());
                }
            }
        }
        assert false; // 此部分不可以到达
        return null;
    }
}
