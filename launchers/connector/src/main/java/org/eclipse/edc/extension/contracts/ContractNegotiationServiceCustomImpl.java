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

import static java.lang.String.format;

public class ContractNegotiationServiceCustomImpl extends ContractNegotiationServiceImpl {
    private Monitor monitor;
    private final ContractNegotiationStore store;
    private final ConsumerContractNegotiationManager consumerManager;
    private final TransactionContext transactionContext;
    private final CommandHandlerRegistry commandHandlerRegistry;
    private final QueryValidator queryValidator;
    public ContractNegotiationServiceCustomImpl(Monitor monitor, ContractNegotiationStore store, ConsumerContractNegotiationManager consumerManager, TransactionContext transactionContext, CommandHandlerRegistry commandHandlerRegistry, QueryValidator queryValidator) {
        super(store, consumerManager, transactionContext, commandHandlerRegistry, queryValidator);
        this.monitor = monitor;
        this.store = store;
        this.consumerManager = consumerManager;
        this.transactionContext = transactionContext;
        this.commandHandlerRegistry = commandHandlerRegistry;
        this.queryValidator = queryValidator;
    }

    public ContractNegotiation initiateNegotiation(ContractRequest request) {
        monitor.info("+++++++++++++++++++++ User token details ++++++++++++++++++++++++++++++");
        List<Permission> oldPermissions = new ArrayList<>(request.getContractOffer().getPolicy().getPermissions());
        monitor.info("********** OLD Permissions with token information: " + oldPermissions);
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
                        Map<String, Object> orgRoles = claims.getJSONObjectClaim("orgRoles");
                        monitor.info(format("JWT TOKEN orgRoles: %s", orgRoles));
                        monitor.info(format("JWT TOKEN Claims: %s",  claims.toJSONObject()));
                         String identifiedRole = null;
                         String identifiedPermission = null;
                        List<String> values = List.of(Arrays.toString(orgRoles.values().toArray()));
                        boolean hasResearch = values.stream().map(String::toLowerCase).anyMatch(role -> role.contains("research"));
                        boolean c = values.stream().map(String::toLowerCase).anyMatch(role -> role.contains("foreststewards"));

                        if(hasResearch){
                            identifiedRole = "Researcher";
                            identifiedPermission = "read";
                        } else if(hasResearch){
                            identifiedRole = "ForestStewards";
                            identifiedPermission = "write";
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
