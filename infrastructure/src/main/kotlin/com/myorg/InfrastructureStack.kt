// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.myorg

import software.amazon.awscdk.CfnOutput
import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.apigatewayv2.alpha.AddRoutesOptions
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpApi
import software.amazon.awscdk.services.apigatewayv2.alpha.HttpMethod
import software.amazon.awscdk.services.apigatewayv2.alpha.PayloadFormatVersion
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.LambdaProxyIntegration
import software.amazon.awscdk.services.apigatewayv2.integrations.alpha.LambdaProxyIntegrationProps
import software.amazon.awscdk.services.dynamodb.Attribute
import software.amazon.awscdk.services.dynamodb.AttributeType
import software.amazon.awscdk.services.dynamodb.BillingMode
import software.amazon.awscdk.services.dynamodb.Table
import software.amazon.awscdk.services.lambda.*
import software.amazon.awscdk.services.lambda.Function
import software.amazon.awscdk.services.logs.RetentionDays
import software.constructs.Construct

class InfrastructureStack constructor(scope: Construct, id: String, props: StackProps) : Stack(scope, id, props) {
    companion object {
        private const val MEMORY_SIZE = 2048
    }

    val functions = mutableListOf<Function>()
    private val codePath = "../software/build/libs/software-1.0-SNAPSHOT-all.jar"
    private val optimizationLayer: LayerVersion = LayerVersion.Builder.create(this, "OptimizationLayer")
        .layerVersionName("OptimizationLayer")
        .description("Enable tiered compilation")
        .compatibleRuntimes(listOf(Runtime.JAVA_11, Runtime.JAVA_8_CORRETTO))
        .code(Code.fromAsset("../software/OptimizationLayer/layer.zip"))
        .build()

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
        val environmentVariables = mapOf(
            "PRODUCT_TABLE" to productsTable.tableName,
            "AWS_LAMBDA_EXEC_WRAPPER" to "/opt/java-exec-wrapper",
        )

        val putProductFunction = buildLambdaFunction("PutProduct", environmentVariables)
        val getProductFunction = buildLambdaFunction("GetProduct", environmentVariables)
        val getAllProductsFunction = buildLambdaFunction("GetAllProducts", environmentVariables)
        val deleteProductFunction = buildLambdaFunction("DeleteProduct", environmentVariables)
        productsTable.grantReadData(getProductFunction)
        productsTable.grantReadData(getAllProductsFunction)
        productsTable.grantWriteData(putProductFunction)
        productsTable.grantWriteData(deleteProductFunction)
        functions.add(getProductFunction)
        functions.add(putProductFunction)
        functions.add(deleteProductFunction)
        functions.add(getAllProductsFunction)

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

        CfnOutput.Builder.create(this, "KotlinApiUrl")
            .exportName("KotlinApiUrl")
            .value(httpApi.apiEndpoint)
            .build()
    }

    private fun buildLambdaFunction(
        name: String,
        environmentVariables: Map<String, String>,
    ) = Function.Builder.create(this, name)
        .functionName(name)
        .handler("com.amazonaws.sample.${name}Handler")
        .runtime(Runtime.JAVA_11)
        .code(Code.fromAsset(codePath))
        .architecture(Architecture.ARM_64)
        .logRetention(RetentionDays.ONE_WEEK)
        .memorySize(MEMORY_SIZE)
        .timeout(Duration.seconds(20))
        .environment(environmentVariables)
        .layers(listOf(optimizationLayer))
        .build()
}