package org.eclipse.edc.extension.contracts;

import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.ConsumerContractNegotiationManager;
import org.eclipse.edc.connector.controlplane.contract.spi.negotiation.store.ContractNegotiationStore;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractNegotiation;
import org.eclipse.edc.connector.controlplane.contract.spi.types.negotiation.ContractRequest;
import org.eclipse.edc.connector.controlplane.contract.spi.types.offer.ContractOffer;
import org.eclipse.edc.connector.controlplane.services.contractnegotiation.ContractNegotiationServiceImpl;
import org.eclipse.edc.connector.controlplane.services.query.QueryValidator;
import org.eclipse.edc.policy.model.AtomicConstraint;
import org.eclipse.edc.policy.model.Constraint;
import org.eclipse.edc.policy.model.Expression;
import org.eclipse.edc.policy.model.LiteralExpression;
import org.eclipse.edc.policy.model.Operator;
import org.eclipse.edc.policy.model.Permission;
import org.eclipse.edc.policy.model.Policy;
import org.eclipse.edc.spi.command.CommandHandlerRegistry;
import org.eclipse.edc.spi.monitor.Monitor;
import org.eclipse.edc.transaction.spi.TransactionContext;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.lang.String.format;

/**
 * This class is a custom implementation of the ContractNegotiationServiceImpl.
 * It overrides the initiateNegotiation method to modify the contract request
 * based on the user token information. User token is passed as a JWT token.
 * As of now this implementation is not used in the codebase instead @RolesVerificationPolicy class is used to extract
 * roles from the JWT token and then used in the contract negotiation process.
 */
public class ContractNegotiationServiceCustomImpl extends ContractNegotiationServiceImpl {
    private Monitor monitor;

    private final String edcClientId;
    private final ContractNegotiationStore store;
    private final ConsumerContractNegotiationManager consumerManager;
    private final TransactionContext transactionContext;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final QueryValidator queryValidator;
    public ContractNegotiationServiceCustomImpl(Monitor monitor, String edcClientId, ContractNegotiationStore store, ConsumerContractNegotiationManager consumerManager, TransactionContext transactionContext, CommandHandlerRegistry commandHandlerRegistry, QueryValidator queryValidator) {
        super(store, consumerManager, transactionContext, commandHandlerRegistry, queryValidator);
        this.monitor = monitor;
        this.edcClientId = edcClientId;
        this.store = store;
        this.consumerManager = consumerManager;
        this.transactionContext = transactionContext;
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.queryValidator = queryValidator;
    }

