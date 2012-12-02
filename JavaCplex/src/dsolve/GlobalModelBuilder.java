package dsolve;

import dsolve.lfs.LfsConstraint;
import dsolve.lfs.LfsReader;
import ilog.concert.*;
import ilog.cplex.IloCplex;
import org.apache.log4j.Logger;
import util.SolverLogger;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: Andrei
 * Date: 11/3/12
 * Time: 5:59 PM
 */

public class GlobalModelBuilder {

    public static Logger logger = SolverLogger.getLogger( GlobalModelBuilder.class.getName() );

    List<LfsConstraint> constraintsContainer = new ArrayList<LfsConstraint>();
    List<IloNumVar>  cplexNamedVariables = null;

    private static class PairAngle {
        int eq0Index, eq1Index;
        double angle;

        public PairAngle( int eq0Index, int eq1Index, double value ) {
            this.eq0Index = eq0Index;
            this.eq1Index = eq1Index;
            this.angle = value;
        }

        static class CompareTwoPairsAngleAsceding implements Comparator<PairAngle> {
            public int compare( PairAngle first, PairAngle other ) {
                if ( first.angle < other.angle ) { return -1; }
                if ( first.angle > other.angle ) { return +1; }
                return 0;
            }
        }
    }

    //---------------------------------------------------------------------------------------------------------

    public static String getVarNameByIndex( int varIndex ) {
        return String.format( "v%03d", varIndex );
    }

    public static String getBlockFileName ( int blockId ) {
        return String.format( "global-model-block-%03d.lp", blockId );
    }

    //---------------------------------------------------------------------------------------------------------

    private void sortIndexOfConstraints() {
        for ( LfsConstraint constraint : constraintsContainer ) {
            constraint.sortIndex();
        }
    }

    private List<PairAngle> computeRelativeAngles() {

        int i, j, count = constraintsContainer.size();
        int anglesCount = count * (count - 1) / 2;

	    logger.info( String.format( "computing equation angles; a total of %d pairs", anglesCount) );

        PairAngle pairAngle;
        List<PairAngle> eqAngles = new ArrayList<PairAngle>( anglesCount );

        // make sure the terms in each equation are sorted
        sortIndexOfConstraints();

        // add computed angles each eq with each other one
        for (i = 0; i < count; i++){
            for (j = i + 1; j < count; j++ ) {
                pairAngle = new PairAngle( i, j, computeAngle(i, j) );
                eqAngles.add( pairAngle );
            }
        }

        Collections.sort( eqAngles, new PairAngle.CompareTwoPairsAngleAsceding() );

        return eqAngles;
    }

    private double computeAngle( int eqIdx0, int eqIdx1 ) {

        int [] indexEq0 = constraintsContainer.get( eqIdx0 ).getIndex();
        int [] indexEq1 = constraintsContainer.get( eqIdx1 ).getIndex();

        double [] termsEq0 = constraintsContainer.get( eqIdx0 ).getTerms();
        double [] termsEq1 = constraintsContainer.get( eqIdx1 ).getTerms();

        double result = 0;
        int i = 0, j = 0;

        while( i < indexEq0.length && j < indexEq1.length ) {
            if ( indexEq0[i] < indexEq1[j] ) { i++; continue; }
            if ( indexEq0[i] > indexEq1[j] ) { j++; continue; }

            result += termsEq0[i] * termsEq1[j];
            i++; j++;
        }

        result /= constraintsContainer.get(eqIdx0).getNorm() * constraintsContainer.get(eqIdx1).getNorm();

        return result;
    }

    /**
     * This method asserts that the indexes of every constraint are already sorted
     * @throws IloException
     */
    private void initializeCplexNamedVariables() throws IloException {

        int largestVarIndex = 0;
        for ( LfsConstraint constraint : constraintsContainer ) {
            int localVarIndex = constraint.getIndex()[ constraint.getSize()-1 ];
            if ( largestVarIndex < localVarIndex ) {
                largestVarIndex = localVarIndex;
            }
        }
	    largestVarIndex ++;

        cplexNamedVariables = new ArrayList<IloNumVar>( largestVarIndex );
        IloCplex cplexVariableProvider = new IloCplex();

        for ( int i=0; i<largestVarIndex; i++ ) {
            IloNumVar var = cplexVariableProvider.numVar(
                    -Double.MAX_VALUE,
                    Double.MAX_VALUE,
                    IloNumVarType.Float,
                    getVarNameByIndex( i )
                );
            cplexNamedVariables.add( var );
        }
    }

