// Copyright Amazon.com, Inc. or its affiliates. All Rights Reserved.
// SPDX-License-Identifier: MIT-0
package com.myorg

import software.amazon.awscdk.Duration
import software.amazon.awscdk.Stack
import software.amazon.awscdk.StackProps
import software.amazon.awscdk.services.cloudwatch.*
import software.amazon.awscdk.services.lambda.Function
import software.constructs.Construct

class DashboardStack(parent: Construct, id: String, props: StackProps, functions: List<Function>) :
    Stack(parent, id, props) {
    init {
        val p50DurationMetrics = functions.map {
            it.metricDuration(
                MetricOptions.builder()
                    .label(it.functionName)
                    .period(Duration.minutes(1))
                    .statistic("p50")
                    .build()
            )
        }
        val p50DurationGraph = GraphWidget.Builder.create()
            .title("P50 Duration")
            .left(p50DurationMetrics)
            .view(GraphWidgetView.TIME_SERIES)
            .build()

        val p90DurationMetrics = functions.map {
            it.metricDuration(
                MetricOptions.builder()
                    .label(it.functionName)
                    .period(Duration.minutes(1))
                    .statistic("p90")
                    .build()
            )
        }
        val p90DurationGraph = GraphWidget.Builder.create()
            .title("P90 Duration")
            .left(p90DurationMetrics)
            .view(GraphWidgetView.TIME_SERIES)
            .build()

        val errorRates = functions.mapIndexed { i, function ->
            MathExpression.Builder.create()
                .expression("(errors$i / invocations$i) * 100")
                .usingMetrics(
                    mapOf(
                        "errors$i" to function.metricErrors(),
                        "invocations$i" to function.metricInvocations()
                    )
                )
                .label(function.functionName + " Error Rate")
                .build()
        }
        val errorRateGraph = GraphWidget.Builder.create()
            .title("Error Rates")
            .left(errorRates)
            .view(GraphWidgetView.TIME_SERIES)
            .build()

        val concurrentExecutionsMetrics = functions.map {
            it.metric(
                "ConcurrentExecutions", MetricOptions.builder()
                .label(it.functionName)
                .period(Duration.minutes(1))
                .statistic("Average")
                .build()
            )
        }
        val concurrentExecutionsGraph = GraphWidget.Builder.create()
            .title("ConcurrentExecutions")
            .left(concurrentExecutionsMetrics)
            .view(GraphWidgetView.TIME_SERIES)
            .build()

        val widgets = listOf(p90DurationGraph, p50DurationGraph, errorRateGraph, concurrentExecutionsGraph)
        Dashboard.Builder.create(this, "KotlinProductsDashboard")
            .dashboardName("KotlinProductsDashboard")
            .widgets(listOf(widgets))
            .build()
    }
}
