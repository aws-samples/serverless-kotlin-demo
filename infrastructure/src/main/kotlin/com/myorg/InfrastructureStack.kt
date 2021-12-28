// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.myorg

import software.constructs.Construct
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.dynamodb.AttributeType
import software.amazon.awscdk.services.lambda.LayerVersion
import software.amazon.awscdk.services.logs.RetentionDays
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.LambdaProxyIntegration
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.LambdaProxyIntegrationProps
import software.amazon.awscdk.services.apigatewayv2.alpha.PayloadFormatVersion
import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod
import software.amazon.awscdk.services.dynamodb.Attribute
import software.amazon.awscdk.services.dynamodb.BillingMode
import software.amazon.awscdk.services.dynamodb.Table
import software.amazon.awscdk.services.lambda.Architecture
import software.amazon.awscdk.services.lambda.Code
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.lambda.Runtime

class InfrastructureStack constructor(scope: Construct?, id: String?, props: StackProps? = null) : Stack(scope, id, props) {
    companion object {
        private const val MEMORY_SIZE = 2048
    }

    val functions = mutableListOf<Function>()

    init {
        val productsTable = Table.Builder.create(this, "Products-Kotlin")
            .tableName("Products-Kotlin")
            .partitionKey(
                Attribute.builder()
                    .type(AttributeType.STRING)
                    .name("PK")
                    .build()
            )
            .billingMode(BillingMode.PAY_PER_REQUEST)
            .build()

        val optimizationLayer = LayerVersion.Builder.create(this, "OptimizationLayer")
            .layerVersionName("OptimizationLayer")
            .description("Enable tiered compilation")
            .compatibleRuntimes(listOf(Runtime.JAVA_11, Runtime.JAVA_8_CORRETTO))
            .code(Code.fromAsset("../software/OptimizationLayer/layer.zip"))
            .build()
        val codePath = "../software/build/libs/software-1.0-SNAPSHOT-all.jar"
        val environmentVariables = mapOf(
            "PRODUCT_TABLE" to productsTable.tableName,
            "AWS_LAMBDA_EXEC_WRAPPER" to "/opt/java-exec-wrapper"
        )

        val putProductFunction = Function.Builder.create(this, "PutProduct")
            .functionName("PutProduct")
            .runtime(Runtime.JAVA_11)
            .code(Code.fromAsset(codePath))
            .handler("com.amazonaws.sample.PutProductHandler")
            .architecture(Architecture.ARM_64)
            .logRetention(RetentionDays.ONE_WEEK)
            .memorySize(MEMORY_SIZE)
            .timeout(Duration.seconds(20))
            .environment(environmentVariables)
            .layers(listOf(optimizationLayer))
            .build()
        val getProductFunction = Function.Builder.create(this, "GetProduct")
            .functionName("GetProduct")
            .runtime(Runtime.JAVA_11)
            .code(Code.fromAsset(codePath))
            .handler("com.amazonaws.sample.GetProductHandler")
            .architecture(Architecture.ARM_64)
            .logRetention(RetentionDays.ONE_WEEK)
            .memorySize(MEMORY_SIZE)
            .timeout(Duration.seconds(20))
            .environment(environmentVariables)
            .layers(listOf(optimizationLayer))
            .build()
        val getAllProductsFunction = Function.Builder.create(this, "GetAllProducts")
            .functionName("GetAllProducts")
            .runtime(Runtime.JAVA_11)
            .code(Code.fromAsset(codePath))
            .handler("com.amazonaws.sample.GetAllProductsHandler")
            .architecture(Architecture.ARM_64)
            .logRetention(RetentionDays.ONE_WEEK)
            .memorySize(MEMORY_SIZE)
            .timeout(Duration.seconds(20))
            .environment(environmentVariables)
            .layers(listOf(optimizationLayer))
            .build()
        val deleteProductFunction = Function.Builder.create(this, "DeleteProduct")
            .functionName("DeleteProduct")
            .runtime(Runtime.JAVA_11)
            .code(Code.fromAsset(codePath))
            .handler("com.amazonaws.sample.DeleteProductHandler")
            .architecture(Architecture.ARM_64)
            .logRetention(RetentionDays.ONE_WEEK)
            .memorySize(MEMORY_SIZE)
            .timeout(Duration.seconds(20))
            .environment(environmentVariables)
            .layers(listOf(optimizationLayer))
            .build()
        productsTable.grantWriteData(putProductFunction)
        productsTable.grantWriteData(deleteProductFunction)
        productsTable.grantReadData(getProductFunction)
        productsTable.grantReadData(getAllProductsFunction)
        val httpApi = HttpApi.Builder.create(this, "KotlinProductsApi")
            .apiName("KotlinProductsApi")
            .build()
        httpApi.addRoutes(
            AddRoutesOptions.builder()
                .path("/{id}")
                .methods(listOf(HttpMethod.PUT))
                .integration(
                    LambdaProxyIntegration(
                        LambdaProxyIntegrationProps.builder()
                            .handler(putProductFunction)
                            .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                            .build()
                    )
                )
                .build()
        )
        httpApi.addRoutes(
            AddRoutesOptions.builder()
                .path("/{id}")
                .methods(listOf(HttpMethod.GET))
                .integration(
                    LambdaProxyIntegration(
                        LambdaProxyIntegrationProps.builder()
                            .handler(getProductFunction)
                            .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                            .build()
                    )
                )
                .build()
        )
        httpApi.addRoutes(
            AddRoutesOptions.builder()
                .path("/")
                .methods(listOf(HttpMethod.GET))
                .integration(
                    LambdaProxyIntegration(
                        LambdaProxyIntegrationProps.builder()
                            .handler(getAllProductsFunction)
                            .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                            .build()
                    )
                )
                .build()
        )
        httpApi.addRoutes(
            AddRoutesOptions.builder()
                .path("/{id}")
                .methods(listOf(HttpMethod.DELETE))
                .integration(
                    LambdaProxyIntegration(
                        LambdaProxyIntegrationProps.builder()
                            .handler(deleteProductFunction)
                            .payloadFormatVersion(PayloadFormatVersion.VERSION_2_0)
                            .build()
                    )
                )
                .build()
        )
        functions.add(getProductFunction)
        functions.add(putProductFunction)
        functions.add(deleteProductFunction)
        functions.add(getAllProductsFunction)

        CfnOutput.Builder.create(this, "KotlinApiUrl")
            .exportName("KotlinApiUrl")
            .value(httpApi.apiEndpoint)
            .build()
    }
}