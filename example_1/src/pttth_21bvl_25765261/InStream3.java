package pttth_21bvl_25765261;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

public class InStream3 {

    public static void main(String[] args) {
        InputStream is = System.in;
        InputStreamReader isr = new InputStreamReader(is);
        BufferedReader br = new BufferedReader(isr);

        while (true) {
            try {
                String line = br.readLine();

                if (line == null)
                    break;

                System.out.println(line);

            } catch (IOException e) {
                System.out.println(e);
            }
        }
    }
}