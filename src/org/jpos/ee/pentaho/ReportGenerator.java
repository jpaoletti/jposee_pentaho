package org.jpos.ee.pentaho;

import java.io.File;
import java.io.OutputStream;
import java.util.Map;
import org.jpos.ee.pentaho.exception.ReportException;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;

/**
 *
 * @author jpaoletti
 */
public interface ReportGenerator {

    public void setReportPath(String reportPath);
    public String getReportPath();
    public Map<String, Object> getParameters();
    public void setParameters(Map<String, Object> parameters);

    public OutputStream generateReport(
            final OutputType outputType,
            final OutputStream out) throws ReportProcessingException, ResourceException, ReportException;

    public File generateReport(
            final OutputType outputType,
            final String outputFilename) throws IllegalArgumentException, ReportProcessingException, ReportException;
}
