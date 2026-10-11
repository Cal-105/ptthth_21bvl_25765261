package exercise6;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

/**
 * Bài tập đề xuất 6: Thực nghiệm so sánh hiệu năng TCP và UDP
 * - Gửi 1000 thông điệp kích thước cố định (64 bytes) qua TCP và UDP trên localhost
 * - Đo tổng thời gian (ms) và số phản hồi nhận được
 * - Lặp lại 5 lần (5 runs) và tính giá trị trung bình
 * - Ghi nhận cấu hình môi trường, kích thước dữ liệu, timeout và phương pháp đo
 */
public class TcpUdpBenchmark {
    private static final int TCP_PORT = 7006;
    private static final int UDP_PORT = 7007;
    private static final int NUM_MESSAGES = 1000;
    private static final int NUM_RUNS = 5;
    private static final int TIMEOUT_MS = 3000;

    // Chuỗi mẫu cố định 64 bytes
    private static final String SAMPLE_PAYLOAD = "BENCHMARK_PAYLOAD_TEST_DATA_PACKET_IUH_NETWORK_PROGRAMMING_2026!";

    public static void main(String[] args) throws Exception {
        System.out.println("==========================================================================");
        System.out.println("BÀI TẬP 6: THỰC NGHIỆM SO SÁNH HIỆU NĂNG TCP VÀ UDP (1000 THÔNG ĐIỆP x 5 LẦN)");
        System.out.println("==========================================================================");
        System.out.println("Thông tin môi trường thử nghiệm:");
        System.out.println("- Hệ điều hành       : " + System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")");
        System.out.println("- Java Runtime       : JDK " + System.getProperty("java.version") + " (" + System.getProperty("java.vendor") + ")");
        System.out.println("- Kích thước payload : " + SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8).length + " bytes / thông điệp");
        System.out.println("- Số lượng thông điệp: " + NUM_MESSAGES + " messages / lượt");
        System.out.println("- Cơ chế đo thời gian: System.nanoTime() độ chính xác nanosecond");
        System.out.println("- Socket Timeout     : " + TIMEOUT_MS + " ms");
        System.out.println("--------------------------------------------------------------------------\n");

        // Khởi động TCP Server ngầm
        Thread tcpServerThread = new Thread(TcpUdpBenchmark::startTcpEchoServer);
        tcpServerThread.setDaemon(true);
        tcpServerThread.start();

        // Khởi động UDP Server ngầm
        Thread udpServerThread = new Thread(TcpUdpBenchmark::startUdpEchoServer);
        udpServerThread.setDaemon(true);
        udpServerThread.start();

        Thread.sleep(800); // Chờ server lắng nghe

        long[] tcpTimes = new long[NUM_RUNS];
        int[] tcpReceived = new int[NUM_RUNS];
        long[] udpTimes = new long[NUM_RUNS];
        int[] udpReceived = new int[NUM_RUNS];

        System.out.printf("%-6s | %-15s | %-14s | %-15s | %-14s\n",
                "Lần", "TCP Thời gian", "TCP Phản hồi", "UDP Thời gian", "UDP Phản hồi");
        System.out.println("--------------------------------------------------------------------------");

        for (int run = 0; run < NUM_RUNS; run++) {
            // 1. Chạy đo TCP
            long startTcp = System.nanoTime();
            int countTcp = runTcpBenchmark();
            long endTcp = System.nanoTime();
            tcpTimes[run] = (endTcp - startTcp) / 1_000_000; // ms
            tcpReceived[run] = countTcp;

            // 2. Chạy đo UDP
            long startUdp = System.nanoTime();
            int countUdp = runUdpBenchmark();
            long endUdp = System.nanoTime();
            udpTimes[run] = (endUdp - startUdp) / 1_000_000; // ms
            udpReceived[run] = countUdp;

            System.out.printf("Run %-2d | %8d ms      | %4d/%-4d      | %8d ms      | %4d/%-4d\n",
                    (run + 1), tcpTimes[run], tcpReceived[run], NUM_MESSAGES,
                    udpTimes[run], udpReceived[run], NUM_MESSAGES);
        }

