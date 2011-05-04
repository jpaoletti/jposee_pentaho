/*
 * jPOS Project [http://jpos.org]
 * Copyright (C) 2000-2011 Alejandro P. Revilla
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.jpos.ee.pentaho;

import java.io.*;
import java.util.Map;
import org.jpos.ee.pentaho.exception.*;
import org.jpos.util.Log;
import org.jpos.util.Logger;

import org.pentaho.reporting.engine.classic.core.*;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.layout.output.AbstractReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.DriverConnectionProvider;
import org.pentaho.reporting.engine.classic.core.modules.misc.datafactory.sql.SQLReportDataFactory;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.base.PageableReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.FlowReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.base.StreamReportProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.AllItemsHtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.FileSystemURLRewriter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlPrinter;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.StreamHtmlOutputProcessor;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.FlowExcelOutputProcessor;
import org.pentaho.reporting.libraries.repository.ContentLocation;
import org.pentaho.reporting.libraries.repository.DefaultNameGenerator;
import org.pentaho.reporting.libraries.repository.stream.StreamRepository;
import org.pentaho.reporting.libraries.resourceloader.*;

/**
 * @author jpaoletti
 */
public abstract class AbstractReportGenerator {

    private static final String PASSWORD = "password";
    private static final String USER = "user";
    private static final String QUERY_NAME = "ReportQuery";
    private Map<String, Object> parameters;
    private String reportPath;
    private String queryPath;
    private String connectionDriver;
    private String connectionUrl;
    private String connectionUser;
    private String connectionPassword;
    private Log log;
    private boolean debug = false;

    /**
     * Performs the basic initialization required to generate a report
     */
    public AbstractReportGenerator() {
        // Initialize the reporting engine
        ClassicEngineBoot.getInstance().start();
    }

    protected void debug(String s) {
        if (isDebug() && getLog() != null) {
            Logger.log(getLog().createDebug(String.format("[DEBUG] %s", s)));
        }
    }

    /**
     * Returns the report definition used by this report generator. If
     * this method returns <code>null</code>, the report generation
     * process will throw a <code>NullPointerException</code>.
     * 
     * @return the report definition used by thus report generator
     */
    protected abstract MasterReport getReportDefinition() throws PentahoReportException;

    /**
     * Returns the data factory used by this report generator. If this
     * method returns <code>null</code>, the report generation process
     * will use the data factory used in the report definition.
     * 
     * @return the data factory used by this report generator
     * @throws PentahoReportException
     */
    protected DataFactory getDataFactory() throws PentahoReportException {
        if (getConnectionDriver() == null) {
            debug("Using internal report definition file query and connection");
            return null;
        }

        final DriverConnectionProvider connection = new DriverConnectionProvider();
        debug(String.format("Using connection info: [%s][%s][%s][%s]", getConnectionDriver(), getConnectionUrl(), getConnectionUser(), "****"));
        connection.setDriver(getConnectionDriver());
        connection.setUrl(getConnectionUrl());
        connection.setProperty(USER, getConnectionUser());
        connection.setProperty(PASSWORD, getConnectionPassword());

        final SQLReportDataFactory dataFactory = new SQLReportDataFactory(connection);
        String sql = getQuery();
        debug(sql);
        dataFactory.setQuery(QUERY_NAME, sql);
        return dataFactory;
    }

    protected String getQuery() throws QueryNotFoundException {
        StringBuilder sb = new StringBuilder("\n");
        try {
            debug(String.format("Using query at [%s]", getQueryPath()));
            final File query = new File(getQueryPath());
            FileInputStream fis = null;
            BufferedInputStream bis = null;
            DataInputStream dis = null;
            fis = new FileInputStream(query);
            bis = new BufferedInputStream(fis);
            dis = new DataInputStream(bis);
            while (dis.available() != 0) {
                sb.append((char) dis.read());
            }
            fis.close();
            bis.close();
            dis.close();
        } catch (Exception e) {
            throw new QueryNotFoundException(e);
        }
        return sb.toString();
    }

    /**
     * Generates the report in the specified <code>outputType</code>
     * and writes it into the specified <code>outputFile</code>.
     * 
     * @param outputType
     *            the output type of the report (HTML, PDF, HTML)
     * @param outputFile
     *            the file into which the report will be written
     * @throws IllegalArgumentException
     *             indicates the required parameters were not provided
     * @throws IOException
     *             indicates an error opening the file for writing
     * @throws ReportProcessingException
     *             indicates an error generating the report
     * @throws ResourceException 
     * @throws ResourceCreationException 
     * @throws ResourceLoadingException 
     * @throws PentahoReportException
     */
    protected void generateReport(final OutputType outputType, File outputFile) throws PentahoReportException {
        if (outputFile == null) {
            throw new IllegalArgumentException("The output file was not specified");
        }

        OutputStream outputStream = null;
        try {
            try {
                outputStream = new BufferedOutputStream(new FileOutputStream(outputFile));
                generateReport(outputType, outputStream);
            } finally {
                if (outputStream != null) {
                    outputStream.close();
                }
            }
        } catch (PentahoReportException e) {
            throw e;
        } catch (ReportProcessingException e) {
            throw new org.jpos.ee.pentaho.exception.ReportProcessingException(e);
        } catch (Exception e) {
            throw new InvalidOutputException(e);
        }
    }

