package org.jpos.ee.pentaho;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import org.jpos.ee.pentaho.exception.PentahoReportException;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;

/**
 *
 * @author jpaoletti
 */
public interface ReportGenerator {

    public void setReportPath(String reportPath);
    public String getReportPath();

    public OutputStream generateReport(
            final OutputType outputType,
            final Map<String, Object> parameters,
            final OutputStream out) throws ReportProcessingException, ResourceException, PentahoReportException;

    public File generateReport(
            final OutputType outputType,
            final Map<String, Object> parameters,
            final String outputFilename) throws IllegalArgumentException, ReportProcessingException, PentahoReportException;
}
