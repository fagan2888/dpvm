package dsolve.lfs;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/3/12
 * Time: 5:48 PM
 */

public class LfsConstraint {

    public static double UNDEFINED_BOUND = Double.MAX_VALUE;

    private double    lowerBound, higherBound;
    private int []    index;
    private double [] terms;
    private int       size;
    private double    norm = -1;
    private String    name;

    public LfsConstraint() {}

    public LfsConstraint( int arraySize ) {
        index  = new int[arraySize];
        terms  = new double[arraySize];
        setSize(arraySize);
    }

    public double getLowerBound() {
        return lowerBound;
    }

    public void setLowerBound( double bound ) {
        lowerBound = bound;
    }

    public double getHigherBound() {
        return higherBound;
    }

    public void setUpperBound(double bound) {
        higherBound = bound;
    }

    public int[] getIndex() {
        return index;
    }

    public void setIndex( int[] index ) {
        this.index = index;
    }

    public double [] getTerms() {
        return terms;
    }

    public void setTerms( double [] terms ) {
        this.terms = terms;
    }

    public void sortIndex() {
        boolean changed;
        int startIndex = 0;
        do {
            changed = false;
            for( int i = startIndex; i < index.length; i++ ) {
                for( int j = i+1; j < index.length; j++ ) {
                    if ( index[i] > index[j] ) {
                        changed = true;
                        int indexAux = index[i];double termAux = terms[i];
                        index[i] = index[j]; terms[i] = terms[j];
                        index[j] = indexAux; terms[j] = termAux;
                    }
                }
                if ( !changed ) {  startIndex ++; }
            }
        } while ( changed );
    }

    public int getSize() {
        return size;
    }

    public void setSize(int size) {
        this.size = size;
    }

    public void setName(String name){
        this.name = name;
    }

    public String getName(){
        return name;
    }


    public void computeNorm(){
        int i;

        norm = 0;
        for (i = 0; i < terms.length; i++)
            norm += terms[i] * terms[i];

        norm = Math.sqrt(norm);
    }

    public double getNorm(){

        if (norm < 0)
            computeNorm();
        return norm;
    }
}