    /**
     * Generates the report in the specified <code>outputType</code>
     * and writes it into the specified <code>outputStream</code>.
     * <p/>
     * It is the responsibility of the caller to close the
     * <code>outputStream</code> after this method is executed.
     * 
     * @param outputType
     *            the output type of the report (HTML, PDF, HTML)
     * @param outputStream
     *            the stream into which the report will be written
     * @throws IllegalArgumentException
     *             indicates the required parameters were not provided
     * @throws ReportProcessingException
     *             indicates an error generating the report
     * @throws ResourceException 
     */
    protected void generateReport(final OutputType outputType, OutputStream outputStream) throws IllegalArgumentException, PentahoReportException, ReportProcessingException {

        if (outputStream == null) {
            throw new IllegalArgumentException("The output stream was not specified");
        }

        // Get the report and data factory
        final MasterReport report = getReportDefinition();
        final DataFactory dataFactory = getDataFactory();

        // Set the data factory for the report
        if (dataFactory != null) {
            report.setQuery(QUERY_NAME);
            report.setDataFactory(dataFactory);
        }

        // Add any parameters to the report
        final Map<String, Object> reportParameters = getParameters();
        if (null != reportParameters) {
            for (String key : reportParameters.keySet()) {
                report.getParameterValues().put(key, reportParameters.get(key));
            }
        }

        // Prepare to generate the report
        AbstractReportProcessor reportProcessor = null;
        try {
            // Greate the report processor for the specified output
            // type
            switch (outputType) {
                case PDF: {
                    debug("Creating PDF output");
                    final PdfOutputProcessor outputProcessor = new PdfOutputProcessor(
                            report.getConfiguration(), outputStream,
                            report.getResourceManager());
                    reportProcessor = new PageableReportProcessor(report, outputProcessor);
                    break;
                }

                case EXCEL: {
                    debug("Creating EXCEL output");
                    final FlowExcelOutputProcessor target = new FlowExcelOutputProcessor(
                            report.getConfiguration(), outputStream,
                            report.getResourceManager());
                    reportProcessor = new FlowReportProcessor(report, target);
                    break;
                }
                default: {
                    debug("Creating HTML output");
                    final StreamRepository targetRepository = new StreamRepository(outputStream);
                    final ContentLocation targetRoot = targetRepository.getRoot();
                    final HtmlOutputProcessor outputProcessor = new StreamHtmlOutputProcessor(report.getConfiguration());
                    final HtmlPrinter printer = new AllItemsHtmlPrinter(report.getResourceManager());
                    printer.setContentWriter(targetRoot, new DefaultNameGenerator(targetRoot, "index", "html"));
                    printer.setDataWriter(null, null);
                    printer.setUrlRewriter(new FileSystemURLRewriter());
                    outputProcessor.setPrinter(printer);
                    reportProcessor = new StreamReportProcessor(report, outputProcessor);
                    break;
                }
            }
            reportProcessor.processReport();
            debug("Report successfuly created");
        } finally {
            if (reportProcessor != null) {
                reportProcessor.close();
            }
        }
    }

    public String getReportPath() {
        return this.reportPath;
    }

    public void setReportPath(String reportPath) {
        this.reportPath = reportPath;
    }

    public String getConnectionDriver() {
        return this.connectionDriver;
    }

    public void setConnectionDriver(String connectionDriver) {
        this.connectionDriver = connectionDriver;
    }

    public String getConnectionUrl() {
        return this.connectionUrl;
    }

    public void setConnectionUrl(String connectionUrl) {
        this.connectionUrl = connectionUrl;
    }

    public String getConnectionUser() {
        return this.connectionUser;
    }

    /**
     * Setter
     *
     * @param connectionUser el connectionUser a setear
     */
    public void setConnectionUser(String connectionUser) {
        this.connectionUser = connectionUser;
    }

    /**
     * Getter
     *
     * @return getter de connectionPassword
     */
    public String getConnectionPassword() {
        return this.connectionPassword;
    }

    /**
     * Setter
     *
     * @param connectionPassword el connectionPassword a setear
     */
    public void setConnectionPassword(String connectionPassword) {
        this.connectionPassword = connectionPassword;
    }

    /**
     * Returns the set of parameters that will be passed to the report
     * generation process. If there are no parameters required for
     * report generation, this method may return either an empty or a
     * <code>null</code> <code>Map</code>
     * 
     * @return the set of report parameters to be used by the report
     *         generation process, or <code>null</code> if no
     *         parameters are required.
     */
    public Map<String, Object> getParameters() {
        return this.parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = parameters;
    }

    public void setQueryPath(String queryPath) {
        this.queryPath = queryPath;
    }

    public String getQueryPath() {
        return this.queryPath;
    }

    public Log getLog() {
        return log;
    }

    public void setLog(Log log) {
        this.log = log;
    }

    public void setDebug(boolean debug) {
        this.debug = debug;
        debug("Debug activado");
    }

    public boolean isDebug() {
        return this.debug;
    }
}
