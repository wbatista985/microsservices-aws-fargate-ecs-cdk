package com.myorg;

import software.amazon.awscdk.*;
import software.amazon.awscdk.services.applicationautoscaling.EnableScalingProps;
import software.amazon.awscdk.services.ecs.*;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedFargateService;
import software.amazon.awscdk.services.ecs.patterns.ApplicationLoadBalancedTaskImageOptions;
import software.amazon.awscdk.services.elasticloadbalancingv2.HealthCheck;
import software.amazon.awscdk.services.logs.LogGroup;
import software.constructs.Construct;

import java.util.HashMap;
import java.util.Map;

public class Service02Stack extends Stack {
    public Service02Stack(final Construct scope, final String id, Cluster cluster) {
        this(scope, id, null, cluster);
    }

    public Service02Stack(final Construct scope, final String id, final StackProps props, Cluster cluster) {
        super(scope, id, props);

        Map<String, String> envVariables = new HashMap<>();
        envVariables.put("AWS_REGION", "us-east-1");

        //criando service e load balance
        ApplicationLoadBalancedFargateService service02 = ApplicationLoadBalancedFargateService.Builder
                .create(this, "ALB02")
                .serviceName("service-02")
                .cluster(cluster)
                .cpu(512)
                .memoryLimitMiB(1024)
                .desiredCount(2)
                .listenerPort(9090)
                .taskImageOptions(
                        ApplicationLoadBalancedTaskImageOptions.builder()
                                .containerName("curso_aws_project02")
                                .image(ContainerImage.fromRegistry("wbatista985/curso_aws_project02:1.0.0"))
                                .containerPort(8080)
                                .logDriver(LogDriver.awsLogs(AwsLogDriverProps.builder()
                                                .logGroup(LogGroup.Builder.create(this, "Service02LogGroup")
                                                        .logGroupName("Service02")
                                                        .removalPolicy(RemovalPolicy.DESTROY)
                                                        .build())
                                                .streamPrefix("Service02")
                                                .build()))
                                .environment(envVariables)
                                .build())
                .publicLoadBalancer(true)
                .build();


        //configuração de healthcheck
        service02.getTargetGroup().configureHealthCheck(new HealthCheck.Builder()
                .path("/actuator/health")
                .port("9090")
                .healthyHttpCodes("200")
                .build());


        //configuração de auto scaling
        ScalableTaskCount scalableTaskCount = service02.getService().autoScaleTaskCount(EnableScalingProps.builder()
                .minCapacity(2)
                .maxCapacity(4)
                .build());

        scalableTaskCount.scaleOnCpuUtilization("Service02AutoScaling", CpuUtilizationScalingProps.builder()
                .targetUtilizationPercent(50)
                .scaleInCooldown(Duration.seconds(60))
                .scaleOutCooldown(Duration.seconds(60))
                .build());

    }
}
