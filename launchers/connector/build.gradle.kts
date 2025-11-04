/*
 *  Copyright (c) 2022 Bayerische Motoren Werke Aktiengesellschaft (BMW AG)
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

plugins {
    `java-library`
    id("application")
    alias(libs.plugins.shadow)
    id(libs.plugins.swagger.get().pluginId)
}

dependencies {
    api(libs.edc.data.plane.spi)
    api(libs.edc.json.ld.spi)

    implementation(libs.edc.fc.spi.crawler)
    implementation(libs.edc.iam.mock)
    implementation("de.fraunhofer.iosb:edc-extension4aas")
    implementation("de.fraunhofer.iosb:edc-connector-client")
    implementation("de.fraunhofer.iosb:data-plane-aas")

    runtimeOnly(libs.edc.bom.controlplane) {
        exclude(group = "org.eclipse.edc", module = "identity-trust-sts-remote-client")
        exclude(group = "org.eclipse.edc", module = "identity-trust-core")
        exclude(group = "org.eclipse.edc", module = "identity-trust-transform")
        exclude(group = "org.eclipse.edc", module = "identity-trust-issuers-configuration")
    }
    runtimeOnly(libs.edc.bom.dataplane)
    runtimeOnly(libs.edc.bom.dataplane.sql)
    runtimeOnly(libs.edc.bom.controlplane.sql)
    runtimeOnly(libs.edc.api.secrets)
    runtimeOnly(libs.edc.dataplane.v2)

    // Data plane components required for transfers
    runtimeOnly(libs.edc.data.plane.selector.api)
    runtimeOnly(libs.edc.data.plane.selector.core)
    runtimeOnly(libs.edc.data.plane.self.registration)
    runtimeOnly(libs.edc.data.plane.signaling.api)
    runtimeOnly(libs.edc.transfer.data.plane.signaling)
    runtimeOnly(libs.edc.validator.data.address.http.data)

    // EDR (Endpoint Data Reference) components
    runtimeOnly(libs.edc.edr.cache.api)
    runtimeOnly(libs.edc.edr.store.core)
    runtimeOnly(libs.edc.edr.store.receiver)
    runtimeOnly(libs.edc.edr.index.sql)
}

application {
    mainClass.set("$group.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    isZip64 = true
    mergeServiceFiles()
    archiveFileName.set("connector.jar")
    dependsOn(distTar, distZip)
}
