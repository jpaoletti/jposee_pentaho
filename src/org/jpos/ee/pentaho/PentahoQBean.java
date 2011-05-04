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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import org.jdom.Element;
import org.jpos.core.ConfigurationException;
import org.jpos.q2.QBeanSupport;
import org.jpos.util.NameRegistrar;

/**
 *
 * @author jpaoletti
 */
public class PentahoQBean extends QBeanSupport {

    private Map<String, PentahoReportDefinition> reports;

    public AbstractReportGenerator getGenerator(final String report, final Map<String, Object> parameters) throws ConfigurationException {
        final PentahoReportDefinition r = reports.get(report);
        if (r == null) {
            throw new ConfigurationException("Missing report");
        }
        final AbstractReportGenerator generator = (AbstractReportGenerator) getFactory().newInstance(cfg.get("generator", "org.jpos.ee.pentaho.BasicReportGenerator"));
        generator.setLog(getLog());
        generator.setDebug(cfg.getBoolean("debug", false));
        generator.setConnectionDriver(cfg.get("connection-driver"));
        generator.setConnectionUser(cfg.get("connection-user"));
        generator.setConnectionUrl(cfg.get("connection-url"));
        generator.setConnectionPassword(cfg.get("connection-password"));
        generator.setQueryPath(r.getSqlFile());
        generator.setReportPath(r.getReportFile());
        generator.setParameters(parameters);
        return generator;
    }

    @Override
    protected void initService() throws Exception {
        NameRegistrar.register(getName(), this);
        initReports();
    }

    protected void initReports() throws ConfigurationException {
        reports = new HashMap<String, PentahoReportDefinition>();
        Iterator iter = getPersist().getChildren("report").iterator();
        while (iter.hasNext()) {
            Element e = (Element) iter.next();
            String name = e.getAttributeValue("name");
            if (name == null) {
                throw new ConfigurationException("missing report name");
            }
            if (reports.get(name) != null) {
                throw new ConfigurationException("Report '" + name + "' already defined");
            }
            reports.put(name, initReport(e));
        }
    }

    @Override
    protected void stopService() throws Exception {
        NameRegistrar.unregister(getName());
    }


    private PentahoReportDefinition initReport(Element e) {
        final PentahoReportDefinition report = new PentahoReportDefinition();
        report.setName(e.getAttributeValue("name"));
        report.setSqlFile(e.getAttributeValue("sql"));
        report.setReportFile(e.getAttributeValue("report"));
        return report;
    }
}
