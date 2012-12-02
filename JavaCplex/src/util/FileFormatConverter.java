package util;

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

    public static void main(String[] args ) throws IOException {

        if (args.length < 1)
            return;

        convertPvmFormatToLibSVM(args[0], args[1]);
        //convertLibSVMFormatToPvm(args[0], args[1]);

    }
}
