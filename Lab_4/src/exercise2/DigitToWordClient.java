package exercise2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Bài tập đề xuất 2: TCP Client gửi chữ số và nhận cách đọc tiếng Việt
 */
public class DigitToWordClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 5002;

        try (Socket socket = new Socket(host, port);
             BufferedReader console = new BufferedReader(
                     new InputStreamReader(System.in, StandardCharsets.UTF_8));
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            System.out.println("Đã kết nối tới DigitToWord Server tại " + host + ":" + port);
            System.out.println("Nhập chữ số (0-9) để nhận cách đọc tiếng Việt, hoặc 'QUIT' để thoát:");

            String line;
            while ((line = console.readLine()) != null) {
                out.println(line);
                String response = in.readLine();
                if (response == null) {
                    System.out.println("Server đã đóng kết nối.");
                    break;
                }
                System.out.println("Server trả lời: " + response);

                if (line.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (NumberFormatException e) {
            System.err.println("Lỗi: Cổng port phải là số nguyên.");
        } catch (IOException e) {
            System.err.println("Lỗi kết nối tới Server: " + e.getMessage());
        }
    }
}
