plugins {
    application
}

dependencies {
    implementation("software.amazon.awscdk:aws-cdk-lib:2.2.0")
    implementation("software.amazon.awscdk:apigatewayv2-alpha:2.0.0-alpha.11")
    implementation("software.amazon.awscdk:apigatewayv2-integrations-alpha:2.0.0-alpha.11")
    implementation("software.constructs:constructs:10.0.15")
}

application {
    mainClass.set("com.myorg.InfrastructureAppKt")
}

task<Exec>("makeOptimizationLayer") {
    workingDir = File("../software/OptimizationLayer/")
    commandLine("./build-layer.sh")
}

tasks.named("run") {
    dependsOn(":infrastructure:makeOptimizationLayer")
    dependsOn(":software:shadowJar")
}
