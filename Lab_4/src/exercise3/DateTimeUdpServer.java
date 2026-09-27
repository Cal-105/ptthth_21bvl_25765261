package exercise3;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * Bài tập đề xuất 3: Dịch vụ ngày giờ qua giao thức UDP
 * Hỗ trợ các lệnh: DATE, TIME, DATETIME.
 * Sử dụng DateTimeFormatter khuôn dạng dd/MM/yyyy và HH:mm:ss.
 */
public class DateTimeUdpServer {
    private static final int PORT = 5004;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;
        byte[] buffer = new byte[4096];

        try (DatagramSocket socket = new DatagramSocket(port)) {
            System.out.println("DateTime UDP Server listening on port " + port);
            while (true) {
                DatagramPacket requestPacket = new DatagramPacket(buffer, buffer.length);
                socket.receive(requestPacket);

                String request = new String(requestPacket.getData(),
                        requestPacket.getOffset(), requestPacket.getLength(),
                        StandardCharsets.UTF_8);

                System.out.println("Received from " + requestPacket.getAddress() + ":" + requestPacket.getPort()
                        + " -> " + request);

                String response = process(request);
                byte[] responseBytes = response.getBytes(StandardCharsets.UTF_8);

                DatagramPacket responsePacket = new DatagramPacket(
                        responseBytes, responseBytes.length,
                        requestPacket.getAddress(), requestPacket.getPort());
                socket.send(responsePacket);
            }
        } catch (IOException e) {
            System.err.println("UDP Server error: " + e.getMessage());
        }
    }

    public static String process(String request) {
        if (request == null) {
            return "ERR UNKNOWN_COMMAND";
        }
        String cmd = request.trim().toUpperCase();
        switch (cmd) {
            case "DATE":
                return "OK " + LocalDate.now().format(DATE_FORMATTER);
            case "TIME":
                return "OK " + LocalTime.now().format(TIME_FORMATTER);
            case "DATETIME":
                return "OK " + LocalDateTime.now().format(DATETIME_FORMATTER);
            default:
                return "ERR UNKNOWN_COMMAND";
        }
    }
}
