// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0

package com.amazonaws.sample.model

import kotlinx.serialization.Serializable

@Serializable
data class Products(val products: List<Product>)
