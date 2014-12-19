/*
 * This file is part of Dependency-Track.
 *
 * Dependency-Track is free software: you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation, either version 3 of the License, or (at your option) any
 * later version.
 *
 * Dependency-Track is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more
 * details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Dependency-Track. If not, see http://www.gnu.org/licenses/.
 *
 * Copyright (c) Axway. All Rights Reserved.
 */

package org.owasp.dependencytrack.dao;

import org.hibernate.SessionFactory;
import org.owasp.dependencycheck.Engine;
import org.owasp.dependencycheck.data.nvdcve.CveDB;
import org.owasp.dependencycheck.data.nvdcve.DatabaseException;
import org.owasp.dependencycheck.data.nvdcve.DatabaseProperties;
import org.owasp.dependencycheck.reporting.ReportGenerator;
import org.owasp.dependencycheck.utils.Settings;
import org.owasp.dependencytrack.model.ApplicationVersion;
import org.owasp.dependencytrack.model.LibraryVersion;
import org.owasp.dependencytrack.model.Vulnerability;
import org.owasp.dependencytrack.util.DCObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.io.ByteArrayOutputStream;
import java.util.ArrayList;
import java.util.List;

@Repository
public class ReportDao {

    private static DatabaseProperties properties = null;

    /**
     * The Hibernate SessionFactory
     */
    @Autowired
    private SessionFactory sessionFactory;

    private void initializeProperties() {
        DatabaseProperties prop = null;
        CveDB cve = null;
        try {
            cve = new CveDB();
            cve.open();
            properties = cve.getDatabaseProperties();
        } catch (DatabaseException ex) {
            //LOGGER.log(Level.FINE, "Unable to retrieve DB Properties", ex);
        } finally {
            if (cve != null) {
                cve.close();
            }
        }
    }

    public String generateDependencyCheckReport(int applicationVersionId, ReportGenerator.Format format) {
        final ApplicationVersion applicationVersion = (ApplicationVersion) sessionFactory
                .getCurrentSession().load(ApplicationVersion.class, applicationVersionId);

        String appName = applicationVersion.getApplication().getName() + " " + applicationVersion.getVersion();

        LibraryVersionDao libraryVersionDao = new LibraryVersionDao(sessionFactory);
        List<LibraryVersion> libraryVersionList = libraryVersionDao.getDependencies(applicationVersion);
        List<org.owasp.dependencycheck.dependency.Dependency> dcDependencies = new ArrayList<>();

        VulnerabilityDao vulnerabilityDao = new VulnerabilityDao(sessionFactory);
        DCObjectMapper mapper = new DCObjectMapper();
        for (LibraryVersion libraryVersion: libraryVersionList) {
            List<Vulnerability> vulnerabilities = vulnerabilityDao.getVulnsForLibraryVersion(libraryVersion);
            org.owasp.dependencycheck.dependency.Dependency dcDependency = mapper.toDCDependency(libraryVersion, vulnerabilities);
            dcDependencies.add(dcDependency);
        }

        if (properties == null) {
            //initializeProperties();
        }
        Settings.initialize();
        Settings.setBoolean(Settings.KEYS.AUTO_UPDATE, false);
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();

            Engine engine = new Engine(this.getClass().getClassLoader());
            ReportGenerator reportGenerator = new ReportGenerator(appName, dcDependencies, engine.getAnalyzers(), properties);
            reportGenerator.generateReports(baos, format);
            engine.cleanup();
            return baos.toString("UTF-8");
        } catch (Exception e) {
            // todo: log this
            e.printStackTrace();
        }
        return null;
    }

}