package util.converters;

import junit.framework.Assert;
import org.junit.Before;
import org.junit.Test;
import util.converters.FileStringDescription;

import java.io.IOException;
import java.util.ArrayList;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 12/8/12
 * Time: 3:07 PM
 * To change this template use File | Settings | File Templates.
 */
public class FileStringDescriptionTest {

    private class FileStringDescriptionForTest extends FileStringDescription{
        public int computeFeaturesCountForTest(ArrayList<String[]> strings){return computeFeaturesCount(strings);}
        public void computeAveragesForTest(){computeAverages();}
        public void getStringCategoricalAndAveragesForTest(boolean labelFirst) throws IOException {getStringCategoricalAndAverages(labelFirst);}


        public int getFeaturesCount(){return featuresCount;}

        public void setAverages(double srcAverages[]){numericalAverages = srcAverages;}
        public double[] getAverages(){return numericalAverages;}
        public void setCounts(int []srcCounts){numericalCounts = srcCounts;}
        public int[] getCounts(){return numericalCounts;}
        public ArrayList<ArrayList<String>> getCategoricals(){return categoricals;}
    }

    @Test
    public void test_ComputeFeaturesCount_GetsFeatureCountForSimpleInput(){
        ArrayList<String[]> strings = new ArrayList<String[]>(2);
        String [] cStrings = {"0", "1"};
        strings.add(cStrings);

        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        tested.computeFeaturesCountForTest(strings);

        Assert.assertEquals(tested.getFeaturesCount(), 2);

        String [] nStrings = {"0", "1", "2"};
        strings.add(nStrings);

        tested.computeFeaturesCountForTest(strings);
        Assert.assertEquals(tested.getFeaturesCount(), 3);
    }

    @Test
    public void test_ComputeAverages(){
        int i;
        double testAverages[] = {0.1, 1, 3};
        int testCounts[] = {10, 2, 3};

        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        tested.setAverages(testAverages);
        tested.setCounts(testCounts);
        tested.computeAveragesForTest();

        double []resAverages = tested.getAverages();

        Assert.assertTrue(Math.abs(resAverages[0] - 0.01) < 1e-10);
        Assert.assertTrue(Math.abs(resAverages[1] - 0.5) < 1e-10);
        Assert.assertTrue(Math.abs(resAverages[2] - 1.0) < 1e-10);
    }

    @Test
    public void test_addCategoricalRecordFeature_DoesNotAddCategoricalOnEmptyString(){
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        strings.add(new String [] {"0", "a"});
        strings.add(new String[] {"1", "b"});
        strings.add(new String[] {"2", ""});

        tested.setArrayOfString(strings);
        tested.computeFeaturesCount(strings);
        tested.initVectors();

        Assert.assertEquals(tested.addCategoricalRecordFeature(1, ""), null);
    }

    @Test
    public void test_addCategoricalRecordFeature_CreatesNewCategoricalForNonEmptyString(){
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        strings.add(new String [] {"0", "a"});
        strings.add(new String[] {"1", "b"});
        strings.add(new String[] {"2", ""});

        tested.setArrayOfString(strings);
        tested.computeFeaturesCount(strings);
        tested.initVectors();

        ArrayList<String> categorical = tested.addCategoricalRecordFeature(1, "a");

        Assert.assertNotNull(categorical);
        Assert.assertTrue(categorical.get(0).contentEquals("a"));
    }

    @Test
    public void test_addCategoricalRecordFeature_DoesNotAddAStringTwice(){
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        strings.add(new String [] {"0", "a"});
        strings.add(new String[] {"1", "b"});
        strings.add(new String[] {"2", ""});

        tested.setArrayOfString(strings);
        tested.computeFeaturesCount(strings);
        tested.initVectors();

        tested.addCategoricalRecordFeature(0, "0");
        tested.addCategoricalRecordFeature(0, "1");
        tested.addCategoricalRecordFeature(0, "2");
        ArrayList<String> categorical = tested.addCategoricalRecordFeature(0, "1");

        for (int i = 0; i < categorical.size(); i++)
            for (int j = i + 1; j < categorical.size(); j++)
                Assert.assertTrue(!categorical.get(i).contentEquals(categorical.get(j)));
    }

