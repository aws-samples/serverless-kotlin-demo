// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sample

import aws.sdk.kotlin.runtime.auth.credentials.EnvironmentCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.AttributeValue
import aws.sdk.kotlin.services.dynamodb.model.DeleteItemRequest
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import kotlinx.coroutines.runBlocking

class DeleteProductHandler : RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private val productTable = System.getenv("PRODUCT_TABLE")
    private val dynamoDbClient = DynamoDbClient {
        region = System.getenv("AWS_REGION")
        credentialsProvider = EnvironmentCredentialsProvider()
    }

    override fun handleRequest(event: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val logger = context.logger

        val id = event.pathParameters?.get("id") ?: return missingId()

        try {
            runBlocking {
                dynamoDbClient.deleteItem(
                    DeleteItemRequest {
                        tableName = productTable
                        key = mapOf("PK" to AttributeValue.S(id))
                    }
                )
            }
        } catch (e: Exception) {
            logger.log("ERROR ${e.message}")
            return APIGatewayV2HTTPResponse().apply {
                statusCode = 500
                headers = mapOf("Content-Type" to "application/json")
                body = """{"message": "Failed to delete product"}"""
            }
        }

        return APIGatewayV2HTTPResponse().apply {
            statusCode = 200
            headers = mapOf("Content-Type" to "application/json")
            body = """{"message": "Product deleted"}"""
        }
    }
}
