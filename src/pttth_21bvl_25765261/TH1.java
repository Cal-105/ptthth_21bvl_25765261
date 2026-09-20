package pttth_21bvl_25765261;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public class TH1 {

    public static void main(String[] args) {

        // Tạo BufferedReader để đọc dữ liệu từ bàn phím
        BufferedReader reader = new BufferedReader(
                new InputStreamReader(System.in, StandardCharsets.UTF_8));

        // Biến đếm số dòng đã nhập
        int count = 0;

        System.out.println("Nhập văn bản; nhập q để kết thúc:");

        try {

            while (true) {

                // Đọc một dòng từ bàn phím
                String line = reader.readLine();

                // Nếu gặp EOF hoặc nhập q thì kết thúc
                if (line == null || line.equalsIgnoreCase("q")) {
                    break;
                }

                // Tăng số dòng
                count++;

                // Hiển thị dòng vừa nhập
                System.out.printf("Dòng %d: %s%n", count, line);
            }

        } catch (IOException e) {

            System.err.println("Không thể đọc dữ liệu: " + e.getMessage());
        }

        // Hiển thị tổng số dòng
        System.out.println("Tổng số dòng đã nhập: " + count);
    }
}
