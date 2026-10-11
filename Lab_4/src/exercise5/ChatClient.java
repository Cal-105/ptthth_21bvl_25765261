package exercise5;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

/**
 * Bài tập đề xuất 5: Chat TCP Client
 * - Luồng nhận tin: chạy ngầm để liên tục nhận broadcast từ Server
 * - Luồng chính: đọc bàn phím và gửi lệnh lên Server (NICK, USERS, MSG, QUIT)
 */
public class ChatClient {
    public static void main(String[] args) {
        String host = args.length > 0 ? args[0] : "localhost";
        int port = args.length > 1 ? Integer.parseInt(args[1]) : 7005;

        try {
            Socket socket = new Socket(host, port);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            PrintWriter out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
            BufferedReader console = new BufferedReader(
                    new InputStreamReader(System.in, StandardCharsets.UTF_8));

            System.out.println("Đã kết nối tới Chat Server tại " + host + ":" + port);
            System.out.println("Hướng dẫn lệnh:");
            System.out.println("  NICK <nickname> : Đặt biệt danh");
            System.out.println("  USERS           : Xem danh sách thành viên online");
            System.out.println("  MSG <nội dung>  : Gửi tin nhắn đến mọi người");
            System.out.println("  QUIT            : Thoát ứng dụng");
            System.out.println("--------------------------------------------------");

            // Luồng nhận tin nhắn bất đồng bộ từ server
            Thread receiverThread = new Thread(() -> {
                try {
                    String serverMsg;
                    while ((serverMsg = in.readLine()) != null) {
                        System.out.println(serverMsg);
                    }
                } catch (IOException e) {
                    // Socket đóng khi thoát
                }
            });
            receiverThread.setDaemon(true);
            receiverThread.start();

            // Luồng chính đọc bàn phím
            String line;
            while ((line = console.readLine()) != null) {
                out.println(line);
                if (line.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }
            }

            socket.close();
            System.out.println("Đã đóng kết nối chat.");
        } catch (IOException e) {
            System.err.println("Lỗi Chat Client: " + e.getMessage());
        }
    }
}
