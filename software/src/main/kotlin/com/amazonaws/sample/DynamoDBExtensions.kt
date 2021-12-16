// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sample

import aws.sdk.kotlin.services.dynamodb.model.AttributeValue

val AttributeValue.asString : String get() = (this as AttributeValue.S).value
val AttributeValue.asFloat : Float get() = (this as AttributeValue.N).value.toFloat()