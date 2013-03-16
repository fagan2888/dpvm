import dsolve.SolverHelper
import pvm.KernelProducts.KernelProductManager
import pvm.PvmEntry
import pvm.PvmSolver

def getElipsePoints( double a, double b ) {
	def elipse = []

	for ( double phi = 0; phi < 2*Math.PI; phi += 0.05 ) {
		def x = a * Math.cos( phi )
		def y = b * Math.sin( phi )
		elipse << [x,y]
	}
	return elipse
}

def generateElipses( String outf ) {

	def writer = new File( outf ).newWriter()
	def el1 = getElipsePoints( 9, 5 )
	def el2 = getElipsePoints( 4.5, 2.5 )

	el1.each { List point -> writer.writeLine( "1|${point[0]},${point[1]}," ) }
	el2.each { List point -> writer.writeLine( "0|${point[0]},${point[1]}," ) }
	writer.close()
}

def elipsesDat = "data\\elipses.txt"
def elipsesPos = "data\\elipses_pos.txt"
def elipsesNeg = "data\\elipses_neg.txt"
def modelPos   = "data\\elipses.txt.model.pos"
def modelNeg   = "data\\elipses.txt.model.neg"

generateElipses( elipsesDat )

SolverHelper.dropNativeCplex()
def solver = new PvmSolver()
solver.core.ReadFile( elipsesDat )

KernelProductManager.setKernelTypeGlobal( KernelProductManager.KerType.KERRBF );
KernelProductManager.setParamInt( 0 );
KernelProductManager.setParamDouble( 1e-0 );

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
for ( double x = -10; x< 10; x+= 0.05 )
	for ( double y = -10; y< 10; y += 0.05 ) {
		def entry = new PvmEntry()
		entry.x = [x,y]
		def dist = solver.core.getSignedDistance( entry )

		if ( dist >= 0 ) distancesPos << [x,y,dist]
		else distancesNeg << [x,y,dist]
	}

// write datasets again
def datasetWriterPos = new File ( elipsesPos ).newWriter()
def datasetWriterNeg = new File ( elipsesNeg ).newWriter()
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
