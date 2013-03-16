import dsolve.SolverHelper
import pvm.KernelProducts.KernelProductManager
import pvm.PvmEntry
import pvm.PvmSolver

def generatePoints( int a, int b, int count ) {
	def points = []

	def rand = new Random( new Date().time );
	count.times {
		double x = a + rand.nextDouble() * (b-a)
		double y = a + rand.nextDouble() * (b-a);
		points << [x,y]
	}

	return points
}

def generateShape( String outf ) {

	def writer = new File( outf ).newWriter()
	def points = generatePoints( 0, 1, 500 )
	def table = []

	def start = 1
	for ( int i = 0; i<4; i++ ) {
		start = (int) Math.abs( 1-start )
		for ( int j=0; j<4; j++) {
			double xl = i*0.25; double xr = i*0.25 + 0.25;
			double yl = j*0.25; double yr = j*0.25 + 0.25;
			def label = ((start+j)%2==0) ?1:0
			table << [xl, xr, yl, yr, label]
		}
	}

	for ( int i=0; i<points.size(); i++) {
		for ( int j=0; j<table.size(); j++) {
			def x = points[i][0], y = points[i][1];
			def xl = table[j][0], xr = table[j][1], yl = table[j][2], yr = table[j][3]
			if ( xl <= x && x < xr && yl <= y && y < yr ) {
				points[i] = [x, y, table[j][4]]
				break
			}
		}
	}

	points.each { List point -> writer.writeLine( "${point[2]}|${point[0]},${point[1]}," ) }
	writer.close()
}

def checkersDat = "data\\checkers.txt"
def checkersPos = "data\\checkers_pos.txt"
def checkersNeg = "data\\checkers_neg.txt"
def modelPos   = "data\\checkers.txt.model.pos"
def modelNeg   = "data\\checkers.txt.model.neg"

//generateShape( checkersDat )

SolverHelper.dropNativeCplex()
def solver = new PvmSolver()
solver.core.ReadFile( checkersDat )

KernelProductManager.setKernelTypeGlobal( KernelProductManager.KerType.KERRBF )
KernelProductManager.setParamInt( 0 )
KernelProductManager.setParamDouble( 0.5 )

def trained = solver.TrainSingleLP()
println "train status: $trained"
def labels = solver.classify( solver.core.entries )

double[] accuracy = new double[1]
double[] sensitivity = new double[1]
double[] specificity = new double[1]

PvmSolver.computeAccuracy( labels, solver.core.entries, accuracy, sensitivity, specificity, 0 )
println "ACC:${accuracy[0]}/SENS:${sensitivity[0]}/SPEC:${specificity[0]}"

def distancesPos = []
def distancesNeg = []

println "computing distanes"
for ( double x = 0; x< 1; x+= 0.005 )
	for ( double y = 0; y< 1; y += 0.005 ) {
		def entry = new PvmEntry()
		entry.x = [x,y]
		def dist = solver.core.getSignedDistance( entry )

		if ( dist >= 0 ) distancesPos << [x,y,dist]
		else distancesNeg << [x,y,dist]
	}

// write datasets again
def datasetWriterPos = new File ( checkersPos ).newWriter()
def datasetWriterNeg = new File ( checkersNeg ).newWriter()
for ( PvmEntry entry in solver.core.entries ) {
	if ( entry.label )
		datasetWriterPos.writeLine( "${entry.x[0]},${entry.x[1]}" )
	else
		datasetWriterNeg.writeLine( "${entry.x[0]},${entry.x[1]}" )
}
datasetWriterPos.close()
datasetWriterNeg.close()

println "writing to files"
// write model points

def modelPosWriter = new File ( modelPos ).newWriter()
def modelNegWriter = new File ( modelNeg ).newWriter()

distancesPos.each { List it -> modelPosWriter.writeLine( "${it[0]},${it[1]}" ) }
distancesNeg.each { List it -> modelNegWriter.writeLine( "${it[0]},${it[1]}" ) }
datasetWriterPos.close()
datasetWriterNeg.close()
