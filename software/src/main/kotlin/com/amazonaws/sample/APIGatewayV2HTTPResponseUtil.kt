// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sample

import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse

fun missingId() = APIGatewayV2HTTPResponse().apply {
    statusCode = 500
    headers = mapOf("Content-Type" to "application/json")
    body = """{ "message": "Missing 'id' parameter in path" }"""
}
