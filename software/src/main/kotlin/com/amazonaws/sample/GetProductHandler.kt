// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sample

import aws.sdk.kotlin.runtime.auth.credentials.EnvironmentCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.GetItemRequest
import com.amazonaws.sample.model.Product
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GetProductHandler : RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private val productTable = System.getenv("PRODUCT_TABLE")
    private val dynamoDbClient = DynamoDbClient {
        region = System.getenv("AWS_REGION")
        credentialsProvider = EnvironmentCredentialsProvider()
    }

    override fun handleRequest(event: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val logger = context.logger

        val requestId = event.pathParameters?.get("id") ?: return missingId()

        logger.log("INFO: Fetching product [$requestId]")

        val response = runBlocking {
            dynamoDbClient.getItem(
                GetItemRequest {
                    tableName = productTable
                    key = mapOf("PK" to AttributeValue.S(requestId))
                }
            )
        }

        val id = response.item?.getValue("PK")?.asString
        val name = response.item?.getValue("name")?.asString
        val price = response.item?.getValue("price")?.asFloat

        if (id == null || name == null || price == null) {
            return APIGatewayV2HTTPResponse().apply {
                statusCode = 500
                headers = mapOf("Content-Type" to "application/json")
                body = """{"message": "Error fetching product"}"""
            }
        }

        return APIGatewayV2HTTPResponse().apply {
            statusCode = 200
            headers = mapOf("Content-Type" to "application/json")
            body = Json.encodeToString(Product(id, name, price))
        }
    }
}
