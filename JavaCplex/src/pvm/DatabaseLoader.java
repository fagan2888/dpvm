package pvm;

import java.io.*;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 10/28/12
 * Time: 4:52 PM
 */

public class DatabaseLoader {

    public static ArrayList<PvmEntry> loadEntries(String filePath) throws IOException {

        ArrayList<PvmEntry> records = new ArrayList<PvmEntry>();

        FileInputStream fstream = new FileInputStream(filePath);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;

        //Read File Line By Line
        while ((strLine = br.readLine()) != null) {
            PvmEntry record = new PvmEntry();
            String[] pipeSplit = strLine.split("\\|");

            record.label = false;
            int labelInt = Integer.parseInt(pipeSplit[0]);
            if ( labelInt == 1 ) {
                record.label = true;
            }

            String[] features = pipeSplit[1].split(",");
            double[] featureVector = new double[features.length];
            int idx = 0;
            for (String entry : features) {
                double entryDouble = Double.parseDouble(entry);
                featureVector[idx++] = entryDouble;
            }
            record.x = featureVector;
            records.add( record );
        }
        //Close the input stream
        in.close();

        return records;
    }

    public static void main( String[] args ) throws IOException {
        String ss = "/work/cplex-pvm/dbs/mushroom/agaricus-lepiota.data.mean";
        ArrayList<PvmEntry> records = DatabaseLoader.loadEntries(ss);
        for ( PvmEntry entry : records ) {
            String featsString = "";
            for ( double feat : entry.x ) {
                featsString += String.format("%.02f,", feat);
            }
            System.out.println( String.format("label=%b; feats=%s", entry.label, featsString));
        }
    }
}
