package dsolve.lfs;

import org.apache.commons.lang3.Validate;
import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/3/12
 * Time: 5:54 PM
 */

public class LfsReader {

    BufferedReader reader = null;

    public LfsReader( String filePath ) throws IOException {

        FileInputStream fstream = new FileInputStream( filePath );
        DataInputStream in = new DataInputStream( fstream );
        reader = new BufferedReader( new InputStreamReader( in ) );
    }

    public LfsConstraint readConstraint() throws IOException {
        String strLine;

        strLine = reader.readLine();
        if ( strLine == null ) {
            reader.close();
            return null;
        }

        String[] pipeSplit = strLine.split("\\|");
        Validate.isTrue( pipeSplit.length == 3, "The correct line format is \"lowerBound|higerBound|idx1:term1,idx2:term2,...,idxn:termn\"" );

        double lowerBound, higerBound;
        if ( pipeSplit[0].equals( "U" ) ) { lowerBound = -Double.MAX_VALUE; }
        else { lowerBound = Double.parseDouble( pipeSplit[0] ); }

        if ( pipeSplit[1].equals("U") ) { higerBound = Double.MAX_VALUE; }
        else { higerBound = Double.parseDouble( pipeSplit[1] ); }

        String[] termsList = pipeSplit[2].split( "," );
        Validate.isTrue( termsList.length > 0, "Invalid line; must contain at least one valid constraint: " + strLine );

        LfsConstraint constraint = new LfsConstraint( termsList.length );
        constraint.setLowerBound( lowerBound );
        constraint.setUpperBound(higerBound);

        int idx = 0;
        int []     index = constraint.getIndex();
        double  [] terms = constraint.getTerms();

        for ( String termStr : termsList ) {
            String [] pair = termStr.split( ":" );
            index[idx] = Integer.parseInt( pair[0] );
            terms[idx] = Double.parseDouble( pair[1] );
	        idx ++;
        }

        constraint.computeNorm();

        return constraint;
    }
}
