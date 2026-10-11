package exercise4;

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
 * Bài tập đề xuất 4: Máy tính từ xa (TCP Remote Calculator Server)
 * Giao thức:
 * - Yêu cầu: CALC <toán_tử> <toán_hạng_1> <toán_hạng_2>
 * - Phản hồi: OK <kết_quả> hoặc ERR <mã_lỗi>
 * - Mã lỗi: DIVIDE_BY_ZERO, UNSUPPORTED_OPERATOR, INVALID_NUMBER, INVALID_FORMAT
 * - Lệnh QUIT: OK BYE
 */
public class RemoteCalculatorServer {
    private static final int PORT = 7004;
    private static final int MAX_CLIENTS = 20;

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;
        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);

        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Remote Calculator Server listening on port " + port);
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

            String line;
            while ((line = in.readLine()) != null) {
                System.out.println("[" + client + "] Request: " + line);
                String response = process(line);
                out.println(response);
                System.out.println("[" + client + "] Response: " + response);

                if (line.trim().equalsIgnoreCase("QUIT")) {
                    break;
                }
            }
        } catch (IOException e) {
            System.err.println("Client " + client + " connection error: " + e.getMessage());
        } finally {
            System.out.println("Client disconnected: " + client);
        }
    }

    public static String process(String request) {
        if (request == null) {
            return "ERR INVALID_FORMAT";
        }
        String trimmed = request.trim();
        if (trimmed.equalsIgnoreCase("QUIT")) {
            return "OK BYE";
        }

        if (!trimmed.startsWith("CALC")) {
            return "ERR UNKNOWN_COMMAND";
        }

        String[] parts = trimmed.split("\\s+");
        // CALC <toán_tử> <toán_hạng_1> <toán_hạng_2> => 4 phần
        if (parts.length != 4) {
            return "ERR INVALID_FORMAT";
        }

        String operator = parts[1];
        double num1;
        double num2;

        try {
            num1 = Double.parseDouble(parts[2]);
            num2 = Double.parseDouble(parts[3]);
        } catch (NumberFormatException e) {
            return "ERR INVALID_NUMBER";
        }

        switch (operator) {
            case "+":
                return formatResult(num1 + num2);
            case "-":
                return formatResult(num1 - num2);
            case "*":
                return formatResult(num1 * num2);
            case "/":
                if (num2 == 0.0) {
                    return "ERR DIVIDE_BY_ZERO";
                }
                return formatResult(num1 / num2);
            default:
                return "ERR UNSUPPORTED_OPERATOR";
        }
    }

    private static String formatResult(double result) {
        if (result == (long) result) {
            return "OK " + (long) result;
        } else {
            return "OK " + result;
        }
    }
}
