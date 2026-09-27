package exercise3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;

/**
 * Bài tập đề xuất 3: TCP Client tra cứu ngày giờ
 */
public class DateTimeTcpClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5003;

        try (Socket socket = new Socket(host, port);
             BufferedReader console = new BufferedReader(
                     new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.println("Đã kết nối TCP Server tại " + host + ":" + port);
            System.out.println("Các lệnh hỗ trợ: DATE, TIME, DATETIME, QUIT");

            String line;
            while ((line = console.readLine()) != null) {
                out.println(line);
                try {
                    String response = in.readLine();
                    if (response == null) {
                        System.out.println("[TCP] Server đã đóng kết nối (Connection closed by server).");
                        break;
                    }
                    System.out.println("Server: " + response);
                } catch (SocketException e) {
                    System.err.println("[TCP] Mất kết nối tới Server (Server có thể đã bị dừng đột ngột): " + e.getMessage());
                    break;
                }

                if (line.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi kết nối TCP: " + e.getMessage());
        }
    }
}
