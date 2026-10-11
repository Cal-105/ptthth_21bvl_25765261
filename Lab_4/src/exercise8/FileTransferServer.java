package exercise8;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Bài tập đề xuất 8: Truyền file TCP an toàn
 * - Client gửi metadata: FILE <filename> <size> <sha256>
 * - Server lưu file vào thư mục upload cố định (uploads/), ngăn chặn Directory Traversal (../)
 * - Đọc đúng số byte đã khai báo (không phụ thuộc EOF)
 * - Tính lại SHA-256 kiểm tra tính toàn vẹn: trả OK hoặc ERR HASH_MISMATCH
 */
public class FileTransferServer {
    private static final int PORT = 7009;
    private static final int MAX_CLIENTS = 10;
    private static final String UPLOAD_DIR = "uploads";

    public static void main(String[] args) {
        int port = args.length > 0 ? Integer.parseInt(args[0]) : PORT;

        File uploadFolder = new File(UPLOAD_DIR);
        if (!uploadFolder.exists()) {
            uploadFolder.mkdirs();
        }

        ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);
        try (ServerSocket server = new ServerSocket(port)) {
            System.out.println("Secure File Transfer Server listening on port " + port);
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
             BufferedInputStream inStream = new BufferedInputStream(socket.getInputStream());
             BufferedReader lineReader = new BufferedReader(new InputStreamReader(inStream, StandardCharsets.UTF_8));
             PrintWriter out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true)) {

            String header = lineReader.readLine();
            if (header == null) return;

            header = header.trim();
            System.out.println("[" + client + "] Header: " + header);

            if (!header.startsWith("FILE ")) {
                out.println("ERR INVALID_PROTOCOL");
                return;
            }

            String[] parts = header.split("\\s+");
            if (parts.length != 4) {
                out.println("ERR INVALID_HEADER_FORMAT");
                return;
            }

            String rawFileName = parts[1];
            long fileSize;
            try {
                fileSize = Long.parseLong(parts[2]);
            } catch (NumberFormatException e) {
                out.println("ERR INVALID_FILE_SIZE");
                return;
            }
            String expectedSha256 = parts[3].toLowerCase();

            // 1. Kiểm tra an toàn: ngăn chặn Directory Traversal (../)
            if (rawFileName.contains("..") || rawFileName.contains("/") || rawFileName.contains("\\")) {
                System.err.println("[" + client + "] Cảnh báo bảo mật: Phát hiện đường dẫn không hợp lệ: " + rawFileName);
                out.println("ERR INVALID_FILENAME (Path traversal detected)");
                return;
            }

            // Chỉ lấy tên file an toàn
            String safeFileName = Paths.get(rawFileName).getFileName().toString();
            File destFile = new File(UPLOAD_DIR, safeFileName);

            System.out.println("[" + client + "] Đang nhận file '" + safeFileName + "', kích thước: " + fileSize + " bytes");

            // 2. Nhận đúng số byte đã khai báo và tính SHA-256 cùng lúc
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (BufferedOutputStream fileOut = new BufferedOutputStream(new FileOutputStream(destFile))) {
                byte[] buffer = new byte[8192];
                long remaining = fileSize;

                while (remaining > 0) {
                    int toRead = (int) Math.min(buffer.length, remaining);
                    int bytesRead = inStream.read(buffer, 0, toRead);
                    if (bytesRead == -1) {
                        break;
                    }
                    fileOut.write(buffer, 0, bytesRead);
                    digest.update(buffer, 0, bytesRead);
                    remaining -= bytesRead;
                }

                if (remaining > 0) {
                    out.println("ERR INCOMPLETE_DATA (Thiếu " + remaining + " bytes)");
                    return;
                }
            }

            // 3. So khớp hash SHA-256
            String calculatedSha256 = HexFormat.of().formatHex(digest.digest()).toLowerCase();
            System.out.println("[" + client + "] SHA-256 tính được: " + calculatedSha256);
            System.out.println("[" + client + "] SHA-256 kỳ vọng  : " + expectedSha256);

            if (calculatedSha256.equalsIgnoreCase(expectedSha256)) {
                out.println("OK HASH_MATCH " + calculatedSha256);
                System.out.println("[" + client + "] Nhận file thành công, mã hash khớp 100%!");
            } else {
                out.println("ERR HASH_MISMATCH");
                System.err.println("[" + client + "] Lỗi: Mã hash không khớp!");
            }

        } catch (IOException | NoSuchAlgorithmException e) {
            System.err.println("[" + client + "] Lỗi xử lý truyền file: " + e.getMessage());
        } finally {
            System.out.println("Client disconnected: " + client);
        }
    }
}
