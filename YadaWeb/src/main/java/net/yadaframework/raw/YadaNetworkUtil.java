package net.yadaframework.raw;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class YadaNetworkUtil {
	@SuppressWarnings("unused")
	private final transient Logger log = LoggerFactory.getLogger(getClass());
	
	/**
	 * Find all ipv4 and ipv6 addresses on the local host that can be used for sending packets to the internet
	 * @return
	 * @throws IOException
	 */
	public List<InetAddress> findPublicAddresses() throws IOException {
		List<InetAddress> result = new ArrayList<InetAddress>();
		Enumeration<NetworkInterface> networkInterfaces = NetworkInterface.getNetworkInterfaces();
		while (networkInterfaces.hasMoreElements()) {
			NetworkInterface networkInterface = networkInterfaces.nextElement();
			if (!networkInterface.isLoopback() && networkInterface.isUp()) {
				Enumeration<InetAddress> inetAddresses = networkInterface.getInetAddresses();
				while (inetAddresses.hasMoreElements()) {
					InetAddress inetAddress = inetAddresses.nextElement();
					if (!inetAddress.isLinkLocalAddress()) {
						result.add(inetAddress);
					}
				}
			}
		}
		return result;
	}
}
