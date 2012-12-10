package util.converters;

import cgl.imr.types.DoubleVectorData;
import dsolve.LocalSolver;
import ilog.concert.IloException;
import pvm.DatabaseLoader;
import pvm.PvmEntry;

import java.io.*;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 12/1/12
 * Time: 5:57 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileFormatConverter {
    public static void convertPvmFormatToLibSVM(String fileNameInput, String fileNameOutput) throws IOException {
        int i;
        ArrayList<PvmEntry> entries = DatabaseLoader.loadEntries(fileNameInput);
        String line;

        File f = new File(fileNameOutput);

        if (!f.exists())
            f.createNewFile();

        FileWriter fstream = new FileWriter(f.getAbsoluteFile());
        PrintWriter out = new PrintWriter(fstream);

        for (PvmEntry rec : entries){

            if (rec.label)
                out.print("+1");
            else
                out.print("-1");

            line = "";
            for (i = 0; i < rec.x.length; i++)
                line += String.format(" %d:%8f", i + 1, rec.x[i]);

            out.print(line);
            out.println();
        }

        out.close();
    }

    public static void convertLibSVMFormatToPvm(String fileNameInput, String fileNameOutput) throws IOException {
        int i;
        ArrayList<PvmEntry> records = new ArrayList<PvmEntry>();

        FileInputStream fstream = new FileInputStream(fileNameInput);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;


        File f = new File(fileNameOutput);

        if (!f.exists())
            f.createNewFile();

        FileWriter foutstream = new FileWriter(f.getAbsoluteFile());
        PrintWriter out = new PrintWriter(foutstream);
        String cline;


        while ((strLine = br.readLine()) != null) {
            PvmEntry record = new PvmEntry();
            String[] pipeSplit = strLine.split(" ");

            record.label = true;

            int labelInt = Integer.parseInt(pipeSplit[0]);
            if (labelInt == -1)
                record.label = false;

            if (record.label)
                cline = "1|";
            else
                cline = "0|";

            record.x = new double[pipeSplit.length - 1];

            for (i = 1; i < pipeSplit.length; i++){

                String[] localSplit = pipeSplit[i].split(":");

                if (localSplit.length != 2)
                    continue;

                int featIdx = Integer.parseInt(localSplit[0]);

                if (featIdx != i)
                    continue;

                cline += localSplit[1] + ",";
            }

            out.print(cline);
            out.println();
        }

        in.close();
        out.close();
    }

    public static void convertNumericalAndCategoricalToPvmFormat(String fileNameInput, String fileNameOutput) throws IOException {
        convertNumericalAndCategoricalToPvmFormat(fileNameInput, fileNameOutput, ",", true);
    }

    public static ArrayList<String[]> splitFileToStrings(String fileNameInput, String separator) throws IOException {
        FileInputStream fstream = new FileInputStream(fileNameInput);
        DataInputStream in = new DataInputStream(fstream);
        BufferedReader br = new BufferedReader(new InputStreamReader(in));
        String strLine;

        ArrayList<String[]> ret = new ArrayList<String[]>();


        while ((strLine = br.readLine()) != null) {
            ret.add(strLine.split(separator));
        }

        return ret;
    }

    public static void convertNumericalAndCategoricalToPvmFormat(String fileNameInput, String fileNameOutput, String separator, boolean firstValueIsClass) throws IOException {
        //this will parse the txt and split each line by the separator
        //missing values will be replaced by the average of that class
        //categorical values will be replaced by conjunctions of booleans

        int i;
        ArrayList<String []> strings = splitFileToStrings(fileNameInput, separator);
        FileStringDescription stringDescription = new FileStringDescription();
        stringDescription.setArrayOfString(strings);
        String pvmStrings[][] = stringDescription.getPvmDescription(firstValueIsClass);

        File f = new File(fileNameOutput);

        if (!f.exists())
            f.createNewFile();

        FileWriter foutstream = new FileWriter(f.getAbsoluteFile());
        PrintWriter out = new PrintWriter(foutstream);

        for (String [] cRecord : pvmStrings)
        {
            out.print(cRecord[0] + "|");

            for (i = 1; i < cRecord.length; i++)
                out.print(cRecord[i] + ",");

            out.println();
        }

        out.close();
    }

    public static void callConversion(String[] args) throws IOException {
        int i;

        assert (args.length > 2);

        for (i = 2; i < args.length; i++)
            args[i] = args[i].toLowerCase();

        if (args[2].contentEquals("ptol")){
            convertPvmFormatToLibSVM(args[0], args[1]);
            return;
        }

        if (args[2].contentEquals("ltop")){
            convertLibSVMFormatToPvm(args[0], args[1]);
            return;
        }

        if (args[2].contentEquals("gtop")){
            if (args.length == 3){
                convertNumericalAndCategoricalToPvmFormat(args[0], args[1]);
                return;
            }
            else if (args.length == 5){
                convertNumericalAndCategoricalToPvmFormat(args[0], args[1], args[3], args[4].contentEquals("0"));
                return;
            }
        }

        printUsage();
    }

    public static void printUsage(){

    }

    public static void main(String[] args ) throws IOException {

        if (args.length < 3)
            return;

        convertNumericalAndCategoricalToPvmFormat(args[0], args[1]);
    }
}
