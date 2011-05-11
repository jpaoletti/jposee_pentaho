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

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.HashMap;
import java.util.Map.Entry;
import org.jpos.ee.pentaho.exception.*;
import org.jpos.util.Logger;

import org.pentaho.reporting.engine.classic.core.MasterReport;
import org.pentaho.reporting.engine.classic.core.ReportProcessingException;
import org.pentaho.reporting.engine.classic.core.modules.output.pageable.pdf.PdfReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.html.HtmlReportUtil;
import org.pentaho.reporting.engine.classic.core.modules.output.table.xls.ExcelReportUtil;
import org.pentaho.reporting.libraries.resourceloader.Resource;
import org.pentaho.reporting.libraries.resourceloader.ResourceException;
import org.pentaho.reporting.libraries.resourceloader.ResourceManager;

/**
 * Generates a report in the following scenario:
 * <ol>
 * <li>The report definition file is a .prpt file which will be loaded
 * and parsed
 * <li>The data factory is a simple JDBC data factory using HSQLDB
 * <li>There are no runtime report parameters used
 * </ol>
 */
public class BasicReportGenerator extends AbstractReportGenerator {

    private static final String SQL_PREFIX = "SQL_";

    /**
     * Returns the report definition which will be used to generate
     * the report. In this case, the report will be loaded and parsed
     * from a file contained in this package.
     * 
     * @return the loaded and parsed report definition to be used in
     *         report generation.
     * @throws PentahoReportException
     * @throws ResourceException 
     */
    @Override
    public MasterReport getReportDefinition() throws ReportException {
        debug(String.format("Report definition: %s", getReportPath()));
        URL reportDefinitionURL;
        try {
            reportDefinitionURL = new URL("file:" + getReportPath()); //classloader.getResource(getReportPath());
        } catch (MalformedURLException e1) {
            throw new ReportNotFoundException(e1);
        }
        final ResourceManager resourceManager = new ResourceManager();
        resourceManager.registerDefaults();
        Resource directly;
        try {
            directly = resourceManager.createDirectly(reportDefinitionURL, MasterReport.class);
        } catch (Exception e) {
            error(e);
            throw new ReportNotFoundException(e);
        }
        try {
            return (MasterReport) directly.getResource();
        } catch (ResourceException e) {
            throw new ReportNotFoundException(e);
        }
    }

    @Override
    public Map<String, Object> getParameters() {
        Map<String, Object> parameters = super.getParameters();
        Map<String, Object> res = new HashMap<String, Object>();
        if (parameters != null) {
            for (Entry<String, Object> entry : parameters.entrySet()) {
                if (!entry.getKey().toUpperCase().startsWith(SQL_PREFIX)) {
                    res.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return res;
    }

    protected Map<String, Object> getSQLParameters() {
        Map<String, Object> parameters = super.getParameters();
        Map<String, Object> res = new HashMap<String, Object>();
        if (parameters != null) {
            for (Entry<String, Object> entry : parameters.entrySet()) {
                if (entry.getKey().toUpperCase().startsWith(SQL_PREFIX)) {
                    res.put(entry.getKey(), entry.getValue());
                }
            }
        }
        return res;
    }

    @Override
    protected String getQuery() throws QueryNotFoundException {
        String sql = super.getQuery();
        Map<String, Object> sqlParameters = getSQLParameters();
        String res = sql;
        for (Entry<String, Object> entry : sqlParameters.entrySet()) {
            res = res.replaceAll("@" + entry.getKey().substring(SQL_PREFIX.length()) + "@", entry.getValue().toString());
        }
        return res;
    }
}
