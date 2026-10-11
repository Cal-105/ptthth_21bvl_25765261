package exercise10;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Bài tập đề xuất 10: Client khám phá dịch vụ qua UDP và kết nối tới TCP Service
 * - Gửi broadcast/unicast DISCOVER_SERVICE qua UDP
 * - Nhận danh sách dịch vụ: SERVICE <tên> <port> <phiên_bản>
 * - Loại bỏ phản hồi trùng lặp
 * - Tự động kết nối tới TCP service tại địa chỉ và cổng tìm được
 */
public class ServiceDiscoveryClient {
    public static void main(String[] args) {
        String targetHost = args.length > 0 ? args[0] : "localhost";
        int discoveryPort = args.length > 1 ? Integer.parseInt(args[1]) : ServiceDiscoveryServer.UDP_DISCOVERY_PORT;

        System.out.println("==========================================================================");
        System.out.println("BÀI TẬP 10: KHÁM PHÁ DỊCH VỤ QUA UDP VÀ TỰ ĐỘNG KẾT NỐI TCP SERVICE");
        System.out.println("==========================================================================");

        DiscoveredService discoveredService = discoverService(targetHost, discoveryPort);

        if (discoveredService != null) {
            System.out.println("\n-> Tiến hành kết nối tới TCP Service tại "
                    + discoveredService.host.getHostAddress() + ":" + discoveredService.tcpPort + "...\n");
            connectAndUseTcpService(discoveredService.host, discoveredService.tcpPort);
        } else {
            System.err.println("Không tìm thấy dịch vụ nào khả dụng!");
        }

        System.out.println("\n==========================================================================");
        System.out.println("HOÀN THÀNH BÀI TẬP 10 KHÁM PHÁ DỊCH VỤ THÀNH CÔNG!");
        System.out.println("==========================================================================\n");
    }

    public static DiscoveredService discoverService(String host, int port) {
        DiscoveredService selectedService = null;
        Set<String> seenServices = new HashSet<>();

        try (DatagramSocket udpSocket = new DatagramSocket()) {
            udpSocket.setSoTimeout(2500); // 2.5s timeout chờ phản hồi
            byte[] sendData = "DISCOVER_SERVICE".getBytes(StandardCharsets.UTF_8);

            InetAddress targetAddr = InetAddress.getByName(host);
            DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, targetAddr, port);

            System.out.println("[Client] Đang gửi yêu cầu UDP DISCOVER_SERVICE tới " + host + ":" + port + "...");
            udpSocket.send(sendPacket);

            byte[] buffer = new byte[1024];
            while (true) {
                try {
                    DatagramPacket receivePacket = new DatagramPacket(buffer, buffer.length);
                    udpSocket.receive(receivePacket);

                    String text = new String(receivePacket.getData(), receivePacket.getOffset(), receivePacket.getLength(), StandardCharsets.UTF_8).trim();
                    System.out.println("[Client] Nhận phản hồi từ " + receivePacket.getAddress() + ":" + receivePacket.getPort() + " -> " + text);

                    if (text.startsWith("SERVICE ")) {
                        String[] parts = text.split("\\s+");
                        if (parts.length >= 4) {
                            String serviceName = parts[1];
                            int tcpPort = Integer.parseInt(parts[2]);
                            String version = parts[3];

                            String uniqueKey = receivePacket.getAddress().getHostAddress() + ":" + tcpPort;
                            if (!seenServices.contains(uniqueKey)) {
                                seenServices.add(uniqueKey);
                                System.out.println("   ==> Tìm thấy dịch vụ mới: " + serviceName
                                        + " (Version: " + version + ") tại IP: " + receivePacket.getAddress().getHostAddress()
                                        + ", TCP Port: " + tcpPort);

                                if (selectedService == null) {
                                    selectedService = new DiscoveredService(serviceName, receivePacket.getAddress(), tcpPort, version);
                                }
                            } else {
                                System.out.println("   (Bỏ qua phản hồi trùng lặp từ " + uniqueKey + ")");
                            }
                        }
                    }
                } catch (SocketTimeoutException e) {
                    System.out.println("[Client] Hết thời gian chờ phản hồi UDP discovery.");
                    break;
                }
            }
        } catch (Exception e) {
            System.err.println("[Client Error] Lỗi khám phá dịch vụ: " + e.getMessage());
        }

        return selectedService;
    }

    public static void connectAndUseTcpService(InetAddress host, int tcpPort) {
        try (Socket socket = new Socket(host, tcpPort);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            // Đọc thông báo chào mừng
            String welcome = in.readLine();
            System.out.println("[TCP Server]: " + welcome);

            // Gửi lệnh ECHO
            System.out.println("[Client -> TCP]: ECHO Hello IUH Distributed Systems!");
            out.println("ECHO Hello IUH Distributed Systems!");
            System.out.println("[TCP Server]: " + in.readLine());

            // Gửi lệnh TIME
            System.out.println("[Client -> TCP]: TIME");
            out.println("TIME");
            System.out.println("[TCP Server]: " + in.readLine());

            // Gửi lệnh QUIT
            System.out.println("[Client -> TCP]: QUIT");
            out.println("QUIT");
            System.out.println("[TCP Server]: " + in.readLine());

        } catch (Exception e) {
            System.err.println("Lỗi khi kết nối tới TCP Service: " + e.getMessage());
        }
    }

    public static class DiscoveredService {
        public String name;
        public InetAddress host;
        public int tcpPort;
        public String version;

        public DiscoveredService(String name, InetAddress host, int tcpPort, String version) {
            this.name = name;
            this.host = host;
            this.tcpPort = tcpPort;
            this.version = version;
        }
    }
}
