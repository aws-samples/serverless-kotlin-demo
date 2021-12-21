// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sample

import aws.sdk.kotlin.runtime.auth.credentials.EnvironmentCredentialsProvider
import aws.sdk.kotlin.services.dynamodb.DynamoDbClient
import aws.sdk.kotlin.services.dynamodb.model.ScanRequest
import com.amazonaws.sample.model.Product
import com.amazonaws.sample.model.Products
import com.amazonaws.services.lambda.runtime.Context
import com.amazonaws.services.lambda.runtime.RequestHandler
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GetAllProductsHandler : RequestHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> {

    private val productTable = System.getenv("PRODUCT_TABLE")
    private val dynamoDbClient = DynamoDbClient {
        region = System.getenv("AWS_REGION")
        credentialsProvider = EnvironmentCredentialsProvider()
    }

    override fun handleRequest(input: APIGatewayV2HTTPEvent, context: Context): APIGatewayV2HTTPResponse {
        val logger = context.logger

        val scanResponse = try {
            runBlocking {
                dynamoDbClient.scan(
                    ScanRequest {
                        tableName = productTable
                        limit = 20
                    }
                )
            }
        } catch (e: Exception) {
            logger.log("ERROR: ${e.message}")
            return APIGatewayV2HTTPResponse().apply {
                statusCode = 500
                headers = mapOf("Content-Type" to "application/json")
                body = """{"message": "Failed to get products"}"""
            }
        }

        val products = ArrayList<Product>()

        scanResponse.items?.map {
            val id = it.getValue("PK").asString
            val name = it.getValue("name").asString
            val price = it.getValue("price").asFloat
            products.add(Product(id, name, price))
        }

        return APIGatewayV2HTTPResponse().apply {
            statusCode = 200
            headers = mapOf("Content-Type" to "application/json")
            body = Json.encodeToString(Products(products))
        }
    }
}
