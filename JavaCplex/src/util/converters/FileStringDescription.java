package util.converters;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 12/8/12
 * Time: 3:40 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileStringDescription {
    ArrayList<String[]> srcStrings = null;

    int featuresCount = 0;
    double [] numericalAverages = null;
    int [] numericalCounts = null;
    ArrayList<ArrayList<String>> categoricals = null;

    public ArrayList<String[]> getArrayOfStrings(){return srcStrings;}

    public void setArrayOfString(ArrayList<String[]> srcStrings){this.srcStrings = srcStrings;}

    protected void initVectors(){

        assert (featuresCount > 0);

        numericalAverages = new double[featuresCount];
        numericalCounts = new int[featuresCount];
        categoricals = new ArrayList<ArrayList<String>>(featuresCount);

        for (int i = 0; i < featuresCount; i++)
            categoricals.add(null);
    }
    protected int computeFeaturesCount(ArrayList<String[]> strings){

        featuresCount = 0;

        for (String[] rec : strings){
            if (featuresCount < rec.length)
                featuresCount = rec.length;
        }

        return featuresCount;
    }

    protected ArrayList<String> addCategoricalRecordFeature(int featIdx, String recFeat){
        ArrayList<String> cCateg = categoricals.get(featIdx);

        if (recFeat.length() < 1)
            return null;

        if (categoricals.get(featIdx) == null){
            cCateg = new ArrayList<String>(1);
            categoricals.remove(featIdx);
            categoricals.add(featIdx, cCateg);
        }

        for (String featStr : cCateg){
            if (featStr.contentEquals(recFeat))
                return cCateg;
        }

        cCateg.add(recFeat);
        return cCateg;
    }

    protected void getStringCategoricalAndAverages(boolean labelFirst) throws IOException {
        int i, j, first_i = 0, last_i, labelIdx;
        double temp;

        if (featuresCount == 0)
            computeFeaturesCount(srcStrings);

        initVectors();

        if (featuresCount < 1)
            throw new IOException("Source strings contain too few features");

        last_i = featuresCount;
        if (labelFirst){
            labelIdx = 0;
            first_i++;
        }
        else{
            last_i--;
            labelIdx = last_i;
        }

        for (String [] rec : srcStrings){
            addCategoricalRecordFeature(labelIdx, rec[labelIdx]);

            for (i = first_i; i < last_i; i++){

                if (rec[i].length() < 1)
                    continue;

                try {
                    temp = Double.parseDouble(rec[i]);

                    numericalAverages[i] += temp;
                    numericalCounts[i]++;
                } catch (NumberFormatException e) {
                    addCategoricalRecordFeature(i, rec[i]);
                }
            }

        }

        computeAverages();
    }
    protected void computeAverages(){
        int i;
        for (i = 0; i < numericalAverages.length; i++)
            if (numericalCounts[i] > 0)
                numericalAverages[i] /= (double)numericalCounts[i];
    }

    protected int computeFinalFeatureCount(boolean labelFirst){
        int i = 0, last_i = featuresCount;
        int totalFeatCount = 0;

        if (labelFirst)
            i = 1;
        else
            last_i--;

        for (; i < last_i; i++){
            if (featureIsEmpty(i))
                continue;

            if (categoricals.get(i)!= null && categoricals.get(i).size() > 0)
            {
                if (categoricals.get(i).size() == 2)
                    totalFeatCount++;
                else
                    totalFeatCount += categoricals.get(i).size();
            }
            else
            {
                assert (numericalCounts[i] > 0);

                totalFeatCount++;
            }
        }

        return totalFeatCount;
    }

    protected boolean featureIsEmpty(int featIdx){
        return (numericalCounts[featIdx] < 2 && (categoricals.get(featIdx) == null || categoricals.get(featIdx).size() < 2));
    }

    protected int getCategoricalIndex(String str, int featIdx) throws IOException{
        int i;

        if (categoricals.get(featIdx) == null)
            throw new IOException("Categorical attribute should have been queried");


        for (i = 0; i < categoricals.get(featIdx).size(); i++)
            if (categoricals.get(featIdx).get(i).contentEquals(str))
                return i;

        return -1;
    }

    protected int fillMissingFeature(int featIdx, int strIdx, String [] destStrings){
        assert (!featureIsEmpty(featIdx));

        if (numericalCounts[featIdx] > 0){
            destStrings[strIdx] = String.valueOf(numericalAverages[featIdx]);
            strIdx++;
        }
        else{
            assert (categoricals.get(featIdx) != null && categoricals.get(featIdx).size() > 1);
            if (categoricals.get(featIdx).size() == 2){
                destStrings[strIdx] = String.valueOf(0.5);
                strIdx++;
            }
            else
                for (int j = 0; j < categoricals.get(featIdx).size(); j++){
                    destStrings[strIdx] = String.valueOf(0.5);
                    strIdx++;
                }
        }

        return strIdx;
    }

    protected int fillCategoricalFeature(int featIdx, int strIdx, String [] destStrings, String srcStr) throws IOException{
        int cidx = getCategoricalIndex(srcStr, featIdx);

        if (!(numericalCounts[featIdx] == 0 && categoricals.get(featIdx) != null))
            throw new IOException("Non categorical feature");

        int categSize = categoricals.get(featIdx).size();
        if (!(categSize > 1 && cidx >= 0 && cidx < categSize))
            throw new IOException("Empty feature used");

        if (categSize == 2){
            destStrings[strIdx] = String.valueOf(cidx);
            strIdx++;
        }
        else {
            int i;

            for (i = 0; i < cidx; i++, strIdx++)
                destStrings[strIdx] = "0";

            destStrings[strIdx] = "1"; strIdx++; i++;

            for (; i < categSize; i++, strIdx++)
                destStrings[strIdx] = "0";
        }

        return strIdx;
    }

    protected String[] getPvmDescriptionSingleRecord(String srcStr[], boolean labelFirst, int totalFeatureCount) throws IOException {
        int i, j, cidx, categIdx, last_i = featuresCount;
        assert (totalFeatureCount > 0 && featuresCount == srcStr.length);
        String retStrings[] = new String[totalFeatureCount + 1];

        if (labelFirst){
            i = 1;
            retStrings[0] = String.valueOf(getCategoricalIndex(srcStr[0], 0));
        }
        else{
            i = 0;
            last_i--;
            retStrings[0] = String.valueOf(getCategoricalIndex(srcStr[last_i], last_i));
        }

        cidx = 1;

        for (; i < last_i; i++){
            if (featureIsEmpty(i))
                continue;

            if (srcStr[i].length() == 0){
                cidx = fillMissingFeature(i, cidx, retStrings);
                continue;
            }

            if (categoricals.get(i) != null)
                cidx = fillCategoricalFeature(i, cidx, retStrings, srcStr[i]);
            else{
                retStrings[cidx] = srcStr[i];
                cidx++;
            }
        }

        return retStrings;
    }

    public String[][] getPvmDescription(boolean labelFirst) throws IOException {
        int i;

        getStringCategoricalAndAverages(labelFirst);

        int totalFeatureCount = computeFinalFeatureCount(labelFirst);
        String retStrings[][] = new String[srcStrings.size()][];

        i = 0;
        for (String [] srcStr : srcStrings){
            retStrings[i] = getPvmDescriptionSingleRecord(srcStr, labelFirst, totalFeatureCount);
            i++;
        }

        return retStrings;
    }
}
