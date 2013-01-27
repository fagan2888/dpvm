package pvm;

import java.util.Vector;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 10/25/12
 * Time: 7:59 PM
 */

public class PvmEntry implements Comparable{
    public boolean label = false;
    public double [] x = null;
    public double compareScore;

    public void resize( int nsize ) {
        int i;
        double [] nx = new double[nsize];

        if (x != null)
            for (i = 0; i < x.length; i++)
                nx[i] = x[i];

        x = nx;
    }

    @Override
    public int compareTo(Object o) {
        if (o.getClass() != PvmEntry.class)
            return 1;

        PvmEntry other = (PvmEntry)o;

        if (compareScore < other.compareScore)
            return -1;
        else if (compareScore > other.compareScore)
            return 1;
        return 0;
    }
}
