package util;

import java.util.Random;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 12/1/12
 * Time: 4:08 PM
 * To change this template use File | Settings | File Templates.
 */
public class RandomUtils {

    public static void doShuffle(int [] src){
        doShuffle(src, 1);
    }

    public static void doShuffle(int [] src, int passes){
        int i, j, temp, passIdx;
        Random r = new Random();

        if (passes < 1)
            return;

        for (passIdx = 0; passIdx < passes; passIdx++)
            for (i = 0; i < src.length; i++){
                j = r.nextInt(src.length);

                temp = src[i];
                src[i] = src[j];
                src[j] = temp;
            }
    }
}
