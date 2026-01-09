package org.openhab.binding.marstek.internal;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;
import java.util.Arrays;

/**
 * Small helper that sends a UDP request and waits for a response.
 * It opens a temporary DatagramSocket bound to the given local port and returns the first response bytes received.
 */
public class MarstekUdpHelper {

    /**
     * Send a UDP request and receive a single response.
     * 
     * @param host remote host
     * @param port remote port
     * @param requestBytes payload to send
     * @param localBindPort local port to bind for receiving (0 = ephemeral)
     * @param timeoutMs socket receive timeout in milliseconds
     * @return the response bytes or null if timeout
     * @throws IOException on socket errors
     */
    public static byte[] sendRequest(String host, int port, byte[] requestBytes, int localBindPort, int timeoutMs)
            throws IOException {
        DatagramSocket socket = null;
        try {
            socket = new DatagramSocket(new InetSocketAddress(localBindPort));
            socket.setSoTimeout(Math.max(1, timeoutMs));

            InetAddress addr = InetAddress.getByName(host);
            DatagramPacket out = new DatagramPacket(requestBytes, requestBytes.length, addr, port);
            socket.send(out);

            byte[] buf = new byte[2048];
            DatagramPacket in = new DatagramPacket(buf, buf.length);
            try {
                socket.receive(in);
                return Arrays.copyOf(in.getData(), in.getLength());
            } catch (SocketTimeoutException e) {
                return null; // no response in time
            }
        } finally {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        }
    }
}
