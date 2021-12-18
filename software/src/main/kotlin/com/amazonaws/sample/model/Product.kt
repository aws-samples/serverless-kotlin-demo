// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sample.model

import kotlinx.serialization.Serializable

@Serializable
data class Product(val id: String, val name: String, val price: Float)
