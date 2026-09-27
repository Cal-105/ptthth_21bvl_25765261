package pttth_21bvl_25765261;

import java.io.IOException;
import java.io.InputStream;

public class InStream1 {

    public static void main(String[] args) {
        InputStream is = System.in;

        while (true) {
            try {
                int ch = is.read();

                if (ch == -1 || ch == 'q') {
                    break;
                }

                System.out.println((char) ch);

            } catch (IOException ioe) {
                System.out.println(ioe);
            }
        }
    }
}