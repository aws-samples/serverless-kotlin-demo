// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.myorg;

import software.amazon.awscdk.CfnOutput;
import software.amazon.awscdk.Duration;
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi;
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod;
import software.amazon.awscdk.services.apigatewayv2.alpha.PayloadFormatVersion;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.LambdaProxyIntegration;
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.LambdaProxyIntegrationProps;
import software.amazon.awscdk.services.dynamodb.Attribute;
import software.amazon.awscdk.services.dynamodb.AttributeType;
import software.amazon.awscdk.services.dynamodb.BillingMode;
import software.amazon.awscdk.services.dynamodb.Table;
import software.amazon.awscdk.services.lambda.Architecture;
import software.amazon.awscdk.services.lambda.Code;
import software.amazon.awscdk.services.lambda.Function;
import software.amazon.awscdk.services.lambda.LayerVersion;
import software.amazon.awscdk.services.lambda.Runtime;
import software.amazon.awscdk.services.logs.RetentionDays;
import software.constructs.Construct;
import software.amazon.awscdk.Stack;
import software.amazon.awscdk.StackProps;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;

public class InfrastructureStack extends Stack {

    private static final int MEMORY_SIZE = 2048;
    List<Function> functions = new ArrayList<>();

    public InfrastructureStack(final Construct scope, final String id) {
        this(scope, id, null);
    }

    public InfrastructureStack(final Construct scope, final String id, final StackProps props) {
        super(scope, id, props);

        Table productsTable = Table.Builder.create(this, "Products-Kotlin")
                .tableName("Products-Kotlin")
                .partitionKey(Attribute.builder()
                        .type(AttributeType.STRING)
                        .name("PK")
                        .build())
                .billingMode(BillingMode.PAY_PER_REQUEST)
                .build();

        LayerVersion optimizationLayer = LayerVersion.Builder.create(this, "OptimizationLayer")
                .layerVersionName("OptimizationLayer")
                .description("Enable tiered compilation")
                .compatibleRuntimes(Arrays.asList(Runtime.JAVA_11, Runtime.JAVA_8_CORRETTO))
                .code(Code.fromAsset("../software/OptimizationLayer/layer.zip"))
                .build();

        String codePath = "../software/build/libs/serverless-kotlin-demo-1.0-SNAPSHOT-all.jar";
        Map<String, String> environmentVariables = Map.of("PRODUCT_TABLE", productsTable.getTableName(),
                "AWS_LAMBDA_EXEC_WRAPPER", "/opt/java-exec-wrapper");
        Function putProductFunction = Function.Builder.create(this, "PutProduct")
                .functionName("PutProduct")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset(codePath))
                .handler("com.amazonaws.sample.PutProductHandler")
                .architecture(Architecture.ARM_64)
                .logRetention(RetentionDays.ONE_WEEK)
                .memorySize(MEMORY_SIZE)
                .timeout(Duration.seconds(20))
                .environment(environmentVariables)
                .layers(List.of(optimizationLayer))
                .build();

        Function getProductFunction = Function.Builder.create(this, "GetProduct")
                .functionName("GetProduct")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset(codePath))
                .handler("com.amazonaws.sample.GetProductHandler")
                .architecture(Architecture.ARM_64)
                .logRetention(RetentionDays.ONE_WEEK)
                .memorySize(MEMORY_SIZE)
                .timeout(Duration.seconds(20))
                .environment(environmentVariables)
                .layers(List.of(optimizationLayer))
                .build();

        Function getAllProductsFunction = Function.Builder.create(this, "GetAllProducts")
                .functionName("GetAllProducts")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset(codePath))
                .handler("com.amazonaws.sample.GetAllProductsHandler")
                .architecture(Architecture.ARM_64)
                .logRetention(RetentionDays.ONE_WEEK)
                .memorySize(MEMORY_SIZE)
                .timeout(Duration.seconds(20))
                .environment(environmentVariables)
                .layers(List.of(optimizationLayer))
                .build();

        Function deleteProductFunction = Function.Builder.create(this, "DeleteProduct")
                .functionName("DeleteProduct")
                .runtime(Runtime.JAVA_11)
                .code(Code.fromAsset(codePath))
                .handler("com.amazonaws.sample.DeleteProductHandler")
                .architecture(Architecture.ARM_64)
                .logRetention(RetentionDays.ONE_WEEK)
                .memorySize(MEMORY_SIZE)
                .timeout(Duration.seconds(20))
                .environment(environmentVariables)
                .layers(List.of(optimizationLayer))
                .build();

        productsTable.grantWriteData(putProductFunction);
        productsTable.grantWriteData(deleteProductFunction);
        productsTable.grantReadData(getProductFunction);
        productsTable.grantReadData(getAllProductsFunction);

        HttpApi httpApi = HttpApi.Builder.create(this, "KotlinProductsApi")
                .apiName("KotlinProductsApi")
                .build();

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/{id}")
                .methods(singletonList(HttpMethod.PUT))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(putProductFunction)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/{id}")
                .methods(singletonList(HttpMethod.GET))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(getProductFunction)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/")
                .methods(singletonList(HttpMethod.GET))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(getAllProductsFunction)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());

        httpApi.addRoutes(AddRoutesOptions.builder()
                .path("/{id}")
                .methods(singletonList(HttpMethod.DELETE))
                .integration(new LambdaProxyIntegration(LambdaProxyIntegrationProps.builder()
                        .handler(deleteProductFunction)
                        .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                        .build()))
                .build());

        functions.add(getProductFunction);
        functions.add(putProductFunction);
        functions.add(deleteProductFunction);
        functions.add(getAllProductsFunction);

        CfnOutput apiUrl = CfnOutput.Builder.create(this, "KotlinApiUrl")
                .exportName("KotlinApiUrl")
                .value(httpApi.getApiEndpoint())
                .build();
    }

    public List<Function> getFunctions() {
        return Collections.unmodifiableList(functions);
    }
}
