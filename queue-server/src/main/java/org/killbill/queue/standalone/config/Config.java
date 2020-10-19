/*
 * Copyright 2020-2020 Equinix, Inc
 * Copyright 2014-2020 The Billing Project, LLC
 *
 * The Billing Project licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package org.killbill.queue.standalone.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

public class Config {

    private static final String PROPERTY_FILE = "org.killbill.queue.standalone.config";
    private static final String TEST_RESOURCE = "config/test.yml";

    private final ConfigModel config;

    public Config() throws IOException, URISyntaxException {
        final ObjectMapper om = new ObjectMapper(new YAMLFactory());
        final URL url = loadPropertiesFromFile();
        if (url == null) {
            final File resourceFile = getConfigFromResource();
            config = om.readValue(resourceFile, ConfigModel.class);
        } else {
            config = om.readValue(url, ConfigModel.class);
        }
        config.initialize();
    }

    public ConfigModel getConfig() {
        return config;
    }

    private File getConfigFromResource() {
        final ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        final File file = new File(classLoader.getResource(TEST_RESOURCE).getFile());
        return file;
    }

    private URL loadPropertiesFromFile() throws IOException, URISyntaxException {
        final String configFile = System.getProperty(PROPERTY_FILE);
        if (configFile != null) {
            final URI inputUri = new URI(configFile);
            final String scheme = inputUri.getScheme();
            if (scheme.equals("file")) {
                final URI uri = (new File(inputUri.getSchemeSpecificPart())).toURI();
                final URL url = uri.toURL();
                return url;
            }
        }
        return null;
    }

}
