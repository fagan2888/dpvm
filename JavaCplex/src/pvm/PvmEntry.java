package pvm;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/25/12
 * Time: 7:59 PM
 */

public class PvmEntry {
    public boolean label = false;
    public double [] x = null;

    public void resize( int nsize ) {
        int i;
        double [] nx = new double[nsize];

        for (i = 0; i < x.length; i++)
            nx[i] = x[i];

        x = nx;
    }
}
