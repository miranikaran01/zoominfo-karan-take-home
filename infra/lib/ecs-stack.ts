import * as cdk from 'aws-cdk-lib';
import { Construct } from 'constructs';
import * as ec2 from 'aws-cdk-lib/aws-ec2';
import * as ecs from 'aws-cdk-lib/aws-ecs';
import * as ecsPatterns from 'aws-cdk-lib/aws-ecs-patterns';
import * as ecr from 'aws-cdk-lib/aws-ecr';

export class EcsStack extends cdk.Stack {
  constructor(scope: Construct, id: string, props?: cdk.StackProps) {
    super(scope, id, props);

    // IMAGE_TAG comes from GitHub Actions; fall back to 'latest' for local dev
    const imageTag = process.env.IMAGE_TAG ?? 'latest';

    const vpc = new ec2.Vpc(this, 'speech-to-text-vpc', {
      maxAzs: 2,
    });

    const cluster = new ecs.Cluster(this, 'speech-to-text-cluster', {
      vpc,
    });

    // ECR repo for speech-to-text app
    const appRepo = ecr.Repository.fromRepositoryName(
      this,
      'speech-to-text-app-repo',
      'speech-to-text',
    );

    // Fargate service with ALB
    // Total: 4 vCPU (4096 CPU units) and 9GB (9216 MiB) shared memory
    const albFargate = new ecsPatterns.ApplicationLoadBalancedFargateService(
      this,
      'speech-to-text-service',
      {
        cluster,
        desiredCount: 1,
        cpu: 4096, // 4 vCPU total
        memoryLimitMiB: 9216, // 9 GB total shared memory
        publicLoadBalancer: true,
        runtimePlatform: {
          cpuArchitecture: ecs.CpuArchitecture.ARM64,
          operatingSystemFamily: ecs.OperatingSystemFamily.LINUX,
        },
        taskImageOptions: {
          containerName: 'speech-to-text',
          image: ecs.ContainerImage.fromEcrRepository(appRepo, imageTag),
          containerPort: 8080,
          environment: {
            // Point app at whisper sidecar
            WHISPER_URL: 'http://localhost:8000',
          },
        },
      },
    );

    // ALB / TARGET GROUP HEALTH CHECK FOR SPEECH-TO-TEXT
    albFargate.targetGroup.configureHealthCheck({
      path: '/management/health',
      interval: cdk.Duration.seconds(30),
      timeout: cdk.Duration.seconds(5),
      healthyHttpCodes: '200',
      port: 'traffic-port',
      unhealthyThresholdCount: 3,
      healthyThresholdCount: 3,
    });

    const taskDef = albFargate.taskDefinition;
    const appContainer = taskDef.defaultContainer;

    if (!appContainer) {
      throw new Error('Default container not found');
    }

    // Set resource limits for speech-to-text container
    // 0.5 vCPU (512 CPU units) and 1GB (1024 MiB) memory
    // Note: For Fargate, we use escape hatch to set container-level CPU and memory
    // 0.5 vCPU = 0.5 * 1024 = 512 CPU units
    const cfnTaskDef = taskDef.node.defaultChild as ecs.CfnTaskDefinition;
    // Update app container (index 0) resources: 0.5 vCPU (512 CPU units) and 1GB (1024 MiB)
    cfnTaskDef.addPropertyOverride('ContainerDefinitions.0.Cpu', 512);
    cfnTaskDef.addPropertyOverride('ContainerDefinitions.0.Memory', 1024);

    // Sidecar: faster-whisper-server from Docker Hub
    // 3.5 vCPU (3584 CPU units) and 8GB (8192 MiB) memory
    // 3.5 vCPU = 3.5 * 1024 = 3584 CPU units
    const whisperContainer = taskDef.addContainer('faster-whisper-server', {
      containerName: 'faster-whisper-server',
      image: ecs.ContainerImage.fromRegistry(
        'fedirz/faster-whisper-server:sha-307e23f-cpu',
      ),
      cpu: 3584, // 3.5 vCPU (3584 CPU units)
      memoryLimitMiB: 8192, // 8 GB
      environment: {
        // Use the smallest model to minimize resource usage
        // Note: Use WHISPER__MODEL (double underscore) to set the nested whisper.model config
        WHISPER__MODEL: 'Systran/faster-whisper-small',
      },
      portMappings: [
        {
          containerPort: 8000,
          protocol: ecs.Protocol.TCP,
        },
      ],
      logging: ecs.LogDrivers.awsLogs({
        streamPrefix: 'faster-whisper',
      }),
      // ECS CONTAINER HEALTH CHECK FOR FASTER-WHISPER
      healthCheck: {
        command: [
          'CMD-SHELL',
          'curl -f http://localhost:8000/health || exit 1',
        ],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        retries: 3,
        startPeriod: cdk.Duration.seconds(300),
      },
    });

    // Ensure app starts after whisper is healthy
    appContainer.addContainerDependencies({
      container: whisperContainer,
      condition: ecs.ContainerDependencyCondition.HEALTHY,
    });
  }
}