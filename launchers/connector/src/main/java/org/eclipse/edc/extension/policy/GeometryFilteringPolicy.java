/*
 *  Copyright (c) 2024 Fraunhofer Institute for Software and Systems Engineering
 *
 *  This program and the accompanying materials are made available under the
 *  terms of the Apache License, Version 2.0 which is available at
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 *  SPDX-License-Identifier: Apache-2.0
 *
 *  Contributors:
 *       Fraunhofer Institute for Software and Systems Engineering - initial API and implementation
 *
 */

package org.eclipse.edc.extension.policy;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.edc.participant.spi.ParticipantAgentPolicyContext;
import org.eclipse.edc.policy.engine.spi.AtomicConstraintRuleFunction;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.spi.monitor.Monitor;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.io.ParseException;
import org.locationtech.jts.io.WKTReader;

import java.util.Arrays;
import java.util.Map;

import static java.lang.String.format;

/**
 * This class implements a geometry filtering policy for participant agents.
 * It checks if the geometry in the JWT token matches the geometry in the rule.
 */
public class GeometryFilteringPolicy {
    private final ObjectMapper mapper = new ObjectMapper();

    private final Monitor monitor;

    public GeometryFilteringPolicy(Monitor monitor) {
        this.monitor = monitor;
    }

    public <T extends ParticipantAgentPolicyContext> AtomicConstraintRuleFunction<Permission, T> generateRuleFunction() {
        return (Operator operator, Object rightValue, Permission rule, T context) -> {

            monitor.info("All claims from JWT token: "+ context.participantAgent().getClaims());
            var geometry = context.participantAgent().getClaims().get("geometry");
            if(geometry == null) {
                monitor.info("Geometry claim is null for participant: " + context.participantAgent().getClaims().get("participant_id"));
                return false;
            }
            try {
                Geometry claimsGeometry = new WKTReader().read(geometry.toString());
                Geometry rightOperandGeometry = new WKTReader().read(rightValue.toString());
                monitor.info(format("Evaluating constraint: geometry %s %s %s", Arrays.toString(claimsGeometry.getCoordinates()), operator, Arrays.toString(rightOperandGeometry.getCoordinates())));
                return switch (operator) {
                    case EQ -> claimsGeometry.equals(rightOperandGeometry);
//                    case NEQ -> !claimsGeometry.equals(rightOperandGeometry);
                    case IN -> claimsGeometry.intersects(rightOperandGeometry);
//                    case IS_NONE_OF -> !claimsGeometry.intersects(rightOperandGeometry);
                    default -> false;
                };

            } catch (ParseException e) {
                monitor.info("Failed to parse geometry: " + e);
                throw new RuntimeException(e);
            }
        };
    }

    private BoundingBox parseBoundingBox(Object json) {
        if (json instanceof Map<?, ?> m) {
            return new BoundingBox(
                    ((Number) m.get("xmin")).doubleValue(),
                    ((Number) m.get("ymin")).doubleValue(),
                    ((Number) m.get("xmax")).doubleValue(),
                    ((Number) m.get("ymax")).doubleValue()
            );
        } else if (json instanceof String s) {
            try {
                return mapper.readValue(s, BoundingBox.class);
            } catch (JsonProcessingException e) {
                monitor.info("Failed to parse bounding box from string: " + e);
                throw new RuntimeException(e);
            }
        } else {
            monitor.info("Unsupported bounding box format: " + json);
            throw new IllegalArgumentException("Unsupported bounding box format: " + json);
        }
    }
}
