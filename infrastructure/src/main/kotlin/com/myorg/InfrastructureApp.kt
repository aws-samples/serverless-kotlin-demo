// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.myorg

import kotlin.jvm.JvmStatic
import software.amazon.awscdk.App
import com.myorg.InfrastructureStack
import software.amazon.awscdk.StackProps
import com.myorg.DashboardStack
import software.amazon.awscdk.Environment

object InfrastructureApp {
    @JvmStatic
    fun main(args: Array<String>) {
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
        val functions = infrastructureStack.functions
        DashboardStack(
            app, "KotlinDashboard", StackProps.builder()
                .env(
                    Environment.builder()
                        .account(System.getenv("CDK_DEFAULT_ACCOUNT"))
                        .region(System.getenv("CDK_DEFAULT_REGION"))
                        .build()
                )
                .build(),
            functions
        )
        app.synth()
    }
}
