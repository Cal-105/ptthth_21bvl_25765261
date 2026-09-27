package pttth_21bvl_25765261;

import static java.lang.Thread.*;

import java.io.IOException;
import java.io.InputStream;

public class InStream2 {

    public static void main(String[] args) throws InterruptedException, IOException {
        InputStream is = System.in;

        try {
            while (true) {
                // Kiểm tra xem có bao nhiêu byte dữ liệu có sẵn mà không bị chặn
                if (is.available() > 0) {
                    byte[] buffer = new byte[is.available()];
                    int bytesRead = is.read(buffer);

                    if (bytesRead == -1) {
                        break; // Kết thúc input stream
                    }

                    String str = new String(buffer, 0, bytesRead);
                    System.out.print(str); // In ra dữ liệu đọc được
                } else {
                    // Nếu không có dữ liệu sẵn có, in một ký tự '.' và đợi một chút
                    System.out.print(".");
                    sleep(100); // Ngừng một chút để giảm tải CPU
                }
            }
        } catch (IOException e) {
        }
    }
}