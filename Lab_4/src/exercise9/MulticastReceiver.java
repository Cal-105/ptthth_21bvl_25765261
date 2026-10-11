package exercise9;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.MulticastSocket;
import java.net.NetworkInterface;
import java.nio.charset.StandardCharsets;

/**
 * Bài tập đề xuất 9: Multicast Receiver (Bên nhận thông báo)
 * - Tham gia group multicast 239.255.0.1 cổng 7010
 * - Chỉ rõ NetworkInterface khi joinGroup và leaveGroup
 */
public class MulticastReceiver {
    public static final String MULTICAST_IP = "239.255.0.1";
    public static final int PORT = 7010;

    public static void main(String[] args) {
        String receiverId = args.length > 0 ? args[0] : "Receiver_1";
        startReceiver(receiverId, 0); // 0 = chạy liên tục
    }

    public static void startReceiver(String receiverId, int maxMessages) {
        byte[] buffer = new byte[4096];

        try {
            InetAddress group = InetAddress.getByName(MULTICAST_IP);
            NetworkInterface netIf = getPreferredNetworkInterface();

            try (MulticastSocket socket = new MulticastSocket(PORT)) {
                InetSocketAddress groupAddress = new InetSocketAddress(group, PORT);
                socket.joinGroup(groupAddress, netIf);
                System.out.println("[" + receiverId + "] Đã tham gia group " + MULTICAST_IP + ":" + PORT
                        + " qua interface: " + netIf.getDisplayName());

                int receivedCount = 0;
                while (maxMessages == 0 || receivedCount < maxMessages) {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    socket.receive(packet);

                    String message = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
                    System.out.println("[" + receiverId + "] Nhận từ " + packet.getAddress() + ":" + packet.getPort() + " -> " + message);
                    receivedCount++;
                }

                socket.leaveGroup(groupAddress, netIf);
                System.out.println("[" + receiverId + "] Đã rời group " + MULTICAST_IP);
            }
        } catch (IOException e) {
            System.err.println("[" + receiverId + "] Lỗi Multicast Receiver: " + e.getMessage());
        }
    }

    public static NetworkInterface getPreferredNetworkInterface() throws IOException {
        // Ưu tiên loopback hoặc card mạng đầu tiên đang hoạt động hỗ trợ multicast
        NetworkInterface loopback = NetworkInterface.getByInetAddress(InetAddress.getByName("127.0.0.1"));
        if (loopback != null && loopback.isUp()) {
            return loopback;
        }
        var interfaces = NetworkInterface.getNetworkInterfaces();
        while (interfaces.hasMoreElements()) {
            NetworkInterface ni = interfaces.nextElement();
            if (ni.isUp() && ni.supportsMulticast()) {
                return ni;
            }
        }
        return loopback;
    }
}
