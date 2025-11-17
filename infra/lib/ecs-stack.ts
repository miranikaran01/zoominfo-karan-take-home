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
    const albFargate = new ecsPatterns.ApplicationLoadBalancedFargateService(
      this,
      'speech-to-text-service',
      {
        cluster,
        desiredCount: 1,
        cpu: 4096,
        memoryLimitMiB: 9216,
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
            WHISPER_URL: 'http://localhost:8000',
          },
        },
      },
    );

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

    const whisperContainer = taskDef.addContainer('faster-whisper-server', {
      containerName: 'faster-whisper-server',
      image: ecs.ContainerImage.fromRegistry(
        'fedirz/faster-whisper-server:sha-307e23f-cpu',
      ),
      memoryLimitMiB: 8192,
      environment: {
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
      essential: true,
      enableRestartPolicy: true,
      healthCheck: {
        command: [
          'CMD-SHELL',
          [
            'echo "[healthcheck] running" &&',
            'curl -sf http://localhost:8000/health',
            '&& echo "[healthcheck] OK" || (echo "[healthcheck] FAILED" >&2; exit 1)'
      ].join(' ')
        ],
        interval: cdk.Duration.seconds(30),
        timeout: cdk.Duration.seconds(5),
        retries: 3,
        startPeriod: cdk.Duration.seconds(300),
      },
    });

    const cfnTaskDef = taskDef.node.defaultChild as ecs.CfnTaskDefinition;
    cfnTaskDef.addPropertyOverride('ContainerDefinitions.0.Cpu', 512);
    cfnTaskDef.addPropertyOverride('ContainerDefinitions.0.Memory', 1024);
    cfnTaskDef.addPropertyOverride('ContainerDefinitions.1.Cpu', 3584);
    cfnTaskDef.addPropertyOverride('ContainerDefinitions.1.Memory', 8192);

    appContainer.addContainerDependencies({
      container: whisperContainer,
      condition: ecs.ContainerDependencyCondition.HEALTHY,
    });
  }
}