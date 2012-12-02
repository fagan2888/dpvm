package util;

import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;

/**
 * Created with IntelliJ IDEA.
 * User: imcu
 * Date: 11/25/12
 * Time: 7:57 PM
 */

public class SolverLogger {

	public static Logger getLogger( String name ) {

		File tempFile = null;
		try {
			tempFile = File.createTempFile( "log4j.config", ".tmp" );
			BufferedWriter writer = new BufferedWriter( new FileWriter( tempFile ) );

			writer.write( "log4j.rootLogger=INFO, stdout\n" );
			writer.write( "log4j.appender.stdout=org.apache.log4j.ConsoleAppender\n" );
			writer.write( "log4j.appender.stdout.Target=System.out\n" );
			writer.write( "log4j.appender.stdout.layout=org.apache.log4j.PatternLayout\n" );
			writer.write( "log4j.appender.stdout.layout.ConversionPattern=%d{ABSOLUTE} %5p %c{1}:%L - %m%n\n" );
			writer.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}

		PropertyConfigurator.configure( tempFile.getAbsolutePath() );
		return Logger.getLogger( name );
	}
}