    public List<String> splitIntoBlockFiles( int eqPerBlock, boolean useAngles ) throws IloException {
	    List<String> blockFilePaths = new ArrayList<String>();
        List<LfsConstraint> blockList;
        Set<Integer> takenEqIndex = new HashSet<Integer>( constraintsContainer.size() );

	    // compute angles betweem every pair of equations
        List<PairAngle> angleList = computeRelativeAngles();

        initializeCplexNamedVariables();

        // TODO: take useAngles var into consideration

        blockList = new ArrayList<LfsConstraint>( eqPerBlock );
        int blockIndex = 0;
        for ( PairAngle currentAngle : angleList ) {
	        // stop condition
	        if ( takenEqIndex.size() >= constraintsContainer.size() ) { break; }

            if ( blockList.size() >= eqPerBlock ) {
                logger.info( "building block number: " + blockIndex );
                IloCplex cplexObj = constructBlockFromConstraints( blockList );

	            String blockFileName = getBlockFileName( blockIndex );
                cplexObj.exportModel( blockFileName );
	            cplexObj.clearModel();
	            cplexObj.end();

	            blockFilePaths.add( blockFileName );
                blockList.clear();
                blockIndex++;
            }

            if ( !takenEqIndex.contains( currentAngle.eq0Index ) ) {
                blockList.add( constraintsContainer.get( currentAngle.eq0Index ) );
                takenEqIndex.add( currentAngle.eq0Index );
            }

            if ( !takenEqIndex.contains( currentAngle.eq1Index ) ) {
                blockList.add( constraintsContainer.get( currentAngle.eq1Index ) );
                takenEqIndex.add( currentAngle.eq1Index );
            }
        }

        // dump the final set of equations
        if ( blockList.size() > 0 ) {
	        logger.info( "building block number: " + blockIndex );

            IloCplex cplexObj = constructBlockFromConstraints(blockList);

	        String blockFileName = getBlockFileName( blockIndex );
	        cplexObj.exportModel( blockFileName );

	        blockFilePaths.add( blockFileName );
        }

        return blockFilePaths;
    }

    public static void addConstraintToCplexModel(IloCplex cplexModel, LfsConstraint constraint, List<IloNumVar> namedVariables) throws IloException {
        IloLinearNumExpr numExpr = cplexModel.linearNumExpr();
        int []    constrIndex = constraint.getIndex();
        double [] constrTerms  = constraint.getTerms();

        // add terms to build the body of the equation
        for ( int i=0; i<constraint.getSize(); i++ ) {
            numExpr.addTerm( constrTerms[i], namedVariables.get( constrIndex[i] ) );
        }

        // add the actual equation to the cplex object
        cplexModel.addRange( constraint.getLowerBound(), numExpr, constraint.getHigherBound(), constraint.getName() );
    }

    private IloCplex constructBlockFromConstraints( List<LfsConstraint> constraintList ) throws IloException {

        IloCplex cplex = new IloCplex();

        for ( LfsConstraint constraint : constraintList ) {
            addConstraintToCplexModel(cplex, constraint, cplexNamedVariables);
        }
        return cplex;
    }

    public GlobalModelBuilder readModelFromFile( String fileName ) throws IOException {
        logger.info( "started reading constraints from file: " + fileName );

        LfsConstraint constraint;
        LfsReader lfsReader = new LfsReader( fileName );
        int count = 0;

        while ( ( constraint = lfsReader.readConstraint() ) != null ) {
            constraintsContainer.add( constraint );

            if ( count % 1000 == 0 ) {
                logger.info( String.format( "until now we read -> %05d", count ) );
            }
            count ++;
        }

        logger.info( "finished reading constraints" );

        return this;
    }

    public String generateStartingPointFile(double defaultVal) throws IOException {
        File f = File.createTempFile( "temp-objective-point-", ".tmp" );

        if (!f.exists())
            f.createNewFile();

        FileWriter fstream = new FileWriter(f.getAbsoluteFile());
        PrintWriter out = new PrintWriter(fstream);
        String line;
        int i;

        for (i = 0; i < cplexNamedVariables.size(); i++)
        {
            line = String.format("%s|%8f", cplexNamedVariables.get(i).getName(), defaultVal);
            out.print(line);
            out.println();
        }

        out.close();

        return f.getAbsolutePath();
    }

    public void clearModel(){
        constraintsContainer.clear();
        cplexNamedVariables = null;
    }

    public void addLfsConstraint(LfsConstraint constraint){constraintsContainer.add(constraint);}

    public void setConstraintNamesToIndexes(){
        int i;

        for (i = 0; i < constraintsContainer.size(); i++)
            constraintsContainer.get(i).setName("a" + String.valueOf(i));
    }

    public boolean getNamesUniqueness()
    {//returns whether the names of the constraints are unique or not
        int i, j;

        for (i = 0; i < constraintsContainer.size(); i++)
        {
            String cName = constraintsContainer.get(i).getName();

            for (j = i + 1; j < constraintsContainer.size(); j++)
                if (cName == constraintsContainer.get(j).getName())
                    return false;
        }

        return true;
    }

    public List<LfsConstraint> getConstraintsContainer(){
        return constraintsContainer;
    }

    public void setConstraintsContainer(List<LfsConstraint> constraintsContainer){
        this.constraintsContainer = constraintsContainer;
    }

}
