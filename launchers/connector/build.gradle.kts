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
    implementation(libs.edc.control.api.configuration)
    implementation(libs.edc.control.plane.api.client)
    implementation(libs.edc.control.plane.api)
    implementation(libs.edc.control.plane.core)
    implementation(libs.edc.dsp)
    implementation(libs.edc.configuration.filesystem)
    implementation(libs.edc.management.api)
    implementation(libs.edc.transfer.data.plane.signaling)
    implementation(libs.edc.transfer.pull.http.receiver)
    implementation(libs.edc.validator.data.address.http.data)

    implementation(libs.edc.token.core)
    implementation(libs.edc.http)
    implementation(libs.edc.data.plane.iam)

    implementation(libs.edc.edr.cache.api)
    implementation(libs.edc.edr.store.core)
    implementation(libs.edc.edr.store.receiver)
    implementation(libs.edc.edr.index.sql)

    implementation(libs.edc.data.plane.selector.api)
    implementation(libs.edc.data.plane.selector.core)

    implementation(libs.edc.data.plane.self.registration)
    implementation(libs.edc.data.plane.signaling.api)
    implementation(libs.edc.data.plane.public.api)
    implementation(libs.edc.data.plane.core)
    implementation(libs.edc.data.plane.http)

    implementation(libs.edc.data.plane.aws.s3)

    implementation(libs.edc.oauth2.core)
    implementation(libs.edc.oauth2.client)

    implementation(libs.edc.api.observability)

    implementation(libs.edc.asset.index.sql)
    implementation(libs.edc.contract.definition.store.sql)
    implementation(libs.edc.contract.negotiation.store.sql)
    implementation(libs.edc.control.plane.sql)
    implementation(libs.edc.policy.definition.store.sql)
    implementation(libs.edc.transfer.process.store.sql)
    implementation(libs.edc.data.plane.store.sql)
    implementation(libs.edc.data.plane.instance.store.sql)
    implementation(libs.edc.accesstokendata.store.sql)

    implementation(libs.edc.sql.pool.apache.commons)
    implementation(libs.postgresql)
    implementation(libs.edc.transaction.local)
    implementation(libs.edc.transaction.datasource.spi)

    implementation(libs.edc.fc.spi.crawler)
    runtimeOnly(libs.edc.fc.core)
    runtimeOnly(libs.edc.fc.api)
    // https://mvnrepository.com/artifact/com.nimbusds/nimbus-jose-jwt
    implementation("com.nimbusds:nimbus-jose-jwt:10.2")
    implementation("org.eclipse.edc:oauth2-core:0.11.1")
    implementation("org.eclipse.edc:token-core:0.11.1")
    implementation("org.eclipse.edc:token-lib:0.11.1")
    implementation("org.eclipse.edc:control-plane-contract:0.11.1")
    implementation("org.eclipse.edc:contract-spi:0.11.1")
    implementation("org.eclipse.edc:control-plane-aggregate-services:0.11.1")
    implementation("org.eclipse.edc:contract-negotiation-api:0.11.1")
}

application {
    mainClass.set("$group.boot.system.runtime.BaseRuntime")
}

var distTar = tasks.getByName("distTar")
var distZip = tasks.getByName("distZip")

tasks.withType<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar> {
    mergeServiceFiles()
    archiveFileName.set("connector.jar")
    dependsOn(distTar, distZip)
}
