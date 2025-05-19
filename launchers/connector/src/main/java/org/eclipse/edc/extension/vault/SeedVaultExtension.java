/*
 *  Copyright (c) 2024 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Bayerische Motoren Werke Aktiengesellschaft (BMW AG) - initial API and implementation
 *
 */

package org.eclipse.edc.extension.vault;


import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.runtime.metamodel.annotation.Setting;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.spi.security.Vault;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Map;
import java.util.Properties;

import static java.lang.String.format;

public class SeedVaultExtension implements ServiceExtension {

    @Inject
    protected Monitor monitor;
    
    @Inject
    private Vault vault;

    @Setting
    private static final String VAULT_FILE_PATH = "edc.vault.file.path";

    @Override
    public void initialize(ServiceExtensionContext context) {
        //Generate self-signed certificate (public key embedded)
        //openssl req -new -x509 -key private.key -out public.crt -days 365
        monitor.info("Initializing File Vault Extension");

        var vaultFilePath = context.getConfig().getString(VAULT_FILE_PATH);

        Properties properties = new Properties();

        try {
            // Load the properties file
            FileInputStream inputStream = new FileInputStream(vaultFilePath);
            properties.load(inputStream);
            inputStream.close();

            // Iterate over all key-value pairs in the properties file
            for (Map.Entry<Object, Object> entry : properties.entrySet()) {
                String key = (String) entry.getKey();
                String value = (String) entry.getValue();

                monitor.info(format("Adding key %s to vault with value %s.", key, value));
                vault.storeSecret(key, value);

            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}