        System.out.println("--------------------------------------------------------------------------");
        double avgTcpTime = Arrays.stream(tcpTimes).average().orElse(0);
        double avgUdpTime = Arrays.stream(udpTimes).average().orElse(0);
        double avgTcpCount = Arrays.stream(tcpReceived).average().orElse(0);
        double avgUdpCount = Arrays.stream(udpReceived).average().orElse(0);

        System.out.printf("T.BÌNH | %8.1f ms      | %4.0f/%-4d      | %8.1f ms      | %4.0f/%-4d\n",
                avgTcpTime, avgTcpCount, NUM_MESSAGES, avgUdpTime, avgUdpCount, NUM_MESSAGES);
        System.out.println("==========================================================================");
        System.out.println("NHẬN XÉT ĐÁNH GIÁ THỰC NGHIỆM:");
        System.out.println("1. Độ tin cậy: TCP đạt tỉ lệ phản hồi tuyệt đối (1000/1000 = 100%) nhờ cơ chế ACK và");
        System.out.println("   truyền lại (retransmission). UDP trên localhost cũng đạt tỷ lệ cao nhưng không cam kết.");
        System.out.println("2. Tốc độ: UDP thường có tổng thời gian nhanh hơn do không tốn overhead kiểm soát luồng,");
        System.out.println("   đánh số thứ tự gói tin và xác nhận bắt tay kết nối như TCP.");
        System.out.println("3. Ứng dụng thực tế: TCP phù hợp truyền tệp, giao dịch web, chat; UDP phù hợp VoIP,");
        System.out.println("   video streaming và game thời gian thực chấp nhận mất mát dữ liệu nhỏ.");
        System.out.println("==========================================================================\n");
    }

    private static int runTcpBenchmark() {
        int receivedCount = 0;
        try (Socket socket = new Socket("localhost", TCP_PORT);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            socket.setSoTimeout(TIMEOUT_MS);
            for (int i = 0; i < NUM_MESSAGES; i++) {
                out.println(SAMPLE_PAYLOAD);
                String resp = in.readLine();
                if (resp != null) {
                    receivedCount++;
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi TCP Benchmark: " + e.getMessage());
        }
        return receivedCount;
    }

    private static int runUdpBenchmark() {
        int receivedCount = 0;
        byte[] sendData = SAMPLE_PAYLOAD.getBytes(StandardCharsets.UTF_8);
        byte[] recvBuffer = new byte[512];

        try (DatagramSocket socket = new DatagramSocket()) {
            socket.setSoTimeout(TIMEOUT_MS);
            InetAddress serverAddr = InetAddress.getByName("localhost");

            for (int i = 0; i < NUM_MESSAGES; i++) {
                DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, serverAddr, UDP_PORT);
                socket.send(sendPacket);

                DatagramPacket recvPacket = new DatagramPacket(recvBuffer, recvBuffer.length);
                try {
                    socket.receive(recvPacket);
                    receivedCount++;
                } catch (SocketTimeoutException e) {
                    // Mất gói hoặc timeout
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi UDP Benchmark: " + e.getMessage());
        }
        return receivedCount;
    }

    private static void startTcpEchoServer() {
        try (ServerSocket server = new ServerSocket(TCP_PORT)) {
            while (true) {
                Socket client = server.accept();
                new Thread(() -> {
                    try (client;
                         BufferedReader in = new BufferedReader(new InputStreamReader(client.getInputStream(), StandardCharsets.UTF_8));
                         PrintWriter out = new PrintWriter(new OutputStreamWriter(client.getOutputStream(), StandardCharsets.UTF_8), true)) {
                        String line;
                        while ((line = in.readLine()) != null) {
                            out.println("ECHO:" + line);
                        }
                    } catch (IOException ignored) {
                    }
                }).start();
            }
        } catch (IOException e) {
            System.err.println("TCP Server Error: " + e.getMessage());
        }
    }

    private static void startUdpEchoServer() {
        byte[] buffer = new byte[512];
        try (DatagramSocket socket = new DatagramSocket(UDP_PORT)) {
            while (true) {
                DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                socket.receive(packet);
                DatagramPacket resp = new DatagramPacket(packet.getData(), packet.getLength(),
                        packet.getAddress(), packet.getPort());
                socket.send(resp);
            }
        } catch (IOException e) {
            System.err.println("UDP Server Error: " + e.getMessage());
        }
    }
}