    @Test
    public void test_GetStringCategoricalAndAverages_GetsCorrectCategoricals() throws IOException {

        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "a"};
        strings.add(s0);
        String s1[] = {"1", "b"};
        strings.add(s1);
        String s2[] = {"2", ""};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.computeFeaturesCount(strings);
        tested.getStringCategoricalAndAveragesForTest(false);

        ArrayList<ArrayList<String>> categoricals = tested.getCategoricals();

        Assert.assertEquals(categoricals.get(0), null);
        Assert.assertNotSame(categoricals.get(1), null);

        Assert.assertTrue(categoricals.get(1).size() == 2);
        Assert.assertTrue(categoricals.get(1).get(0).matches("a"));
        Assert.assertTrue(categoricals.get(1).get(1).matches("b"));
    }

    @Test
    public void test_GetStringCategoricalAndAverages_GetCorrectDoubleSums() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "1", "a"};
        strings.add(s0);
        String s1[] = {"1", "", "b"};
        strings.add(s1);
        String s2[] = {"2", "4", "a"};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.computeFeaturesCount(strings);
        tested.getStringCategoricalAndAveragesForTest(false);

        double sums[] = tested.getAverages();

        Assert.assertTrue(Math.abs(sums[0] - 1) < 1e-10);
        Assert.assertTrue(Math.abs(sums[1] - 2.5) < 1e-10);
    }

    @Test
    public void test_GetStringCategoricalAndAverages_GetCorrectDoubleCount() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "1", "a"};
        strings.add(s0);
        String s1[] = {"1", "", "b"};
        strings.add(s1);
        String s2[] = {"2", "4", ""};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.computeFeaturesCount(strings);
        tested.getStringCategoricalAndAveragesForTest(false);

        int counts[] = tested.getCounts();

        Assert.assertTrue(counts[0] == 3);
        Assert.assertTrue(counts[1] == 2);
    }

    @Test
    public void test_computeFinalFeatureCount_CorrectFeatureLabelFirstAndBinaryCategoricals() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "1", "a"};
        strings.add(s0);
        String s1[] = {"1", "", "b"};
        strings.add(s1);
        String s2[] = {"0", "4", ""};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(true);

        int totalFeatureCount = tested.computeFinalFeatureCount(true);

        Assert.assertEquals(totalFeatureCount, 2);
    }

    @Test
    public void test_computeFinalFeatureCount_CorrectFeatureLabelFirstAndMultinomialCategoricals() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "1", "a"};
        strings.add(s0);
        String s1[] = {"1", "", "b"};
        strings.add(s1);
        String s2[] = {"0", "4", "c"};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(true);

        int totalFeatureCount = tested.computeFinalFeatureCount(true);

        Assert.assertEquals(totalFeatureCount, 4);
    }

    @Test
    public void test_computeFinalFeatureCount_CorrectFeatureLabelLast() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "1", "a", "uu"};
        strings.add(s0);
        String s1[] = {"1", "", "b", "aa"};
        strings.add(s1);
        String s2[] = {"2", "4", "c", "uu"};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        int totalFeatureCount = tested.computeFinalFeatureCount(false);

        Assert.assertEquals(totalFeatureCount, 5);
    }

    @Test
    public void test_featureIsEmpty_EliminatesSingletons() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "1", "", "uu"};
        strings.add(s0);
        String s1[] = {"1", "", "", "aa"};
        strings.add(s1);
        String s2[] = {"2", "", "c", "uu"};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        Assert.assertTrue(tested.featureIsEmpty(1));
        Assert.assertTrue(tested.featureIsEmpty(2));
    }

    @Test
    public void test_featureIsEmpty_EliminatesEmpty() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "1", "", "uu"};
        strings.add(s0);
        String s1[] = {"1", "", "", "aa"};
        strings.add(s1);
        String s2[] = {"2", "", "", "uu"};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        Assert.assertTrue(tested.featureIsEmpty(1));
        Assert.assertTrue(tested.featureIsEmpty(2));
    }

    @Test
    public void test_featureIsEmpty_DoesNotEliminateNonEmpty() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        String s0[] = {"0", "1", "", "uu"};
        strings.add(s0);
        String s1[] = {"", "", "", "aa"};
        strings.add(s1);
        String s2[] = {"2", "", "c", "uu"};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        Assert.assertTrue(!tested.featureIsEmpty(0));
        Assert.assertTrue(!tested.featureIsEmpty(3));
    }

    @Test
    public void test_fillMissingFeature_FillsDoubleVals() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();
        String destStrings[] = new String[2];

        String s0[] = {"0", "1", "", "uu"};
        strings.add(s0);
        String s1[] = {"", "2", "", "aa"};
        strings.add(s1);
        String s2[] = {"2", "", "c", "uu"};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        int cidx = 0;

        cidx = tested.fillMissingFeature(0, cidx, destStrings);
        cidx = tested.fillMissingFeature(1, cidx, destStrings);

        Assert.assertTrue(cidx == 2);
        Assert.assertTrue(destStrings[0].contentEquals(String.valueOf(1.0)));
        Assert.assertTrue(destStrings[1].contentEquals(String.valueOf(1.5)));
    }


    @Test
    public void test_fillMissingFeature_FillsCategorical() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();
        String destStrings[] = new String[4];

        String s0[] = {"0", "1", "a", "uu"};
        strings.add(s0);
        String s1[] = {"", "2", "", "aa"};
        strings.add(s1);
        String s2[] = {"2", "", "c", "cu"};
        strings.add(s2);

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        int cidx = 0;

        cidx = tested.fillMissingFeature(2, cidx, destStrings);
        cidx = tested.fillMissingFeature(3, cidx, destStrings);

        Assert.assertTrue(cidx == 4);
        Assert.assertTrue(destStrings[0].contentEquals("0.5"));
        Assert.assertTrue(destStrings[1].contentEquals("0.5"));
        Assert.assertTrue(destStrings[2].contentEquals("0.5"));
        Assert.assertTrue(destStrings[3].contentEquals("0.5"));
    }

    @Test (expected = IOException.class)
    public void test_fillCategoricalFeature_failsForNumericalOrEmpty() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();
        String destStrings[] = new String[4];

        strings.add(new String[]{"0", "", "a", "uu"});
        strings.add(new String[]{"", "", "", "aa"});
        strings.add(new String[]{"2", "", "c", "cu"});

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        int cidx = 0;

        tested.fillCategoricalFeature(0, cidx, destStrings, "a");
        tested.fillCategoricalFeature(1, cidx, destStrings, "a");
    }

    @Test
    public void test_fillCategoricalFeature_FillsBinaryCategorical() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();
        String destStrings[] = new String[4];

        strings.add(new String[]{"0", "", "a", "uu"});
        strings.add(new String[]{"", "", "", "aa"});
        strings.add(new String[]{"2", "", "c", "cu"});

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        int cidx = 0;

        cidx = tested.fillCategoricalFeature(2, cidx, destStrings, "a");
        cidx = tested.fillCategoricalFeature(2, cidx, destStrings, "c");

        Assert.assertTrue(cidx == 2);
        Assert.assertTrue(destStrings[0].contentEquals("0"));
        Assert.assertTrue(destStrings[1].contentEquals("1"));
    }

    @Test
    public void test_fillCategoricalFeature_FillsMultinominalCategorical() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();
        String destStrings[] = new String[9];

        strings.add(new String[]{"0", "", "a", "uu"});
        strings.add(new String[]{"", "", "", "aa"});
        strings.add(new String[]{"2", "", "c", "cu"});

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        int cidx = 0;

        cidx = tested.fillCategoricalFeature(3, cidx, destStrings, "uu");
        cidx = tested.fillCategoricalFeature(3, cidx, destStrings, "aa");
        cidx = tested.fillCategoricalFeature(3, cidx, destStrings, "cu");

        Assert.assertTrue(cidx == 9);
        Assert.assertTrue(destStrings[0].contentEquals("1"));
        Assert.assertTrue(destStrings[1].contentEquals("0"));
        Assert.assertTrue(destStrings[2].contentEquals("0"));

        Assert.assertTrue(destStrings[3].contentEquals("0"));
        Assert.assertTrue(destStrings[4].contentEquals("1"));
        Assert.assertTrue(destStrings[5].contentEquals("0"));

        Assert.assertTrue(destStrings[6].contentEquals("0"));
        Assert.assertTrue(destStrings[7].contentEquals("0"));
        Assert.assertTrue(destStrings[8].contentEquals("1"));
    }

    @Test (expected = IOException.class)
    public void test_fillCategoricalFeature_failsWhenValueIsNotRecorded() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();
        String destStrings[] = new String[3];

        strings.add(new String[]{"0", "", "a", "uu"});
        strings.add(new String[]{"", "", "", "aa"});
        strings.add(new String[]{"2", "", "c", "cu"});

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(false);

        int cidx = 0;

        cidx = tested.fillCategoricalFeature(3, cidx, destStrings, "a");
    }

    @Test
    public void test_getPvmDescriptionSingleRecord_BuildsDescriptionForMixedMissingAndNotMissing() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();
        String srcStrings[] = new String[]{"1", "5", "0", "c", "aa", ""};

        strings.add(new String[]{"1", "0", "", "a", "uu", "a"});
        strings.add(new String[]{"0", "", "", "", "aa", "b"});
        strings.add(new String[]{"1", "2", "", "c", "cu", ""});

        tested.setArrayOfString(strings);
        tested.getStringCategoricalAndAverages(true);

        int totalFeaturesCount = tested.computeFinalFeatureCount(true);

        String retStrings[] = tested.getPvmDescriptionSingleRecord(srcStrings, true, totalFeaturesCount);

        Assert.assertTrue(retStrings.length == totalFeaturesCount + 1);
        Assert.assertTrue(retStrings[0].contentEquals("0"));
        Assert.assertTrue(retStrings[1].contentEquals("5"));
        Assert.assertTrue(retStrings[2].contentEquals("1"));
        Assert.assertTrue(retStrings[3].contentEquals("0"));
        Assert.assertTrue(retStrings[4].contentEquals("1"));
        Assert.assertTrue(retStrings[5].contentEquals("0"));
        Assert.assertTrue(retStrings[6].contentEquals(String.valueOf(0.5)));
    }

    @Test
    public void test_getPvmDescription_BuildsDescriptionForMultipleRecords() throws IOException {
        ArrayList<String[]> strings = new ArrayList<String[]>(3);
        FileStringDescriptionForTest tested = new FileStringDescriptionForTest();

        strings.add(new String[]{"1", "0", "", "a", "uu", "a"});
        strings.add(new String[]{"0", "", "", "", "aa", "b"});
        strings.add(new String[]{"1", "2", "", "c", "cu", ""});

        tested.setArrayOfString(strings);
        String resStrings[][] = tested.getPvmDescription(true);

        String cStrings[], destString[];

        cStrings = resStrings[0];
        destString = new String[] {"0", "0", "0", "1", "0", "0", "0"};

        Assert.assertTrue(cStrings.length == destString.length);
        for (int i = 0; i < cStrings.length; i++)
            Assert.assertTrue(cStrings[i].contentEquals(destString[i]));

        cStrings = resStrings[1];
        destString = new String[] {"1", String.valueOf(1.0), String.valueOf(0.5), "0", "1", "0", "1"};

        Assert.assertTrue(cStrings.length == destString.length);
        for (int i = 0; i < cStrings.length; i++)
            Assert.assertTrue(cStrings[i].contentEquals(destString[i]));


        cStrings = resStrings[2];
        destString = new String[] {"0", "2", "1", "0", "0", "1", String.valueOf(0.5)};

        Assert.assertTrue(cStrings.length == destString.length);
        for (int i = 0; i < cStrings.length; i++)
            Assert.assertTrue(cStrings[i].contentEquals(destString[i]));
    }

}