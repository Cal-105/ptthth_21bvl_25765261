package exercise3;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bài tập đề xuất 3: Dịch vụ ngày giờ qua giao thức TCP
 * Hỗ trợ các lệnh: DATE, TIME, DATETIME, QUIT.
 * Sử dụng DateTimeFormatter khuôn dạng dd/MM/yyyy và HH:mm:ss.
 */
public class DateTimeTcpServer {
    private static final int PORT = 5003;
    private static final int MAX_CLIENTS = 20;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("DateTime TCP Server listening on port " + port);
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
        String client = String.valueOf(socket.getRemoteSocketAddress());
        System.out.println("Client connected: " + client);

        try (socket;
             BufferedReader in = new BufferedReader(
                     new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(
                     new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String request;
            while ((request = in.readLine()) != null) {
                System.out.println("[" + client + "] Request: " + request);
                String response = process(request);
                out.println(response);
                System.out.println("[" + client + "] Response: " + response);

                if (request.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Lỗi phiên làm việc với client " + client + ": " + e.getMessage());
        } finally {
            System.out.println("Client disconnected: " + client);
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
            case "QUIT":
                return "OK BYE";
            default:
                return "ERR UNKNOWN_COMMAND";
        }
    }
}
