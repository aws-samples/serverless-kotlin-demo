// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.myorg

import software.amazon.awscdk.App
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.Environment

fun main() {
    val app = App()
    val infrastructureStack = InfrastructureStack(
        app, "ServerlessKotlinDemo", StackProps.builder()
            .env(
                Environment.builder()
                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                    .region(System.getenv("CDK_DEFAULT_REGION"))
                    .build()
            )
            .build()
    )
    DashboardStack(
        app, "KotlinDashboard", StackProps.builder()
            .env(
                Environment.builder()
                    .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                    .region(System.getenv("CDK_DEFAULT_REGION"))
                    .build()
            )
            .build(),
        infrastructureStack.functions
    )
    app.synth()
}
