package exercise3;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Bài tập đề xuất 3: UDP Client tra cứu ngày giờ
 * Có cơ chế Timeout 3 giây để tránh treo vô hạn khi Server dừng.
 */
public class DateTimeUdpClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5004;

        try (DatagramSocket socket = new DatagramSocket();
             BufferedReader console = new BufferedReader(
                     new InputStreamReader(System.in, StandardCharsets.UTF_8))) {

            socket.setSoTimeout(3000); // 3 giây timeout
            InetAddress serverAddress = InetAddress.getByName(host);

            System.out.println("Sẵn sàng gửi UDP tới " + host + ":" + port);
            System.out.println("Các lệnh hỗ trợ: DATE, TIME, DATETIME (hoặc gõ 'exit' để thoát client):");

            String line;
            byte[] receiveBuffer = new byte[4096];

            while ((line = console.readLine()) != null) {
                if (line.trim().equalsIgnoreCase("exit")) {
                    System.out.println("Thoát client UDP.");
                    break;
                }

                byte[] sendData = line.getBytes(StandardCharsets.UTF_8);
                DatagramPacket sendPacket = new DatagramPacket(
                        sendData, sendData.length, serverAddress, port);
                socket.send(sendPacket);

                DatagramPacket receivePacket = new DatagramPacket(receiveBuffer, receiveBuffer.length);
                try {
                    socket.receive(receivePacket);
                    String response = new String(
                            receivePacket.getData(),
                            receivePacket.getOffset(),
                            receivePacket.getLength(),
                            StandardCharsets.UTF_8);
                    System.out.println("Server trả lời: " + response);
                } catch (SocketTimeoutException e) {
                    System.err.println("[UDP Timeout] Hết 3 giây không nhận được phản hồi từ server! (Server có thể chưa chạy hoặc đã dừng)");
                }
            }
        } catch (Exception e) {
            System.err.println("Lỗi UDP Client: " + e.getMessage());
        }
    }
}
