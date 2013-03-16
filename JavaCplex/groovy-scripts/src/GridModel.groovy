import dsolve.SolverHelper
import pvm.KernelProducts.KernelProductManager
import pvm.PvmDataCore
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
	def el2 = getElipsePoints( 4, 4.5 )

	el1.each { List point -> writer.writeLine( "1|${point[0]},${point[1]}," ) }
	el2.each { List point -> writer.writeLine( "0|${point[0]},${point[1]}," ) }
	writer.close()
}

def getMargins( PvmDataCore core ) {
	def sx = Double.MAX_VALUE, bx = Double.MIN_VALUE, sy = Double.MAX_VALUE, by = Double.MIN_VALUE
	for ( PvmEntry entry in core.entries ) {
		x = entry.x[0]
		y = entry.x[1]
		if ( sx > x ) sx = x
		if ( bx < x ) bx = x
		if ( sy > y ) sy = y
		if ( by < y ) by = y
	}
	return [sx, bx, sy, by]
}

def elipsesDataset = "C:\\dpvm\\JavaCplex\\groovy-scripts\\elipses.txt"
def elipsesPos = "C:\\dpvm\\JavaCplex\\groovy-scripts\\elipses_pos.txt"
def elipsesNeg = "C:\\dpvm\\JavaCplex\\groovy-scripts\\elipses_neg.txt"
def modelFile   = "C:\\dpvm\\JavaCplex\\groovy-scripts\\elipses.txt.model"

generateElipses( elipsesDataset )

SolverHelper.dropNativeCplex()
def solver = new PvmSolver()
solver.core.ReadFile( elipsesDataset )

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

def margins = getMargins( solver.core )

def writer = new File ( modelFile ).newWriter()
def distances = []

println "computing distanes"
for ( double x = margins[0]; x< margins[1]; x+= 0.01 )
	for ( double y = margins[2]; y< margins[3]; y += 0.01 ) {
		def entry = new PvmEntry()
		entry.x = [x,y]
		distances << [x,y,Math.abs( solver.core.getSignedDistance( entry ) )]
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

println "filtering points"
// write model points
def modelPoints = distances.findAll () { it[2] < 0.001 }
modelPoints.each { List it ->
	writer.writeLine( "${it[0]},${it[1]}" )
}
writer.close()
