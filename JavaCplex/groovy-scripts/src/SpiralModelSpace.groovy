import dsolve.SolverHelper
import pvm.KernelProducts.KernelProductManager
import pvm.PvmEntry
import pvm.PvmSolver

def getShapePoints( double a, double b, boolean twin ) {
	def shape = []

	def inc = 5
	for ( double i = 30; i < 400; i += inc ) {

		inc -= 0.07
		if ( inc < 1 ) inc = 1

		double ti = (b*i) % (2*Math.PI)   // b != 0
		double ri = a * Math.abs(b) * i // a > 0

		def x = ri * Math.cos( ti )
		def y = ri * Math.sin( ti )

		if ( twin ) {
			x = -x;
			y = -y
		}
		shape << [x,y]
	}
	return shape
}

def generateShapes( String outf ) {

	def writer = new File( outf ).newWriter()
	def el1 = getShapePoints( 0.06, (2*Math.PI)/153.8, false )
	def el2 = getShapePoints( 0.06, (2*Math.PI)/153.8, true )

	//el1.each { List point -> writer.writeLine( "${point[0]},${point[1]}," ) }
	//el2.each { List point -> writer.writeLine( "${point[0]},${point[1]}," ) }
	el1.each { List point -> writer.writeLine( "1|${point[0]},${point[1]}," ) }
	el2.each { List point -> writer.writeLine( "0|${point[0]},${point[1]}," ) }
	writer.close()
}

def prefix = "spiral"

def shapeDat = "data\\${prefix}.txt"
def shapePos = "data\\${prefix}_pos.txt"
def shapeNeg = "data\\${prefix}_neg.txt"
def modelPos = "data\\${prefix}.txt.model.pos"
def modelNeg = "data\\${prefix}.txt.model.neg"

generateShapes( shapeDat )

SolverHelper.dropNativeCplex()
def solver = new PvmSolver()
solver.core.ReadFile( shapeDat )

KernelProductManager.setKernelTypeGlobal( KernelProductManager.KerType.KERRBF );
KernelProductManager.setParamInt( -950 );
KernelProductManager.setParamDouble( 1.455 );

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
for ( double x = -1.5; x<= 1.5; x+= 0.005 )
	for ( double y = -1.5; y<= 1.5; y += 0.005 ) {

		def entry = new PvmEntry()
		entry.x = [x,y]
		def dist = solver.core.getSignedDistance( entry )

		if ( dist >= 0 ) distancesPos << [x,y,dist]
		else distancesNeg << [x,y,dist]
	}

// write datasets again
def datasetWriterPos = new File ( shapePos ).newWriter()
def datasetWriterNeg = new File ( shapeNeg ).newWriter()
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

//distancesPos.each { List it -> modelPosWriter.writeLine( "${it[0]},${it[1]}" ) }
//distancesNeg.each { List it -> modelNegWriter.writeLine( "${it[0]},${it[1]}" ) }

for ( int i=0;i< distancesPos.size(); i++ ) {
	List it = distancesPos[i]
	modelPosWriter.writeLine( "${it[0]},${it[1]}" )
}

for ( int i=0;i< distancesNeg.size(); i++ ) {
	List it = distancesNeg[i]
	if ( i== 28300 ) {
		i = i
	}
	modelNegWriter.writeLine( "${it[0]},${it[1]}" )
}

modelPosWriter.close()
modelNegWriter.close()
