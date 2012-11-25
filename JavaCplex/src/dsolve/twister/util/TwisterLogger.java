package dsolve.twister.util;

import cgl.imr.base.impl.JobConf;
import cgl.imr.base.impl.MapperConf;
import cgl.imr.base.impl.ReducerConf;
import org.apache.commons.lang3.Validate;
import org.apache.log4j.Logger;
import org.apache.log4j.PropertyConfigurator;

import java.io.*;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import static java.util.Collections.*;

public class TwisterLogger {

    public static final String JOB_LOGGER_FOLDER_PROP = "dpvm.twister.job.sharedfolder";

    private void formattedWrite( Writer writer, String format, String... args ) throws IOException {
        String line = String.format( format, (Object[])args );
        writer.write( line + "\n" );
    }

    public static String getHostIp () throws SocketException {
        String hostIp = "127.0.0.1";

        Enumeration nets = NetworkInterface.getNetworkInterfaces();
        NetworkInterface eth0 = (NetworkInterface) nets.nextElement();
        Enumeration inetAddresses = eth0.getInetAddresses();
        InetAddress ip = (InetAddress) inetAddresses.nextElement();

        for ( Object inetObject : list(inetAddresses) ) {
            InetAddress inet = ( InetAddress ) inetObject;
            if ( inet.getHostAddress().contains(".") ) {
                hostIp = inet.getHostAddress();
                break;
            }
        }

        return hostIp;
    }

    private Logger createLogger( JobConf jobConf, String prefix, int taskId ) throws IOException {

        String sharedFolder = jobConf.getProperty( JOB_LOGGER_FOLDER_PROP );
        Validate.notNull( sharedFolder, String.format( "Property: %s not found", JOB_LOGGER_FOLDER_PROP) );

        if ( prefix.toLowerCase().contains( "driver" )) {
            boolean status = new File( sharedFolder ).mkdir();
            System.out.print("Driver created shared logging folder with: ");
            System.out.println(status ? "success" : "error");
        }

        String jobId = jobConf.getJobId();
        Validate.notNull( jobId, "Could not get JobId; please fix this" );

        String taskIdent = String.format( "%s-%s-%03d", getHostIp().replaceAll("\\.","-"), prefix, taskId );
        String loggerPath = sharedFolder + File.separator + taskIdent + ".log";

        File tempFile = File.createTempFile("log4j.config", ".tmp");
        BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

        formattedWrite(writer, "log4j.category.%s = DEBUG, %s", taskIdent, taskIdent);
        formattedWrite(writer, "log4j.appender.%s = org.apache.log4j.RollingFileAppender", taskIdent);
        formattedWrite(writer, "log4j.appender.%s.File = %s", taskIdent, loggerPath);
        formattedWrite(writer, "log4j.appender.%s.MaxFileSize = 1MB", taskIdent);
        formattedWrite(writer, "log4j.appender.%s.MaxBackupIndex = 1", taskIdent);
        formattedWrite(writer, "log4j.appender.%s.layout = org.apache.log4j.PatternLayout", taskIdent);
        formattedWrite(writer, "log4j.appender.%s.layout.ConversionPattern%s", taskIdent, " = %d{ABSOLUTE} %5p %c{1}:%L - %m%n");
        writer.close();

        PropertyConfigurator.configure( tempFile.getAbsolutePath() );
        return Logger.getLogger( taskIdent );
    }

    public Logger fromMapperConfig ( JobConf jobConf, MapperConf mapperConf ) throws IOException {
        return createLogger( jobConf, "mapper", mapperConf.getMapTaskNo() );
    }

    public Logger fromReducerConfig ( JobConf jobConf, ReducerConf reducerConf ) throws IOException {
        return createLogger( jobConf, "reducer", reducerConf.getReduceTaskNo() );
    }

    public Logger fromCombinerConfig ( JobConf jobConf ) throws IOException {
        return createLogger( jobConf, "combiner", 0 );
    }

    public Logger fromDriverConfig ( JobConf jobConf ) throws IOException {
        return createLogger( jobConf, "driver", 0 );
    }
}
