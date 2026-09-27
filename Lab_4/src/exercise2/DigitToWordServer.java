package exercise2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bài tập đề xuất 2: TCP Server đổi chữ số thành chữ tiếng Việt
 * Giao thức dòng, mã hóa UTF-8.
 * Hỗ trợ đa client đồng thời qua Thread Pool.
 */
public class DigitToWordServer {
    private static final int PORT = 5002;
    private static final int MAX_CLIENTS = 20;

    private static final String[] DIGIT_WORDS = {
        "không", "một", "hai", "ba", "bốn",
        "năm", "sáu", "bảy", "tám", "chín"
    };

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("DigitToWord TCP Server listening on port " + port);
            while (true) {
                Socket socket = server.accept();
                pool.submit(() -> handleClient(socket));
            }
        } catch (IOException e) {
            System.err.println("Server error: " + e.getMessage());
        } finally {
            pool.shutdown();
        }
    }

    private static void handleClient(Socket socket) {
        String clientAddress = String.valueOf(socket.getRemoteSocketAddress());
        System.out.println("Client connected: " + clientAddress);

        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[" + clientAddress + "] Request: '" + line + "'");
                String response = process(line);
                out.println(response);
                System.out.println("[" + clientAddress + "] Response: " + response);

                if (line.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Connection error with client " + clientAddress + ": " + e.getMessage());
        } finally {
            System.out.println("Client disconnected: " + clientAddress);
        }
    }

    public static String process(String request) {
        if (request == null) {
            return "ERR INVALID_DIGIT";
        }

        // Lệnh thoát
        if (request.trim().equalsIgnoreCase("QUIT")) {
            return "OK BYE";
        }

        // Yêu cầu: dữ liệu phải đúng một ký tự từ '0' đến '9'
        // Không chấp nhận chuỗi rỗng, độ dài khác 1, ký tự không phải số hoặc có khoảng trắng
        if (request.length() == 1 && Character.isDigit(request.charAt(0))) {
            int digit = request.charAt(0) - '0';
            return "OK " + DIGIT_WORDS[digit];
        }

        return "ERR INVALID_DIGIT";
    }
}
