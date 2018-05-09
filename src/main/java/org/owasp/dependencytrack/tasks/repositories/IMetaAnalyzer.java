/*
 * This file is part of Dependency-Track.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Copyright (c) Steve Springett. All Rights Reserved.
 */
package org.owasp.dependencytrack.tasks.repositories;

import com.github.packageurl.PackageURL;
import org.owasp.dependencytrack.model.Component;
import java.util.Collections;
import java.util.List;

/**
 * Interface that defines Repository Meta Analyzers.
 *
 * @author Steve Springett
 * @since 3.1.0
 */
public interface IMetaAnalyzer {

    /**
     * Returns whether or not the analyzer is capable of supporting the ecosystem of the component.
     * @param component the component to analyze
     * @return true if analyzer can be used for this component, false if not
     * @since 3.1.0
     */
    boolean isApplicable(Component component);

    /**
     * A list of components to analyze.
     * @param components a list of components
     * @return a list of MetaModel objects
     * @since 3.1.0
     */
    List<MetaModel> analyze(List<Component> components);

    /**
     * The component to analyze.
     * @param component the component to analyze
     * @return a MetaModel object
     * @since 3.1.0
     */
    MetaModel analyze(Component component);

    /**
     * Convenience factory method that creates an IMetaAnalyzer implementation suitable
     * to analyze the specified component.
     * @param component the component to analyze
     * @return an IMetaAnalyzer implementation
     * @since 3.1.0
     */
    static IMetaAnalyzer build(Component component) {
        if (component.getPurl() != null) {
            if (PackageURL.StandardTypes.MAVEN.equals(component.getPurl().getType())) {
                IMetaAnalyzer analyzer = new NexusMetaAnalyzer();
                if (analyzer.isApplicable(component)) {
                    return analyzer;
                }
            } else if (PackageURL.StandardTypes.NPM.equals(component.getPurl().getType())) {
                IMetaAnalyzer analyzer = new NpmMetaAnalyzer();
                if (analyzer.isApplicable(component)) {
                    return analyzer;
                }
            }
        }

        return new IMetaAnalyzer() {
            @Override
            public boolean isApplicable(Component component) {
                return false;
            }

            @Override
            public List<MetaModel> analyze(List<Component> components) {
                return Collections.emptyList();
            }

            @Override
            public MetaModel analyze(Component component) {
                return new MetaModel(component);
            }
        };
    }

}
