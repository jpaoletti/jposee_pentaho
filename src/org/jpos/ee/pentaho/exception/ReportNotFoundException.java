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
package org.jpos.ee.pentaho.exception;

/**
 * Representa un error indicando que el reporte especificado no se encontr� en 
 * el lugar indicado o tuvo un error al leerlo.
 * 
 * */
public class ReportNotFoundException extends PentahoReportException {

    public ReportNotFoundException() {
        super();
    }

    public ReportNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }

    public ReportNotFoundException(String message) {
        super(message);
    }

    public ReportNotFoundException(Throwable cause) {
        super(cause);
    }
}