    public ContractNegotiation initiateNegotiation(ContractRequest request) {
        monitor.info("User token details for client: " + edcClientId);
        List<Permission> oldPermissions = new ArrayList<>(request.getContractOffer().getPolicy().getPermissions());
        monitor.info("OLD Permissions with token information: " + oldPermissions);
        if(oldPermissions.isEmpty()){
            monitor.severe("OLD Permissions does not contain any token information: " + oldPermissions);
        }
        List<Permission> newPermissions = new ArrayList<>();
        oldPermissions.forEach(permission -> {
            List<Constraint> newConstraints = new ArrayList<>();
            List<AtomicConstraint> constraints = permission.getConstraints().stream().filter(AtomicConstraint.class::isInstance)
                    .map(AtomicConstraint.class::cast)
                    .toList();
            constraints.forEach(constraint -> {
                Expression userTokenKey = constraint.getLeftExpression();
                Expression tokenValue = constraint.getRightExpression();
                monitor.info(format("User token key: %s",  userTokenKey));
                monitor.info(format("User tokenValue: %s",  tokenValue.toString()));
                if(userTokenKey.toString().contains("https://w3id.org/edc/v0.0.1/ns/token")) {
                    // TODO: now here you have read actual token and extract role information from it
                    try {
                        SignedJWT jwt = SignedJWT.parse(tokenValue.toString());
                        JWTClaimsSet claims = jwt.getJWTClaimsSet();

                        //parse org id from the token and compare with client id for associating user with the connector
                        Map<String, Object> orgRoles = claims.getJSONObjectClaim("orgRoles");
                        monitor.info(format("JWT TOKEN orgRoles: %s", orgRoles));
                        monitor.info(format("JWT TOKEN Claims: %s",  claims.toJSONObject()));
                         String identifiedRole = null;
                         String identifiedPermission = null;
                        Optional<Map.Entry<String, Object>> research = orgRoles.entrySet().stream().filter(entry -> entry.getValue().toString().toLowerCase().contains("research")).findAny();
                        Optional<Map.Entry<String, Object>> forestStewards = orgRoles.entrySet().stream().filter(entry -> entry.getValue().toString().toLowerCase().contains("foreststewards")).findAny();
                         monitor.info(format("JWT TOKEN found match for research: %s", research));
                         monitor.info(format("JWT TOKEN found match for hasForestStewards: %s", forestStewards));
                        List<String> values = List.of(Arrays.toString(orgRoles.values().toArray()));
//                        boolean hasResearch = values.stream().map(String::toLowerCase).anyMatch(role -> role.contains("research"));
//                        boolean hasForestStewards = values.stream().map(String::toLowerCase).anyMatch(role -> role.contains("foreststewards"));

                        if(research.isPresent() && "org".concat(research.get().getKey()).equalsIgnoreCase(edcClientId)){
                            identifiedRole = "Researcher";
                            identifiedPermission = "read";
                        } else if(forestStewards.isPresent() && "org".concat(forestStewards.get().getKey()).equalsIgnoreCase(edcClientId)){
                            identifiedRole = "ForestStewards";
                            identifiedPermission = "write";
                        } else {
                            monitor.severe("User token does not contain valid role information or client id does not match with the token research: " + research + " | forestStewards: " + forestStewards + " | edcClientId: " + edcClientId);
                        }
                        monitor.info("identifiedRole: " + identifiedRole);
                        monitor.info("identifiedPermission: " + identifiedPermission);
                        LiteralExpression leftExp = new LiteralExpression("https://w3id.org/edc/v0.0.1/ns/"+ identifiedRole);
                        Operator operator = constraint.getOperator();
                        LiteralExpression rightExp = new LiteralExpression(identifiedPermission);
                        monitor.info(format("userTokenKey leftExp after modification: %s",  leftExp));
                        monitor.info(format("UsertokenValue rightExp after modification: %s",  rightExp));
                        newConstraints.add(AtomicConstraint.Builder.newInstance()
                                .leftExpression(leftExp)
                                .operator(operator)
                                .rightExpression(rightExp)
                                .build());
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JWT", e);
                    }
                } else {
                    // add all other constraints here
                    newConstraints.add(constraint);
                }
                monitor.info("New modified constraint: " + newConstraints);
            });
            newPermissions.add(Permission.Builder.newInstance()
                    .action(permission.getAction())
                    .constraints(newConstraints)
                    .duties(permission.getDuties())
                    .build());
        });
        monitor.info("New Permissions with role information: " + newPermissions);
        Policy newpolicy = Policy.Builder.newInstance()
                .prohibitions(request.getContractOffer().getPolicy().getProhibitions())
                .permissions(newPermissions)
                .profiles(request.getContractOffer().getPolicy().getProfiles())
                .duties(request.getContractOffer().getPolicy().getObligations())
                .assignee(request.getContractOffer().getPolicy().getAssignee())
                .assigner(request.getContractOffer().getPolicy().getAssigner())
                .target(request.getContractOffer().getPolicy().getTarget())
                .inheritsFrom(request.getContractOffer().getPolicy().getInheritsFrom())
                .type(request.getContractOffer().getPolicy().getType())
                .extensibleProperties(request.getContractOffer().getPolicy().getExtensibleProperties())
                .build();
        ContractOffer newRequestOffer = ContractOffer.Builder.newInstance()
                .id(request.getContractOffer().getId())
                .assetId(request.getContractOffer().getAssetId())
                .policy(newpolicy)
                .build();

        ContractRequest newContractRequest = ContractRequest.Builder.newInstance()
                .callbackAddresses(request.getCallbackAddresses())
                .protocol(request.getProtocol())
                .counterPartyAddress(request.getCounterPartyAddress())
                .contractOffer(newRequestOffer)
                .build();

        return super.initiateNegotiation(newContractRequest);
    }

}
