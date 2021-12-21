// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sample

import aws.sdk.kotlin.runtime.auth.credentials.EnvironmentCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.PutItemRequest
import com.amazonaws.sample.model.Product
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

class PutProductHandler : RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private val productTable = System.getenv("PRODUCT_TABLE")
    private val dynamoDbClient = DynamoDbClient {
        region = System.getenv("AWS_REGION")
        credentialsProvider = EnvironmentCredentialsProvider()
    }

    override fun handleRequest(event: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val logger = context.logger
        logger.log(event.toString())

        val id = event.pathParameters?.get("id") ?: return missingId()

        if (event.body == null || event.body.isEmpty()) {
            return APIGatewayV2HTTPResponse().apply {
                statusCode = 400
                headers = mapOf("Content-Type" to "application/json")
                body = """{"message": "Empty request body"}"""
            }
        }

        val product = try {
            Json.decodeFromString<Product>(event.body)
        } catch (e: Exception) {
            logger.log(e.message)
            return APIGatewayV2HTTPResponse().apply {
                statusCode = 400
                headers = mapOf("Content-Type" to "application/json")
                body = """{"message": "Failed to parse product from request body"}"""
            }
        }

        logger.log("Product: $product")

        if (id != product.id) {
            logger.log("ERROR: Product ID in path ($id) does not match product ID in body (${product.id})")
            return APIGatewayV2HTTPResponse().apply {
                statusCode = 400
                headers = mapOf("Content-Type" to "application/json")
                body = """{"message": "Product ID in path does not match product ID in body"}"""
            }
        }

        val itemValues = mapOf(
            "PK" to AttributeValue.S(product.id),
            "name" to AttributeValue.S(product.name),
            "price" to AttributeValue.N(product.price.toString())
        )

        runBlocking {
            dynamoDbClient.putItem(
                PutItemRequest {
                    tableName = productTable
                    item = itemValues
                }
            )
        }

        return APIGatewayV2HTTPResponse().apply {
            statusCode = 201
            headers = mapOf("Content-Type" to "application/json")
            body = """{"message": "Product created"}"""
        }
    }
}
