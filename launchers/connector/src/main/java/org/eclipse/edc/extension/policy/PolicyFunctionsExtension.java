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

import org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext;
import org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext;
import org.eclipse.edc.policy.engine.spi.PolicyEngine;
import org.eclipse.edc.policy.engine.spi.RuleBindingRegistry;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.runtime.metamodel.annotation.Inject;
import org.eclipse.edc.spi.system.ServiceExtension;
import org.eclipse.edc.spi.system.ServiceExtensionContext;

import static org.eclipse.edc.connector.controlplane.catalog.spi.policy.CatalogPolicyContext.CATALOG_SCOPE;
import static org.eclipse.edc.connector.controlplane.contract.spi.policy.ContractNegotiationPolicyContext.NEGOTIATION_SCOPE;
import static org.eclipse.edc.jsonld.spi.PropertyAndTypeNames.ODRL_USE_ACTION_ATTRIBUTE;
import static org.eclipse.edc.policy.engine.spi.PolicyEngine.ALL_SCOPES;
import static org.eclipse.edc.spi.constants.CoreConstants.EDC_NAMESPACE;

public class PolicyFunctionsExtension implements ServiceExtension {
    private static final String LOCATION_CONSTRAINT_KEY = EDC_NAMESPACE + "country";
    private static final String PARTICIPANT_CONSTRAINT_KEY = EDC_NAMESPACE + "participantId";
    private static final String KEY_POLICY_EVALUATION_TIME = EDC_NAMESPACE + "policyEvaluationTime";
    private static final String KEY_POLICY_NO_RESTRICTION = EDC_NAMESPACE + "noRestriction";
    private static final String USER_ID_CONSTRAINT_KEY = EDC_NAMESPACE + "user-id";

    @Inject
    private RuleBindingRegistry ruleBindingRegistry;
    @Inject
    private PolicyEngine policyEngine;
    
    @Override
    public String name() {
        return "Sample policy functions";
    }
    
    @Override
    public void initialize(ServiceExtensionContext context) {
        var monitor = context.getMonitor();
        
        ruleBindingRegistry.bind(ODRL_USE_ACTION_ATTRIBUTE, ALL_SCOPES);

        ruleBindingRegistry.bind(LOCATION_CONSTRAINT_KEY, CATALOG_SCOPE);
        ruleBindingRegistry.bind(LOCATION_CONSTRAINT_KEY, NEGOTIATION_SCOPE);

        ruleBindingRegistry.bind(PARTICIPANT_CONSTRAINT_KEY, CATALOG_SCOPE);
        ruleBindingRegistry.bind(PARTICIPANT_CONSTRAINT_KEY, NEGOTIATION_SCOPE);

        ruleBindingRegistry.bind(KEY_POLICY_EVALUATION_TIME, CATALOG_SCOPE);
        ruleBindingRegistry.bind(KEY_POLICY_EVALUATION_TIME, NEGOTIATION_SCOPE);

        ruleBindingRegistry.bind(USER_ID_CONSTRAINT_KEY, CATALOG_SCOPE);
        ruleBindingRegistry.bind(USER_ID_CONSTRAINT_KEY, NEGOTIATION_SCOPE);

        LocationConstraintFactory locationConstraint = new LocationConstraintFactory(monitor);
        policyEngine.registerFunction(CatalogPolicyContext.class, Permission.class, LOCATION_CONSTRAINT_KEY, locationConstraint.generateRuleFunction());
        policyEngine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, LOCATION_CONSTRAINT_KEY, locationConstraint.generateRuleFunction());

        ParticipantConstraintFactory participantConstraint = new ParticipantConstraintFactory(monitor);
        policyEngine.registerFunction(CatalogPolicyContext.class, Permission.class, PARTICIPANT_CONSTRAINT_KEY, participantConstraint.generateRuleFunction());
        policyEngine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, PARTICIPANT_CONSTRAINT_KEY, participantConstraint.generateRuleFunction());

        TimeConstraintFactory timeConstraint = new TimeConstraintFactory(monitor);
        policyEngine.registerFunction(CatalogPolicyContext.class, Permission.class, KEY_POLICY_EVALUATION_TIME, timeConstraint.generateRuleFunction());
        policyEngine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, KEY_POLICY_EVALUATION_TIME, timeConstraint.generateRuleFunction());

        NoRestrictionPolicy noRestrictionPolicy = new NoRestrictionPolicy(monitor);
        policyEngine.registerFunction(CatalogPolicyContext.class, Permission.class, KEY_POLICY_NO_RESTRICTION, noRestrictionPolicy.generateRuleFunction());
        policyEngine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, KEY_POLICY_NO_RESTRICTION, noRestrictionPolicy.generateRuleFunction());

        ReferringUserConstraintFunction referringUserConstraintFunction = new ReferringUserConstraintFunction(monitor);
        policyEngine.registerFunction(CatalogPolicyContext.class, Permission.class, USER_ID_CONSTRAINT_KEY, referringUserConstraintFunction.generateRuleFunction());
        policyEngine.registerFunction(ContractNegotiationPolicyContext.class, Permission.class, USER_ID_CONSTRAINT_KEY, referringUserConstraintFunction.generateRuleFunction());
    }
}