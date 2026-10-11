package exercise10;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;

/**
 * Bài tập đề xuất 10: Server kết hợp UDP Discovery và TCP Service
 * - Phản hồi UDP gói tin DISCOVER_SERVICE: "SERVICE IUH_MainService 7012 v2026"
 * - Cung cấp dịch vụ TCP tương ứng trên cổng 7012
 */
public class ServiceDiscoveryServer {
    public static final int UDP_DISCOVERY_PORT = 7011;
    public static final int TCP_SERVICE_PORT = 7012;
    public static final String SERVICE_NAME = "IUH_MainService";
    public static final String SERVICE_VERSION = "v2026";

    public static void main(String[] args) {
        // 1. Luồng chạy dịch vụ TCP
        Thread tcpThread = new Thread(ServiceDiscoveryServer::startTcpService);
        tcpThread.setDaemon(true);
        tcpThread.start();

        // 2. Luồng chính chạy UDP Discovery Responder
        startUdpDiscoveryResponder();
    }

    public static void startUdpDiscoveryResponder() {
        byte[] buffer = new byte[1024];
        try (DatagramSocket udpSocket = new DatagramSocket(UDP_DISCOVERY_PORT)) {
            System.out.println("[UDP Discovery] Lắng nghe yêu cầu khám phá dịch vụ tại cổng " + UDP_DISCOVERY_PORT);
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                udpSocket.receive(packet);

                String request = new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8).trim();
                System.out.println("[UDP Discovery] Nhận từ " + packet.getAddress() + ":" + packet.getPort() + " -> " + request);

                if (request.equalsIgnoreCase("DISCOVER_SERVICE")) {
                    String response = String.format("SERVICE %s %d %s", SERVICE_NAME, TCP_SERVICE_PORT, SERVICE_VERSION);
                    byte[] respData = response.getBytes(StandardCharsets.UTF_8);

                    DatagramPacket respPacket = new DatagramPacket(respData, respData.length, packet.getAddress(), packet.getPort());
                    udpSocket.send(respPacket);
                    System.out.println("[UDP Discovery] Đã trả lời: " + response);
                }
            }
        } catch (IOException e) {
            System.err.println("[UDP Discovery Error] " + e.getMessage());
        }
    }

    public static void startTcpService() {
        try (ServerSocket tcpServer = new ServerSocket(TCP_SERVICE_PORT)) {
            System.out.println("[TCP Service] Dịch vụ chính đang mở tại cổng " + TCP_SERVICE_PORT);
            while (true) {
                Socket client = tcpServer.accept();
                new Thread(() -> handleTcpClient(client)).start();
            }
        } catch (IOException e) {
            System.err.println("[TCP Service Error] " + e.getMessage());
        }
    }

    private static void handleTcpClient(Socket socket) {
        String clientAddr = String.valueOf(socket.getRemoteSocketAddress());
        System.out.println("[TCP Service] Client đã kết nối: " + clientAddr);

        try (socket;
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            out.println("WELCOME Chào mừng bạn đến dịch vụ " + SERVICE_NAME + " " + SERVICE_VERSION);

            String line;
            while ((line = in.readLine()) != null) {
                line = line.trim();
                if (line.equalsIgnoreCase("QUIT")) {
                    out.println("OK BYE");
                    break;
                } else if (line.equalsIgnoreCase("TIME")) {
                    out.println("OK " + LocalDateTime.now());
                } else if (line.startsWith("ECHO ")) {
                    out.println("OK " + line.substring(5));
                } else {
                    out.println("ERR UNKNOWN_COMMAND (Hỗ trợ: ECHO <text>, TIME, QUIT)");
                }
            }
        } catch (IOException e) {
            System.err.println("[TCP Service] Lỗi kết nối client: " + e.getMessage());
        } finally {
            System.out.println("[TCP Service] Client đã ngắt kết nối: " + clientAddr);
        }
    }
}
